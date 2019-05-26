/**
 * 
 */
package querqy.rewrite;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.infologging.InfoLoggingContext;

/**
 * The chain of rewriters to manipulate a {@link Query}.
 * 
 * @author rene
 *
 */
public class RewriteChain {

    final List<RewriterFactory> factories;
    final Map<String, RewriterFactory> factoriesByName;

    public RewriteChain() {
        this(Collections.emptyList());
    }

    public RewriteChain(final List<RewriterFactory> factories) {
        this.factories = factories;
        factoriesByName = new HashMap<>(factories.size());
        factories.forEach(factory -> {
            final String rewriterId = factory.getRewriterId();
            if (rewriterId == null || rewriterId.trim().isEmpty()) {
                throw new IllegalArgumentException("Missing rewriter id for factory: " + factory.getClass().getName());
            }
            if (factoriesByName.put(rewriterId, factory) != null) {
                throw new IllegalArgumentException("Duplicate rewriter id: " + rewriterId);
            }
        });
    }

    public ExpandedQuery rewrite(final ExpandedQuery query,
                                 final SearchEngineRequestAdapter searchEngineRequestAdapter) {
      
        ExpandedQuery work = query;

        final Optional<InfoLoggingContext> loggingContext = searchEngineRequestAdapter.getInfoLoggingContext();

        final String oldRewriterId = loggingContext.map(InfoLoggingContext::getRewriterId).orElse(null);

        try {

            for (final RewriterFactory factory : factories) {

                loggingContext.ifPresent(context -> context.setRewriterId(factory.getRewriterId()));

                final QueryRewriter rewriter = factory.createRewriter(work, searchEngineRequestAdapter);

                work = (rewriter instanceof ContextAwareQueryRewriter)
                        ? ((ContextAwareQueryRewriter) rewriter).rewrite(work, searchEngineRequestAdapter)
                        : rewriter.rewrite(work);

            }

        } finally {
            loggingContext.ifPresent(context -> context.setRewriterId(oldRewriterId));
        }

        return work;
    }
    
    public List<RewriterFactory> getRewriterFactories() {
        return factories;
    }

    public RewriterFactory getFactory(final String rewriterId) {
        return factoriesByName.get(rewriterId);
    }
}
