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
package querqy.rewrite.contrib;

import querqy.model.AbstractNodeVisitor;
import querqy.model.BoostQuery;
import querqy.model.BoostedPhraseQuery;
import querqy.model.Clause.Occur;
import querqy.model.DisjunctionMaxClause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.Node;
import querqy.model.PhraseQuery;
import querqy.model.Query;
import querqy.model.QuerqyQuery;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterOutput;
import querqy.rewrite.SearchEngineRequestAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>Adds phrase boost queries derived from the user query terms to the {@link ExpandedQuery}.</p>
 *
 * <p>For a query with terms A B C D this rewriter can generate boost queries for
 * bigrams ("A B", "B C", "C D"), trigrams ("A B C", "B C D"), and the full phrase
 * ("A B C D"), depending on configuration. All generated phrase queries are combined
 * into a single {@link DisjunctionMaxQuery} with a configurable tie-breaker so that
 * matching multiple sub-phrases does not inflate scores unboundedly.</p>
 *
 * <p>Only non-generated terms from non-MUST_NOT clauses are considered. This rewriter
 * should run first in the rewrite chain so that later rewriters' generated terms are
 * not included.</p>
 *
 * @param bigramConfig  configuration for bigram phrase boosts, or {@code null} to disable
 * @param trigramConfig configuration for trigram phrase boosts, or {@code null} to disable
 * @param fullConfig    configuration for full-phrase boost, or {@code null} to disable
 * @param tieBreaker    tie-breaker for combining sub-phrase scores (0 = max wins, 1 = sum)
 */
public record PhraseBoostRewriter(
        PhraseTypeConfig bigramConfig,
        PhraseTypeConfig trigramConfig,
        PhraseTypeConfig fullConfig,
        float tieBreaker) implements QueryRewriter {

    /** A field name together with its boost weight, parsed from specs like {@code "title"} or {@code "brand^4"}. */
    public static class FieldAndBoost {
        public final String field;
        public final float boost;

        public FieldAndBoost(final String field, final float boost) {
            this.field = field;
            this.boost = boost;
        }

        public static FieldAndBoost parse(final String fieldSpec) {
            final int caretIdx = fieldSpec.indexOf('^');
            if (caretIdx < 0) {
                return new FieldAndBoost(fieldSpec.trim(), 1.0f);
            }
            return new FieldAndBoost(
                    fieldSpec.substring(0, caretIdx).trim(),
                    Float.parseFloat(fieldSpec.substring(caretIdx + 1).trim()));
        }
    }

    /** Configuration for one phrase length (bigram, trigram, or full phrase). */
    public static class PhraseTypeConfig {
        public final List<FieldAndBoost> fields;
        public final int slop;

        public PhraseTypeConfig(final List<FieldAndBoost> fields, final int slop) {
            this.fields = fields;
            this.slop = slop;
        }
    }

    /**
     * Collects the sequence of non-generated, non-MUST_NOT terms from a query by
     * traversing it with the visitor pattern. Created fresh per rewrite call so
     * that {@link PhraseBoostRewriter} itself remains stateless and reusable.
     */
    static class TermSequenceCollector extends AbstractNodeVisitor<Node> {

        final List<String> sequence = new ArrayList<>();

        List<String> collect(final Query query) {
            visit(query);
            return sequence;
        }

        @Override
        public Node visit(final DisjunctionMaxQuery dmq) {
            if (dmq.occur == Occur.MUST_NOT) {
                return null;
            }
            final List<DisjunctionMaxClause> clauses = dmq.getClauses();
            if (clauses == null || clauses.isEmpty()) {
                return null;
            }
            if (clauses.size() == 1) {
                return super.visit(dmq);
            }
            // Multiple clauses: find the single non-generated clause to visit
            DisjunctionMaxClause nonGenerated = null;
            for (final DisjunctionMaxClause clause : clauses) {
                if (!clause.isGenerated()) {
                    if (nonGenerated != null) {
                        // More than one non-generated clause — cannot determine a clear term order
                        return null;
                    }
                    nonGenerated = clause;
                }
            }
            if (nonGenerated != null) {
                nonGenerated.accept(this);
            }
            return null;
        }

        @Override
        public Node visit(final Term term) {
            if (!term.isGenerated()) {
                sequence.add(term.getValue().toString());
            }
            return null;
        }
    }

    @Override
    public RewriterOutput rewrite(final ExpandedQuery expandedQuery,
                                   final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        final QuerqyQuery<?> userQuery = expandedQuery.getUserQuery();
        if (!(userQuery instanceof Query)) {
            return RewriterOutput.builder().expandedQuery(expandedQuery).build();
        }

        final List<String> sequence = new TermSequenceCollector().collect((Query) userQuery);

        if (sequence.size() < 2) {
            return RewriterOutput.builder().expandedQuery(expandedQuery).build();
        }

        final List<PhraseQuery> phrases = new ArrayList<>();
        addBigrams(sequence, phrases);
        addTrigrams(sequence, phrases);
        addFullPhrase(sequence, phrases);

        if (!phrases.isEmpty()) {
            final Query wrapperQuery = new Query(true);
            final DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(wrapperQuery, Occur.SHOULD, true, tieBreaker);
            for (final PhraseQuery pq : phrases) {
                dmq.addClause(pq.clone(dmq, true));
            }
            wrapperQuery.addClause(dmq);
            expandedQuery.addBoostUpQuery(new BoostQuery(wrapperQuery, 1.0f));
        }

        return RewriterOutput.builder().expandedQuery(expandedQuery).build();
    }

    private void addBigrams(final List<String> sequence, final List<PhraseQuery> phrases) {
        if (bigramConfig == null) {
            return;
        }
        for (int i = 0; i < sequence.size() - 1; i++) {
            final List<String> terms = Arrays.asList(sequence.get(i), sequence.get(i + 1));
            for (final FieldAndBoost fab : bigramConfig.fields) {
                phrases.add(new BoostedPhraseQuery(null, Occur.SHOULD, fab.field, terms, bigramConfig.slop, fab.boost));
            }
        }
    }

    private void addTrigrams(final List<String> sequence, final List<PhraseQuery> phrases) {
        if (trigramConfig == null || sequence.size() < 3) {
            return;
        }
        for (int i = 0; i < sequence.size() - 2; i++) {
            final List<String> terms = Arrays.asList(sequence.get(i), sequence.get(i + 1), sequence.get(i + 2));
            for (final FieldAndBoost fab : trigramConfig.fields) {
                phrases.add(new BoostedPhraseQuery(null, Occur.SHOULD, fab.field, terms, trigramConfig.slop, fab.boost));
            }
        }
    }

    private void addFullPhrase(final List<String> sequence, final List<PhraseQuery> phrases) {
        if (fullConfig == null) {
            return;
        }
        for (final FieldAndBoost fab : fullConfig.fields) {
            phrases.add(new BoostedPhraseQuery(null, Occur.SHOULD, fab.field, new ArrayList<>(sequence),
                    fullConfig.slop, fab.boost));
        }
    }
}
