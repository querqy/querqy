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

import querqy.model.ExpandedQuery;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.contrib.PhraseBoostRewriter.FieldAndBoost;
import querqy.rewrite.contrib.PhraseBoostRewriter.PhraseTypeConfig;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Factory for {@link PhraseBoostRewriter}. Because the rewriter is stateless it is
 * created once and returned on every {@link #createRewriter} call.
 */
public class PhraseBoostRewriterFactory extends RewriterFactory {

    private final PhraseBoostRewriter rewriter;

    /**
     * @param rewriterId    identifier for this rewriter in the rewrite chain
     * @param bigramConfig  bigram phrase boost config, or {@code null} to disable
     * @param trigramConfig trigram phrase boost config, or {@code null} to disable
     * @param fullConfig    full-phrase boost config, or {@code null} to disable
     * @param tieBreaker    tie-breaker for combining sub-phrase scores (0 = max wins, 1 = sum)
     */
    public PhraseBoostRewriterFactory(final String rewriterId,
                                       final PhraseTypeConfig bigramConfig,
                                       final PhraseTypeConfig trigramConfig,
                                       final PhraseTypeConfig fullConfig,
                                       final float tieBreaker) {
        super(rewriterId);
        this.rewriter = new PhraseBoostRewriter(bigramConfig, trigramConfig, fullConfig, tieBreaker);
    }

    /**
     * Convenience constructor that parses field specs such as {@code "title"} or
     * {@code "brand^4"} into {@link FieldAndBoost} instances.
     *
     * @param rewriterId     identifier for this rewriter in the rewrite chain
     * @param bigramFields   field specs for bigram boosts, or {@code null}/empty to disable
     * @param bigramSlop     slop for bigram phrase queries
     * @param trigramFields  field specs for trigram boosts, or {@code null}/empty to disable
     * @param trigramSlop    slop for trigram phrase queries
     * @param fullFields     field specs for full-phrase boost, or {@code null}/empty to disable
     * @param fullSlop       slop for full phrase query
     * @param tieBreaker     tie-breaker for combining sub-phrase scores
     */
    public static PhraseBoostRewriterFactory create(
            final String rewriterId,
            final List<String> bigramFields,  final int bigramSlop,
            final List<String> trigramFields, final int trigramSlop,
            final List<String> fullFields,    final int fullSlop,
            final float tieBreaker) {

        return new PhraseBoostRewriterFactory(rewriterId,
                toConfig(bigramFields,  bigramSlop),
                toConfig(trigramFields, trigramSlop),
                toConfig(fullFields,    fullSlop),
                tieBreaker);
    }

    private static PhraseTypeConfig toConfig(final List<String> fieldSpecs, final int slop) {
        if (fieldSpecs == null || fieldSpecs.isEmpty()) {
            return null;
        }
        final List<FieldAndBoost> fields = fieldSpecs.stream()
                .map(FieldAndBoost::parse)
                .collect(Collectors.toList());
        return new PhraseTypeConfig(fields, slop);
    }

    @Override
    public QueryRewriter createRewriter(final ExpandedQuery input,
                                         final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        return rewriter;
    }

    @Override
    public Set<Term> getCacheableGenerableTerms() {
        return QueryRewriter.EMPTY_GENERABLE_TERMS;
    }
}
