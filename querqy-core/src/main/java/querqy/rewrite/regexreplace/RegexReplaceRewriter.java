/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2026 Querqy Contributors
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
package querqy.rewrite.regexreplace;

import querqy.model.AbstractNodeVisitor;
import querqy.model.BoostQuery;
import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.Node;
import querqy.model.QuerqyQuery;
import querqy.model.Query;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterOutput;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.logging.ActionLog;
import querqy.rewrite.logging.RewriterLog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class RegexReplaceRewriter extends AbstractNodeVisitor<Node> implements QueryRewriter {

    private final RegexReplacing regexReplacing;
    private LinkedList<CharSequence> collectedTerms;

    public RegexReplaceRewriter(final RegexReplacing regexReplacing) {
        this.regexReplacing = regexReplacing;
    }

    @Override
    public RewriterOutput rewrite(final ExpandedQuery expandedQuery,
                                  final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        final QuerqyQuery<?> querqyQuery = expandedQuery.getUserQuery();

        if (!(querqyQuery instanceof Query)) {
            return RewriterOutput.builder().expandedQuery(expandedQuery).build();
        }

        collectedTerms = new LinkedList<>();
        visit((Query) querqyQuery);

        final List<ActionLog> actionLogs = searchEngineRequestAdapter.getRewriteLoggingConfig().hasDetails()
                ? new ArrayList<>() : null;

        final String queryString = String.join(" ", collectedTerms);

        return regexReplacing.replace(queryString).map(replacementResult ->
                        RewriterOutput.builder()
                                .expandedQuery(buildQueryFromSeqList(expandedQuery, replacementResult.replacement()))
                                .rewriterLog(RewriterLog.builder()
                                        .hasAppliedRewriting(true)
                                        .actionLogs(actionLogs)
                                        .build())
                                .build()
        ).orElseGet( () -> RewriterOutput.builder()
                .expandedQuery(expandedQuery)
                .rewriterLog(RewriterLog.builder()
                        .hasAppliedRewriting(false)
                        .actionLogs(actionLogs)
                        .build())
                .build());

    }

    // TODO: Alternatives in DMQs should be considered
    @Override
    public Node visit(final Term term) {
        if (!term.isGenerated()) {
            collectedTerms.addLast(term);
        }
        return null;
    }

    private static ExpandedQuery buildQueryFromSeqList(final ExpandedQuery oldQuery, final CharSequence queryString) {
        final Query query = new Query();

        // TODO: try not to convert between CharSequence and String so many times
        for (final CharSequence token: queryString.toString().split(" ")) {
            final DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, Clause.Occur.SHOULD, false);
            query.addClause(dmq);
            final Term term = new Term(dmq, token);
            dmq.addClause(term);
        }

        final ExpandedQuery newQuery = new ExpandedQuery(query);

        final Collection<BoostQuery> boostDownQueries = oldQuery.getBoostDownQueries();
        if (boostDownQueries != null) {
            boostDownQueries.forEach(newQuery::addBoostDownQuery);
        }

        final Collection<BoostQuery> boostUpQueries = oldQuery.getBoostUpQueries();
        if (boostUpQueries != null) {
            boostUpQueries.forEach(newQuery::addBoostUpQuery);
        }

        final Collection<BoostQuery> multiplicativeBoostQueries = oldQuery.getMultiplicativeBoostQueries();
        if (multiplicativeBoostQueries != null) {
            multiplicativeBoostQueries.forEach(newQuery::addMultiplicativeBoostQuery);
        }

        final Collection<QuerqyQuery<?>> filterQueries = oldQuery.getFilterQueries();
        if (filterQueries != null) {
            filterQueries.forEach(newQuery::addFilterQuery);
        }

        return newQuery;
    }


}
