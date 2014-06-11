/**
 * 
 */
package querqy.rewrite;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import querqy.model.Query;

/**
 * @author rene
 *
 */
public class RewriteChain {
    
    final List<RewriterFactory> factories;
    
    public RewriteChain() {
        this(Collections.<RewriterFactory>emptyList());
    }
    
    public RewriteChain(List<RewriterFactory> factories) {
        this.factories = factories;
    }

    public Query rewrite(Query query, Map<String, ?> context) {
        Query work = query;
        for (RewriterFactory factory: factories) {
            QueryRewriter rewriter = factory.createRewriter(work, context);
            work = rewriter.rewrite(work);
        }
        return work;
    }
}
