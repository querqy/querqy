/**
 * 
 */
package querqy.rewrite;

import java.util.Collections;
import java.util.Set;

import querqy.model.ExpandedQuery;
import querqy.model.Term;

/**
 * A query rewriter.
 * 
 * @author rene
 *
 */
public interface QueryRewriter {
    
    static final Set<Term> EMPTY_GENERABLE_TERMS = Collections.emptySet();

    ExpandedQuery rewrite(ExpandedQuery query);

}
