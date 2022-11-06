/**
 * 
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

    final List<RewriterFactory> factories;

    public RewriteChain() {
        this(Collections.emptyList());
    }

    public RewriteChain(final List<RewriterFactory> factories) {
        this.factories = factories;
        ensureThatRewriterIdsAreValid();
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

        private final RewriteChainLog.RewriteChainLogBuilder rewriteChainLoggingBuilder = RewriteChainLog.builder();

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

                if (rewriteLoggingConfig.isActive() && rewriterOutput.getRewriterLogging().isPresent()) {
                    addLoggingIfRewritingHasBeenApplied(
                            factory.getRewriterId(), rewriterOutput.getRewriterLogging().get());
                }

                expandedQuery = rewriterOutput.getExpandedQuery();
            }

            return buildOutput();
        }

        private RewriterOutput applyFactory(final RewriterFactory factory) {
            final QueryRewriter rewriter = factory.createRewriter(expandedQuery, searchEngineRequestAdapter);
            return rewriter.rewrite(expandedQuery, searchEngineRequestAdapter);
        }

        private void addLoggingIfRewritingHasBeenApplied(final String factoryId, final RewriterLog rewriterLogging) {
            if (rewriterLogging.hasAppliedRewriting()) {
                addLoggingIfRewriterIdIsIncluded(factoryId, rewriterLogging);
            }
        }

        private void addLoggingIfRewriterIdIsIncluded(final String factoryId, final RewriterLog rewriterLogging) {
            final Set<String> includedIds = rewriteLoggingConfig.getIncludedRewriters();

            if (includedIds.isEmpty() || includedIds.contains(factoryId)) {
                addLoggingWithOrWithoutDetails(factoryId, rewriterLogging);
            }
        }

        private void addLoggingWithOrWithoutDetails(final String factoryId, final RewriterLog rewriterLogging) {
            if (rewriteLoggingConfig.hasDetails()) {
                rewriteChainLoggingBuilder.add(factoryId, rewriterLogging.getActionLoggings());

            } else {
                rewriteChainLoggingBuilder.add(factoryId);
            }
        }

        private RewriteChainOutput buildOutput() {
            return RewriteChainOutput.builder()
                    .expandedQuery(expandedQuery)
                    .rewriteLogging(rewriteChainLoggingBuilder.build())
                    .build();
        }
    }
}
