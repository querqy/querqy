/**
 * 
 */
package querqy.rewrite;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import querqy.model.ExpandedQuery;
import querqy.model.Query;

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

    public RewriteChain(List<RewriterFactory> factories) {
        this.factories = factories;
    }

    public ExpandedQuery rewrite(final ExpandedQuery query,
                                 final SearchEngineRequestAdapter searchEngineRequestAdapter) {
      
        ExpandedQuery work = query;
      
        for (final RewriterFactory factory : factories) {
         
            final QueryRewriter rewriter = factory.createRewriter(work, searchEngineRequestAdapter);
         
            work = (rewriter instanceof ContextAwareQueryRewriter)
                 ? ((ContextAwareQueryRewriter) rewriter).rewrite(work, searchEngineRequestAdapter)
                 : rewriter.rewrite(work);
         
        }
        return work;
    }
    
    public List<RewriterFactory> getRewriterFactories() {
        return factories;
    }
}
