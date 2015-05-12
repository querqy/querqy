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
        this(Collections.<RewriterFactory> emptyList());
    }

    public RewriteChain(List<RewriterFactory> factories) {
        this.factories = factories;
    }

    public ExpandedQuery rewrite(ExpandedQuery query, Map<String, Object> context) {
      
        ExpandedQuery work = query;
      
        for (RewriterFactory factory : factories) {
         
            QueryRewriter rewriter = factory.createRewriter(work, context);
         
            work = (rewriter instanceof ContextAwareQueryRewriter)
                 ? ((ContextAwareQueryRewriter) rewriter).rewrite(work, context)
                 : rewriter.rewrite(work);
         
        }
        return work;
    }
    
    public List<RewriterFactory> getRewriterFactories() {
        return factories;
    }
}
