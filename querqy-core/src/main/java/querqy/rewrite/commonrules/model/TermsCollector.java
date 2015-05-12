/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.HashSet;
import java.util.Set;

import querqy.model.AbstractNodeVisitor;
import querqy.model.Query;
import querqy.model.Term;

/**
 * @author rene
 *
 */
public class TermsCollector extends AbstractNodeVisitor<Term> {

    protected final Set<Term> result = new HashSet<Term>();
    
    protected TermsCollector() {
        super();
    }
    
    public Set<Term> getResult() {
        return result;
    }

    @Override
    public Term visit(Term term) {
        result.add(term);
        return term;
    }
    
    public static Set<Term> collect(Query query) {
        TermsCollector collector = new TermsCollector();
        collector.visit(query);
        return collector.getResult();
    }
    
}
