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
package querqy.rewrite;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.rewrite.logging.RewriteChainLog;
import querqy.rewrite.logging.RewriterLog;

/**
 * The chain of rewriters to manipulate a {@link Query}.
 * 
 * @author rene
 *
 */
public class RewriteChain {

    private final List<RewriterFactory> factories;

    public RewriteChain() {
        this(Collections.emptyList());
    }

    public RewriteChain(final List<RewriterFactory> factories) {
        this.factories = factories;
        ensureThatRewriterIdsAreValid();
    }

    public List<RewriterFactory> getFactories() {
        return factories;
    }

    private void ensureThatRewriterIdsAreValid() {
        final Set<String> rewriterIds = new HashSet<>();

        for (final RewriterFactory factory : this.factories) {
            final String rewriterId = factory.getRewriterId();

            if (rewriterId == null || rewriterId.trim().isEmpty()) {
                throw new IllegalArgumentException("Missing rewriter id for factory: " + factory.getClass().getName());
            }

            if (rewriterIds.contains(rewriterId)) {
                throw new IllegalArgumentException("Duplicate rewriter id: " + rewriterId);
            }

            rewriterIds.add(rewriterId);
        }
    }

    public RewriteChainOutput rewrite(final ExpandedQuery query,
                                      final SearchEngineRequestAdapter searchEngineRequestAdapter) {

        final RewritingExecutor executor = new RewritingExecutor(factories, searchEngineRequestAdapter, query);
        return executor.rewrite();
    }

    private static class RewritingExecutor {

        private final List<RewriterFactory> rewriterFactories;

        private final SearchEngineRequestAdapter searchEngineRequestAdapter;
        private final RewriteLoggingConfig rewriteLoggingConfig;

        private ExpandedQuery expandedQuery;

        private final RewriteChainLog.RewriteChainLogBuilder rewriteChainLogBuilder = RewriteChainLog.builder();

        public RewritingExecutor(
                final List<RewriterFactory> rewriterFactories,
                final SearchEngineRequestAdapter searchEngineRequestAdapter,
                final ExpandedQuery expandedQuery
        ) {
            this.rewriterFactories = rewriterFactories;

            this.searchEngineRequestAdapter = searchEngineRequestAdapter;
            this.rewriteLoggingConfig = searchEngineRequestAdapter.getRewriteLoggingConfig();

            this.expandedQuery = expandedQuery;
        }

        public RewriteChainOutput rewrite() {
            for (final RewriterFactory factory : rewriterFactories) {
                final RewriterOutput rewriterOutput = applyFactory(factory);

                if (rewriteLoggingConfig.isActive() && rewriterOutput.getRewriterLog().isPresent()) {
                    addLogIfRewritingHasBeenApplied(
                            factory.getRewriterId(), rewriterOutput.getRewriterLog().get());
                }

                expandedQuery = rewriterOutput.getExpandedQuery();
            }

            return buildOutput();
        }

        private RewriterOutput applyFactory(final RewriterFactory factory) {
            final QueryRewriter rewriter = factory.createRewriter(searchEngineRequestAdapter);
            return rewriter.rewrite(expandedQuery, searchEngineRequestAdapter);
        }

        private void addLogIfRewritingHasBeenApplied(final String factoryId, final RewriterLog rewriterLog) {
            if (rewriterLog.hasAppliedRewriting()) {
                addLogIfRewriterIdIsIncluded(factoryId, rewriterLog);
            }
        }

        private void addLogIfRewriterIdIsIncluded(final String factoryId, final RewriterLog rewriterLog) {
            final Set<String> includedIds = rewriteLoggingConfig.getIncludedRewriters();

            if (includedIds.contains(factoryId)) {
                addLogWithOrWithoutDetails(factoryId, rewriterLog);
            }
        }

        private void addLogWithOrWithoutDetails(final String factoryId, final RewriterLog rewriterLog) {
            if (rewriteLoggingConfig.hasDetails()) {
                rewriteChainLogBuilder.add(factoryId, rewriterLog.getActionLogs());

            } else {
                rewriteChainLogBuilder.add(factoryId);
            }
        }

        private RewriteChainOutput buildOutput() {
            return RewriteChainOutput.builder()
                    .expandedQuery(expandedQuery)
                    .rewriteLog(rewriteChainLogBuilder.build())
                    .build();
        }
    }
}
