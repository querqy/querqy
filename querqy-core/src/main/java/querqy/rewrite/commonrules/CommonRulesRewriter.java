package querqy.rewrite.commonrules;

import querqy.model.BooleanQuery;
import querqy.model.ExpandedQuery;
import querqy.model.MatchAllQuery;
import querqy.model.QuerqyQuery;
import querqy.model.Query;
import querqy.rewrite.commonrules.model.InstructionsSupplier;
import querqy.rewrite.logging.RewriterLog;
import querqy.rewrite.RewriterOutput;
import querqy.model.Term;
import querqy.rewrite.logging.ActionLog;
import querqy.rewrite.logging.InstructionLog;
import querqy.rewrite.logging.MatchLog;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.commonrules.model.Action;
import querqy.rewrite.commonrules.model.Instruction;
import querqy.rewrite.commonrules.model.InstructionDescription;
import querqy.rewrite.commonrules.model.Instructions;
import querqy.rewrite.commonrules.model.TermMatch;
import querqy.rewrite.commonrules.select.SelectionStrategy;
import querqy.rewrite.commonrules.select.TopRewritingActionCollector;
import querqy.rewrite.lookup.TrieMapLookup;
import querqy.rewrite.lookup.model.Match;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author rene
 *
 */
public class CommonRulesRewriter implements QueryRewriter {

    private final TrieMapLookup<InstructionsSupplier> trieMapLookup;

    protected ExpandedQuery expandedQuery;
    protected SearchEngineRequestAdapter searchEngineRequestAdapter;

    protected SelectionStrategy selectionStrategy;

    private final RewriterLog.RewriterLogBuilder rewriterLogBuilder = RewriterLog.builder();

    public CommonRulesRewriter(
            final TrieMapLookup<InstructionsSupplier> trieMapLookup, final SelectionStrategy selectionStrategy) {
        this.trieMapLookup = trieMapLookup;
        this.selectionStrategy = selectionStrategy;
    }

    @Override
    public RewriterOutput rewrite(final ExpandedQuery query, final SearchEngineRequestAdapter searchEngineRequestAdapter) {

        final QuerqyQuery<?> userQuery = query.getUserQuery();

        if (userQuery instanceof Query) {

            this.expandedQuery = query;
            this.searchEngineRequestAdapter = searchEngineRequestAdapter;

            rewriteBooleanQuery((BooleanQuery) query.getUserQuery());

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

    protected void rewriteBooleanQuery(final BooleanQuery booleanQuery) {

        final TopRewritingActionCollector collector = selectionStrategy.createTopRewritingActionCollector();
        final List<Match<InstructionsSupplier>> matches = trieMapLookup.lookupMatches(booleanQuery);

        for (final Match<InstructionsSupplier> match : matches) {
            collector.collect(match.getValue(), instructions -> new Action(instructions, match.getTermMatches()));
        }

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

    private void appendActionLogs(final Action action) {
        final ActionLog actionLog = new ActionLogConverter(action).convert();
        rewriterLogBuilder.addActionLogs(actionLog);
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
