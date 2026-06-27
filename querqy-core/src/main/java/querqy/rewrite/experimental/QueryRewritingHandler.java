/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2021 Querqy Contributors
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
package querqy.rewrite.experimental;

import static java.nio.charset.StandardCharsets.UTF_8;

import querqy.model.ExpandedQuery;
import querqy.model.convert.builder.ExpandedQueryBuilder;
import querqy.parser.QuerqyParser;
import querqy.rewrite.RewriteChain;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.commonrules.FieldAwareWhiteSpaceQuerqyParserFactory;
import querqy.rewrite.commonrules.QuerqyParserFactory;
import querqy.rewrite.commonrules.SimpleCommonRulesRewriterFactory;
import querqy.rewrite.commonrules.WhiteSpaceQuerqyParserFactory;
import querqy.rewrite.commonrules.model.BoostInstruction.BoostMethod;
import querqy.rewrite.commonrules.model.DecorateInstruction;
import querqy.rewrite.commonrules.select.ExpressionCriteriaSelectionStrategyFactory;
import querqy.rewrite.replace.ReplaceRewriterFactory;
import querqy.rewrite.lookup.preprocessing.LookupPreprocessorType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QueryRewritingHandler {

    private final RewriteChain rewriteChain;

    private static final QuerqyParserFactory QUERQY_PARSER_FACTORY = new FieldAwareWhiteSpaceQuerqyParserFactory();

    private QueryRewritingHandler(final RewriteChain rewriteChain) {
        this.rewriteChain = rewriteChain;
    }

    // TODO: Implement InfoLogging
    public RewrittenQuery rewriteQuery(final String queryString) {
        final Map<String, String[]> params = new HashMap<>();

        final QuerqyParser querqyParser = QUERQY_PARSER_FACTORY.createParser();
        final SearchEngineRequestAdapter adapter = new LocalSearchEngineRequestAdapter(this.rewriteChain, params);
        final ExpandedQuery inputQuery = new ExpandedQuery(querqyParser.parse(queryString));

        final ExpandedQueryBuilder expandedQueryBuilder = new ExpandedQueryBuilder(
                this.rewriteChain.rewrite(inputQuery, adapter).getExpandedQuery());

        final RewrittenQuery rewrittenQuery = new RewrittenQuery(expandedQueryBuilder);
        final Map<String, Object> context = adapter.getContext();

        if (context != null) {
            @SuppressWarnings("unchecked")
            final Set<Object> decorations = (Set<Object>) context.get(DecorateInstruction.DECORATION_CONTEXT_KEY);
            rewrittenQuery.setDecorations(decorations);

            @SuppressWarnings("unchecked")
            final Map<String, Object> namedDecorations =
                    (Map<String, Object>) context.get(DecorateInstruction.DECORATION_CONTEXT_MAP_KEY);
            rewrittenQuery.setNamedDecorations(namedDecorations);
        }

        return rewrittenQuery;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        final private List<RewriterFactory> rewriterFactories = new LinkedList<>();
        private int rewriterIdCounter = 0;

        // TODO: Replace these methods by a method addRewriterFactoryBuilder(...) and by builders in the respective
        //  factories
        public QueryRewritingHandler.Builder addReplaceRewriter(final String rules) throws IOException {
            final String rewriterId = "querqy_replace_" + this.rewriterIdCounter++;
            rewriterFactories.add(new ReplaceRewriterFactory(
                    rewriterId,
                    new InputStreamReader(new ByteArrayInputStream(rules.getBytes(UTF_8)), UTF_8),
                    true,
                    "\t",
                    new WhiteSpaceQuerqyParserFactory().createParser()));

            return this;
        }

        public QueryRewritingHandler.Builder addCommonRulesRewriter(final String rules) throws IOException {
            final String rewriterId = "querqy_commonrules_" + this.rewriterIdCounter++;
            rewriterFactories.add(new SimpleCommonRulesRewriterFactory(
                    rewriterId,
                    new StringReader(rules),
                    true,
                    BoostMethod.ADDITIVE,
                    new WhiteSpaceQuerqyParserFactory(),
                    Collections.emptyMap(),
                    new ExpressionCriteriaSelectionStrategyFactory(), false, LookupPreprocessorType.NONE));

            return this;
        }

        public QueryRewritingHandler build() {
            return new QueryRewritingHandler(new RewriteChain(Collections.unmodifiableList(this.rewriterFactories)));
        }
    }
}
