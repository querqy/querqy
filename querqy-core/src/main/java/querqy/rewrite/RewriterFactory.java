/**
 * 
 */
package querqy.rewrite;

import java.util.Map;
import java.util.Set;

import querqy.model.ExpandedQuery;
import querqy.model.Term;

/**
 * @author rene
 *
 */
public interface RewriterFactory {

   QueryRewriter createRewriter(ExpandedQuery input, Map<String, ?> context);
   
   Set<Term> getGenerableTerms();

}
