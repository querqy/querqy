package querqy.rewrite.commonrules;

import static org.mockito.Mockito.mock;
import static querqy.model.builder.QueryBuilder.query;
import static querqy.rewrite.commonrules.select.SelectionStrategyFactory.DEFAULT_SELECTION_STRATEGY;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import querqy.model.EmptySearchEngineRequestAdapter;
import querqy.model.ExpandedQuery;
import querqy.model.InputSequenceElement;
import querqy.model.Query;
import querqy.model.convert.builder.BooleanQueryBuilder;
import querqy.parser.WhiteSpaceQuerqyParser;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.commonrules.model.Action;
import querqy.rewrite.commonrules.model.DecorateInstruction;
import querqy.rewrite.commonrules.model.DeleteInstruction;
import querqy.rewrite.commonrules.model.FilterInstruction;
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
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInput;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInputLiteral;

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

    public FilterInstruction filter(String... terms) {
        return new FilterInstruction(query(terms).build());
    }

    public Input input(String... terms) {
        return new Input(Arrays.stream(terms).map(this::mkTerm).collect(Collectors.toList()), false, false, "");
    }

    public Input input(List<String> terms) {
        return new Input(terms.stream().map(this::mkTerm).collect(Collectors.toList()), false, false, "");
    }

    public void addRule(RulesCollectionBuilder builder, Input input, Instruction... instructions) {
        int ruleCount = ruleCounter++;
        builder.addRule(input, new Instructions(ruleCount, ruleCount, Arrays.asList(instructions)));
    }

    public CommonRulesRewriter rewriter(List<BooleanInputLiteral> literals) {
        return rewriter(literals.stream()
                .map(literal -> rule(input(literal.getTerms()), literal))
                .toArray(Rule[]::new));
    }

    public CommonRulesRewriter rewriter(Rule... rules) {
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        Arrays.stream(rules).forEach(rule -> {
            if (rule.instructions != null) {
                builder.addRule(rule.input, rule.instructions);
            } else {
                builder.addRule(rule.input, rule.literal);
            }
        });
        return new CommonRulesRewriter(builder.build(), DEFAULT_SELECTION_STRATEGY);
    }

    public Rule rule(Input input, Instruction... instructions) {
        int ruleCount = ruleCounter++;
        return new Rule(input, new Instructions(ruleCount, ruleCount, Arrays.asList(instructions)));
    }

    public Rule rule(Input input, BooleanInputLiteral literal) {
        return new Rule(input, literal);
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

    public Predicate<boolean[]> createConjunctionPredicate(int size) {
        Predicate<boolean[]> predicate = booleans -> booleans[0];

        for (int i = 1; i < size; i++) {
            final int j = i;
            predicate = predicate.and(booleans -> booleans[j]);
        }

        return predicate;
    }

    public void booleanInput(List<BooleanInputLiteral> literals) {
        booleanInput(literals, mock(Instructions.class));
    }

    public void booleanInput(List<BooleanInputLiteral> literals, Instructions instructions) {
        BooleanInput.BooleanInputBuilder builder = BooleanInput.builder();
        literals.forEach(builder::addLiteralAndCreateReferenceId);
        builder.setPredicate(createConjunctionPredicate(literals.size()))
                .linkToInstructions(instructions)
                .build();
    }

    public void booleanInput(List<BooleanInputLiteral> literals, Instruction... instructions) {
        int ruleCount = ruleCounter++;
        booleanInput(literals, new Instructions(ruleCount, ruleCount, Arrays.asList(instructions)));
    }

    public List<BooleanInputLiteral> literals(final int size) {
        return IntStream.range(0, size).mapToObj(number -> literal(String.valueOf(number))).collect(Collectors.toList());
    }

    public BooleanInputLiteral literal(final String term) {
        return new BooleanInputLiteral(Collections.singletonList(term));
    }

    public List<BooleanInputLiteral> literals(final String... terms) {
        return Arrays.stream(terms).map(this::literal).collect(Collectors.toList());
    }

    public static class Rule {
        public final Input input;
        public final Instructions instructions;
        public final BooleanInputLiteral literal;

        public Rule(Input input, Instructions instructions) {
            this(input, instructions, null);
        }

        public Rule(Input input, BooleanInputLiteral literal) {
            this(input, null, literal);
        }

        private Rule(Input input, Instructions instructions, BooleanInputLiteral literal) {
            this.input = input;
            this.instructions = instructions;
            this.literal = literal;
        }
    }

}
