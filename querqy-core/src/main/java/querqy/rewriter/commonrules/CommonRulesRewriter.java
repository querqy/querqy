/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2014 Querqy Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package querqy.rewriter.commonrules;

import querqy.model.BooleanQuery;
import querqy.model.ExpandedQuery;
import querqy.model.MatchAllQuery;
import querqy.model.QuerqyQuery;
import querqy.model.Query;
import querqy.rewriter.commonrules.model.InstructionsSupplier;
import querqy.rewrite.logging.RewriterLog;
import querqy.rewrite.RewriterOutput;
import querqy.model.Term;
import querqy.rewrite.logging.ActionLog;
import querqy.rewrite.logging.InstructionLog;
import querqy.rewrite.logging.MatchLog;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewriter.commonrules.model.Action;
import querqy.rewriter.commonrules.model.Instruction;
import querqy.rewriter.commonrules.model.InstructionDescription;
import querqy.rewriter.commonrules.model.Instructions;
import querqy.rewriter.commonrules.model.TermMatch;
import querqy.rewriter.commonrules.select.SelectionStrategy;
import querqy.rewriter.commonrules.select.TopRewritingActionCollector;
import querqy.rewrite.lookup.triemap.TrieMapLookupQueryVisitor;
import querqy.rewrite.lookup.triemap.TrieMapLookupQueryVisitorFactory;
import querqy.rewrite.lookup.model.Match;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author rene
 *
 */
public class CommonRulesRewriter implements QueryRewriter {

    private final TrieMapLookupQueryVisitorFactory<InstructionsSupplier> trieMapLookupQueryVisitorFactory;

    protected ExpandedQuery expandedQuery;
    protected SearchEngineRequestAdapter searchEngineRequestAdapter;

    protected SelectionStrategy selectionStrategy;

    private final RewriterLog.RewriterLogBuilder rewriterLogBuilder = RewriterLog.builder();

    public CommonRulesRewriter(
            final TrieMapLookupQueryVisitorFactory<InstructionsSupplier> trieMapLookupQueryVisitorFactory, final SelectionStrategy selectionStrategy) {
        this.trieMapLookupQueryVisitorFactory = trieMapLookupQueryVisitorFactory;
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

        final TrieMapLookupQueryVisitor<InstructionsSupplier> trieMapLookupQueryVisitor = trieMapLookupQueryVisitorFactory.createTrieMapLookup(booleanQuery);

        final List<Match<InstructionsSupplier>> matches = trieMapLookupQueryVisitor.lookupAndCollect();

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
