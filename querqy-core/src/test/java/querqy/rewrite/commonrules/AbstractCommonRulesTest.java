package querqy.rewrite.commonrules;

import static querqy.rewrite.commonrules.SelectionStrategyFactory.DEFAULT_SELECTION_STRATEGY;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import querqy.model.ExpandedQuery;
import querqy.model.InputSequenceElement;
import querqy.parser.WhiteSpaceQuerqyParser;
import querqy.rewrite.commonrules.model.Action;
import querqy.rewrite.commonrules.model.PositionSequence;
import querqy.rewrite.commonrules.model.RulesCollection;
import querqy.rewrite.commonrules.model.Term;
import querqy.rewrite.commonrules.model.TopRewritingActionCollector;

public abstract class AbstractCommonRulesTest {
    
    public final static Map<String, Object> EMPTY_CONTEXT = new HashMap<>();

    protected ExpandedQuery makeQuery(String input) {
        return new ExpandedQuery(new WhiteSpaceQuerqyParser().parse(input));
    }

    protected Term mkTerm(String s) {
        return new Term(s.toCharArray(), 0, s.length(), null);
    }

    protected Term mkTerm(String s, String... fieldName) {
        return new Term(s.toCharArray(), 0, s.length(), Arrays.asList(fieldName));
    }

    public static List<Action> getActions(final RulesCollection rules, final PositionSequence<InputSequenceElement> seq) {
        final TopRewritingActionCollector collector = DEFAULT_SELECTION_STRATEGY.getTopRewritingActionCollector();
        rules.collectRewriteActions(seq, collector);
        return collector.createActions();
    }

}
