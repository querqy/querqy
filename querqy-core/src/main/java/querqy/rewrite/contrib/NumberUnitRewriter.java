package querqy.rewrite.contrib;

import querqy.ComparableCharSequence;
import querqy.model.AbstractNodeVisitor;
import querqy.model.ExpandedQuery;
import querqy.model.MatchAllQuery;
import querqy.model.Node;
import querqy.model.QuerqyQuery;
import querqy.model.Query;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.contrib.numberunit.NumberUnitQueryCreator;
import querqy.rewrite.contrib.numberunit.model.NumberUnitQueryInput;
import querqy.rewrite.contrib.numberunit.model.PerUnitNumberUnitDefinition;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NumberUnitRewriter extends AbstractNodeVisitor<Node> implements QueryRewriter {

    private static final Set<Character> FLOAT_DELIMITERS = Stream.of('.', ',').collect(Collectors.toSet());

    private final Map<ComparableCharSequence, List<PerUnitNumberUnitDefinition>> numberUnitMap;
    private final NumberUnitQueryCreator numberUnitQueryCreator;

    private final Set<NumberUnitQueryInput> numberUnitQueryInputs = new HashSet<>();
    private NumberUnitQueryInput incompleteNumberUnitQueryInput = null;

    public NumberUnitRewriter(Map<ComparableCharSequence, List<PerUnitNumberUnitDefinition>> numberUnitMap, NumberUnitQueryCreator numberUnitQueryCreator) {
        this.numberUnitMap = numberUnitMap;
        this.numberUnitQueryCreator = numberUnitQueryCreator;
    }

    @Override
    public ExpandedQuery rewrite(final ExpandedQuery expandedQuery) {

        final QuerqyQuery<?> userQuery = expandedQuery.getUserQuery();

        if (!(userQuery instanceof Query)){
            return expandedQuery;
        }

        Query query = (Query) userQuery;

        visit(query);

        numberUnitQueryInputs.stream()
                .map(input -> numberUnitQueryCreator.createFilterQuery(
                        input.getNumber(), numberUnitMap.get(input.getUnit())))
                .forEach(expandedQuery::addFilterQuery);

        numberUnitQueryInputs.stream()
                .map(input -> numberUnitQueryCreator.createBoostQuery(
                        input.getNumber(), numberUnitMap.get(input.getUnit())))
                .forEach(expandedQuery::addBoostUpQuery);

        numberUnitQueryInputs.stream()
                .flatMap(numberUnitQueryInput -> numberUnitQueryInput.getOriginDisjunctionMaxQuery().stream())
                .forEach(query::removeClause);

        if (query.getClauses().isEmpty()) {
            expandedQuery.setUserQuery(new MatchAllQuery());
        }

        return expandedQuery;
    }

    @Override
    public Node visit(final Term term) {

        final ComparableCharSequence seq = term.getValue();

        if (incompleteNumberUnitQueryInput != null) {
            if (isUnit(seq)) {
                final NumberUnitQueryInput completeNumberUnitQueryInput = incompleteNumberUnitQueryInput;
                completeNumberUnitQueryInput.setUnit(seq);
                completeNumberUnitQueryInput.addOriginDisjunctionMaxQuery(term.getParent());
                numberUnitQueryInputs.add(completeNumberUnitQueryInput);
                incompleteNumberUnitQueryInput = null;
                return null;

            } else {
                incompleteNumberUnitQueryInput = null;
            }
        }

        Optional<NumberUnitQueryInput> numberUnitInputOptional = parseNumberAndUnit(seq);
        if (numberUnitInputOptional.isPresent()) {
            final NumberUnitQueryInput numberUnitQueryInput = numberUnitInputOptional.get();
            numberUnitQueryInput.addOriginDisjunctionMaxQuery(term.getParent());

            if (numberUnitQueryInput.hasUnit()) {
                numberUnitQueryInputs.add(numberUnitQueryInput);

            } else {
                incompleteNumberUnitQueryInput = numberUnitQueryInput;
            }
        }

        return null;
    }

    protected Optional<NumberUnitQueryInput> parseNumberAndUnit(final ComparableCharSequence seq) {

        boolean isNumber = false;
        int floatDelimiter = -1;

        for (int i = 0; i < seq.length(); i++) {
            char c = seq.charAt(i);

            if (Character.isDigit(c)) {
                isNumber = true;

            } else if (FLOAT_DELIMITERS.contains(c)) {
                if (floatDelimiter > -1) {
                    return Optional.empty();
                }

                floatDelimiter = i;

            } else {
                if (!isNumber) {
                    return Optional.empty();
                }

                final ComparableCharSequence unit = seq.subSequence(i, seq.length());
                if (isUnit(unit)) {
                    return Optional.of(new NumberUnitQueryInput(parseNumber(seq.subSequence(0, i), floatDelimiter), unit));

                } else {
                    return Optional.empty();
                }
            }
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
        return new BigDecimal(number).setScale(this.numberUnitQueryCreator.getScale(), this.numberUnitQueryCreator.getRoundingMode());
    }

    private boolean isUnit(final ComparableCharSequence seq) {
        return numberUnitMap.containsKey(seq);
    }
}
