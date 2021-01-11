package querqy.rewrite.commonrules;

import static querqy.rewrite.commonrules.select.SelectionStrategyFactory.DEFAULT_SELECTION_STRATEGY;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import querqy.model.EmptySearchEngineRequestAdapter;
import querqy.model.ExpandedQuery;
import querqy.model.InputSequenceElement;
import querqy.model.Query;
import querqy.model.builder.impl.BooleanQueryBuilder;
import querqy.parser.WhiteSpaceQuerqyParser;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.commonrules.model.Action;
import querqy.rewrite.commonrules.model.DecorateInstruction;
import querqy.rewrite.commonrules.model.DeleteInstruction;
import querqy.rewrite.commonrules.model.Input;
import querqy.rewrite.commonrules.model.Instruction;
import querqy.rewrite.commonrules.model.Instructions;
import querqy.rewrite.commonrules.model.PositionSequence;
import querqy.rewrite.commonrules.model.RulesCollection;
import querqy.rewrite.commonrules.model.RulesCollectionBuilder;
import querqy.rewrite.commonrules.model.SynonymInstruction;
import querqy.rewrite.commonrules.model.Term;
import querqy.rewrite.commonrules.model.TrieMapRulesCollectionBuilder;
import querqy.rewrite.commonrules.select.TopRewritingActionCollector;

public abstract class AbstractCommonRulesTest {

    private int ruleCounter = 0;

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
        final TopRewritingActionCollector collector = DEFAULT_SELECTION_STRATEGY.createTopRewritingActionCollector();
        rules.collectRewriteActions(seq, collector);
        return collector.createActions();
    }

    public DeleteInstruction delete(String... terms) {
        return new DeleteInstruction(Arrays.stream(terms).map(this::mkTerm).collect(Collectors.toList()));
    }

    public DecorateInstruction decorate(String key, String value) {
        return new DecorateInstruction(key, value);
    }

    public SynonymInstruction synonym(String... terms) {
        return new SynonymInstruction(Arrays.stream(terms).map(this::mkTerm).collect(Collectors.toList()));
    }

    public Input input(String... terms) {
        return new Input(Arrays.stream(terms).map(this::mkTerm).collect(Collectors.toList()), false, false, "");
    }

    public void addRule(RulesCollectionBuilder builder, Input input, Instruction... instructions) {
        int ruleCount = ruleCounter++;
        builder.addRule(input, new Instructions(ruleCount, ruleCount, Arrays.asList(instructions)));
    }

    public CommonRulesRewriter rewriter(Rule... rules) {
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        Arrays.stream(rules).forEach(rule -> builder.addRule(rule.input, rule.instructions));
        return new CommonRulesRewriter(builder.build(), DEFAULT_SELECTION_STRATEGY);
    }

    public Rule rule(Input input, Instruction... instructions) {
        int ruleCount = ruleCounter++;
        return new Rule(input, new Instructions(ruleCount, ruleCount, Arrays.asList(instructions)));
    }

    public BooleanQueryBuilder rewrite(BooleanQueryBuilder queryBuilder, CommonRulesRewriter rewriter) {
        return rewrite(queryBuilder, rewriter, new EmptySearchEngineRequestAdapter());
    }

    public BooleanQueryBuilder rewrite(BooleanQueryBuilder queryBuilder, CommonRulesRewriter rewriter,
                         SearchEngineRequestAdapter searchEngineRequestAdapter) {
        ExpandedQuery query = new ExpandedQuery(queryBuilder.buildQuerqyQuery());
        return new BooleanQueryBuilder((Query) rewriter.rewrite(query, searchEngineRequestAdapter).getUserQuery());
    }

    public static List<String> list(String... items) {
        return Arrays.asList(items);
    }

    public static List<Instruction> list(Instruction... instructions) {
        return Arrays.asList(instructions);
    }

    public static class Rule {
        public final Input input;
        public final Instructions instructions;

        public Rule(Input input, Instructions instructions) {
            this.input = input;
            this.instructions = instructions;
        }
    }

}
