package querqy.rewrite.contrib;

import querqy.ComparableCharSequence;
import querqy.model.AbstractNodeVisitor;
import querqy.model.ExpandedQuery;
import querqy.model.MatchAllQuery;
import querqy.model.Node;
import querqy.model.QuerqyQuery;
import querqy.model.Query;
import querqy.model.RewritingOutput;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.contrib.numberunit.NumberUnitQueryCreator;
import querqy.rewrite.contrib.numberunit.model.NumberUnitQueryInput;
import querqy.rewrite.contrib.numberunit.model.PerUnitNumberUnitDefinition;
import querqy.trie.State;
import querqy.trie.TrieMap;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class NumberUnitRewriter extends AbstractNodeVisitor<Node> implements QueryRewriter {

    private final TrieMap<List<PerUnitNumberUnitDefinition>> numberUnitMap;
    private final NumberUnitQueryCreator numberUnitQueryCreator;

    private final Set<NumberUnitQueryInput> numberUnitQueryInputs = new HashSet<>();
    private NumberUnitQueryInput incompleteNumberUnitQueryInput = null;

    public static boolean isFloatDelimiter(final char ch) {
        return ch == ',' || ch == '.';
    }

    public NumberUnitRewriter(final TrieMap<List<PerUnitNumberUnitDefinition>> numberUnitMap,
                              final NumberUnitQueryCreator numberUnitQueryCreator) {
        this.numberUnitMap = numberUnitMap;
        this.numberUnitQueryCreator = numberUnitQueryCreator;
    }

    @Override
    public RewritingOutput rewrite(final ExpandedQuery expandedQuery, final SearchEngineRequestAdapter requestAdapter) {

        final QuerqyQuery<?> userQuery = expandedQuery.getUserQuery();

        if (!(userQuery instanceof Query)){
            return new RewritingOutput(expandedQuery);
        }

        final Query query = (Query) userQuery;

        visit(query);

        numberUnitQueryInputs.stream()
                .map(input -> numberUnitQueryCreator.createFilterQuery(
                        input.getNumber(), input.getPerUnitNumberUnitDefinitions()))
                .forEach(expandedQuery::addFilterQuery);

        numberUnitQueryInputs.stream()
                .map(input -> numberUnitQueryCreator.createBoostQuery(
                        input.getNumber(), input.getPerUnitNumberUnitDefinitions()))
                .forEach(expandedQuery::addBoostUpQuery);

        numberUnitQueryInputs.stream()
                .flatMap(numberUnitQueryInput -> numberUnitQueryInput.getOriginDisjunctionMaxQuery().stream())
                .forEach(query::removeClause);

        if (query.getClauses().isEmpty()) {
            expandedQuery.setUserQuery(new MatchAllQuery());
        }

        return new RewritingOutput(expandedQuery);
    }

    @Override
    public Node visit(final Term term) {

        if (term.isGenerated()) {
            return null;
        }

        final ComparableCharSequence seq = term.getValue();

        if (incompleteNumberUnitQueryInput != null) {
            final List<PerUnitNumberUnitDefinition> unitDefinitions = lookupUnitDef(seq);

            if (unitDefinitions.isEmpty()) {
                incompleteNumberUnitQueryInput = null;

            } else {
                final NumberUnitQueryInput completeNumberUnitQueryInput = incompleteNumberUnitQueryInput;
                completeNumberUnitQueryInput.addPerUnitNumberUnitDefinitions(unitDefinitions);
                completeNumberUnitQueryInput.addOriginDisjunctionMaxQuery(term.getParent());

                numberUnitQueryInputs.add(completeNumberUnitQueryInput);

                incompleteNumberUnitQueryInput = null;

                return null;
            }
        }

        parseNumberAndUnit(seq).ifPresent(numberUnitQueryInput -> {

            numberUnitQueryInput.addOriginDisjunctionMaxQuery(term.getParent());

            if (numberUnitQueryInput.hasUnit()) {
                numberUnitQueryInputs.add(numberUnitQueryInput);

            } else {
                incompleteNumberUnitQueryInput = numberUnitQueryInput;
            }

        });

        return null;
    }

    protected Optional<NumberUnitQueryInput> parseNumberAndUnit(final ComparableCharSequence seq) {

        boolean isNumber = false;
        int floatDelimiter = -1;

        for (int i = 0, len = seq.length(); i < len; i++) {

            final char c = seq.charAt(i);

            if (Character.isDigit(c)) {
                isNumber = true;

            } else if (isFloatDelimiter(c)) {
                if (floatDelimiter > -1) {
                    return Optional.empty();
                }

                floatDelimiter = i;

            } else {
                if (!isNumber) {
                    return Optional.empty();
                }

                final ComparableCharSequence unit = seq.subSequence(i, seq.length());

                final List<PerUnitNumberUnitDefinition> unitDefinitions = lookupUnitDef(unit);
                return unitDefinitions.isEmpty()
                        ? Optional.empty()
                        : Optional.of(
                                new NumberUnitQueryInput(
                                        parseNumber(seq.subSequence(0, i), floatDelimiter),
                                        unitDefinitions));
            }
        }

        if (!isNumber) {
            return Optional.empty();
        }

        return Optional.of(new NumberUnitQueryInput(parseNumber(seq, floatDelimiter)));
    }

    private BigDecimal parseNumber(final ComparableCharSequence seq, final int floatDelimiter) {

        if (floatDelimiter < 0) {
            return createBigDecimal(seq.toString());

        } else if (floatDelimiter == seq.length() - 1) {
            return createBigDecimal(seq.subSequence(0, seq.length() - 1).toString());

        } else if (floatDelimiter < seq.length() - 1) {
            return createBigDecimal(seq.subSequence(0, floatDelimiter) + "." +
                    seq.subSequence(floatDelimiter + 1, seq.length()));

        } else {
            throw new NumberFormatException("Float delimiter out of range");
        }
    }

    private BigDecimal createBigDecimal(final String number) {
        return new BigDecimal(number).setScale(numberUnitQueryCreator.getScale(),
                numberUnitQueryCreator.getRoundingMode());
    }

    private List<PerUnitNumberUnitDefinition> lookupUnitDef(final ComparableCharSequence seq) {
        final State<List<PerUnitNumberUnitDefinition>> state = numberUnitMap.get(seq).getStateForCompleteSequence();
        return state.isFinal() ? state.value : Collections.emptyList();
    }
}
