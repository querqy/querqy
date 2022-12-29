package querqy.rewrite.commonrules;

import querqy.model.AbstractNodeVisitor;
import querqy.model.BooleanQuery;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.InputSequenceElement;
import querqy.model.MatchAllQuery;
import querqy.model.Node;
import querqy.model.QuerqyQuery;
import querqy.model.Query;
import querqy.rewrite.logging.RewriterLog;
import querqy.rewrite.RewriterOutput;
import querqy.model.Term;
import querqy.rewrite.logging.ActionLog;
import querqy.rewrite.logging.InstructionLog;
import querqy.rewrite.logging.MatchLog;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.commonrules.model.Action;
import querqy.rewrite.commonrules.model.InputBoundary;
import querqy.rewrite.commonrules.model.InputBoundary.Type;
import querqy.rewrite.commonrules.model.Instruction;
import querqy.rewrite.commonrules.model.InstructionDescription;
import querqy.rewrite.commonrules.model.Instructions;
import querqy.rewrite.commonrules.model.PositionSequence;
import querqy.rewrite.commonrules.model.RulesCollection;
import querqy.rewrite.commonrules.model.TermMatch;
import querqy.rewrite.commonrules.select.SelectionStrategy;
import querqy.rewrite.commonrules.select.TopRewritingActionCollector;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author rene
 *
 */
public class CommonRulesRewriter extends AbstractNodeVisitor<Node> implements QueryRewriter {

    static final InputBoundary LEFT_BOUNDARY = new InputBoundary(Type.LEFT);
    static final InputBoundary RIGHT_BOUNDARY = new InputBoundary(Type.RIGHT);

    protected final RulesCollection rules;
    protected final LinkedList<PositionSequence<Term>> sequencesStack;
    protected ExpandedQuery expandedQuery;
    protected SearchEngineRequestAdapter searchEngineRequestAdapter;

    protected SelectionStrategy selectionStrategy;

    private final RewriterLog.RewriterLogBuilder rewriterLogBuilder = RewriterLog.builder();

    public CommonRulesRewriter(final RulesCollection rules,  final SelectionStrategy selectionStrategy) {
        this.rules = rules;
        sequencesStack = new LinkedList<>();
        this.selectionStrategy = selectionStrategy;
    }

    @Override
    public RewriterOutput rewrite(final ExpandedQuery query, final SearchEngineRequestAdapter searchEngineRequestAdapter) {

        final QuerqyQuery<?> userQuery = query.getUserQuery();

        if (userQuery instanceof Query) {

            this.expandedQuery = query;
            this.searchEngineRequestAdapter = searchEngineRequestAdapter;

            sequencesStack.add(new PositionSequence<>());

            super.visit((BooleanQuery) query.getUserQuery());

            applySequence(sequencesStack.removeLast(), true);

            if (((Query) userQuery).isEmpty()
                    && (query.getBoostUpQueries() != null || query.getFilterQueries() != null)) {
                query.setUserQuery(new MatchAllQuery(true));
            }
        }

        return RewriterOutput.builder()
                .expandedQuery(query)
                .rewriterLog(rewriterLogBuilder.build())
                .build();
    }

    @Override
    public Node visit(final BooleanQuery booleanQuery) {
        sequencesStack.add(new PositionSequence<>());
        super.visit(booleanQuery);
        applySequence(sequencesStack.removeLast(), false);
        return null;
    }

    protected void applySequence(final PositionSequence<Term> sequence, final boolean addBoundaries) {

        final PositionSequence<InputSequenceElement> sequenceForLookUp = addBoundaries
               ? addBoundaries(sequence) : termSequenceToInputSequence(sequence);

        final TopRewritingActionCollector collector = selectionStrategy.createTopRewritingActionCollector();
        rules.collectRewriteActions(sequenceForLookUp, collector);

        final List<Action> actions = collector.evaluateBooleanInput().createActions();

        for (final Action action : actions) {

            final Instructions instructions = action.getInstructions();
            instructions.forEach(instruction ->
                    instruction.apply(action.getTermMatches(), expandedQuery, searchEngineRequestAdapter)
            );

            rewriterLogBuilder.hasAppliedRewriting(true);
            if (searchEngineRequestAdapter.getRewriteLoggingConfig().hasDetails()) {
                appendActionLogs(action);
            }
        }
    }


    protected PositionSequence<InputSequenceElement> termSequenceToInputSequence(
            final PositionSequence<Term> sequence) {

        final PositionSequence<InputSequenceElement> result = new PositionSequence<>();
        sequence.forEach(termList -> result.add(Collections.unmodifiableList(termList)));
        return result;
    }

    protected PositionSequence<InputSequenceElement> addBoundaries(final PositionSequence<Term> sequence) {

        PositionSequence<InputSequenceElement> result = new PositionSequence<>();
        result.nextPosition();
        result.addElement(LEFT_BOUNDARY);

        for (List<Term> termList : sequence) {
            result.add(Collections.unmodifiableList(termList));
        }

        result.nextPosition();
        result.addElement(RIGHT_BOUNDARY);
        return result;
    }

    private void appendActionLogs(final Action action) {
        final ActionLog actionLog = new ActionLogConverter(action).convert();
        rewriterLogBuilder.addActionLogs(actionLog);
    }

    @Override
    public Node visit(final DisjunctionMaxQuery disjunctionMaxQuery) {
        sequencesStack.getLast().nextPosition();
        return super.visit(disjunctionMaxQuery);
    }

    @Override
    public Node visit(final Term term) {
        sequencesStack.getLast().addElement(term);
        return super.visit(term);
    }

    private static class ActionLogConverter {

        private final Action action;

        public ActionLogConverter(final Action action) {
            this.action = action;
        }

        public ActionLog convert() {
            return ActionLog.builder()
                    .message(convertMessage())
                    .match(convertMatch())
                    .instructions(convertInstructions())
                    .build();
        }

        private String convertMessage() {
            return (String) action.getInstructions()
                    .getProperty(Instructions.StandardPropertyNames.LOG_MESSAGE)
                    .orElse("");
        }

        private MatchLog convertMatch() {
            final String term = action.getTermMatches().stream()
                    .map(TermMatch::getQueryTerm)
                    .map(Term::getValue)
                    .collect(Collectors.joining(" "));

            final boolean isPrefix = action.getTermMatches().stream().anyMatch(TermMatch::isPrefix);

            return MatchLog.builder()
                    .term(term)
                    .type(isPrefix ? MatchLog.MatchType.AFFIX : MatchLog.MatchType.EXACT)
                    .build();
        }

        private List<InstructionLog> convertInstructions() {

            return action.getInstructions().stream()
                    .map(this::convertInstruction)
                    .collect(Collectors.toList());
        }

        private InstructionLog convertInstruction(final Instruction instruction) {
            final InstructionDescription instructionDescription = instruction.getInstructionDescription();

            final InstructionLog.InstructionLogBuilder builder = InstructionLog.builder()
                    .type(instructionDescription.getTypeName());
            instructionDescription.getParam().ifPresent(param -> builder.param(param.toString()));
            instructionDescription.getValue().ifPresent(builder::value);

            return builder.build();
        }
    }

}
