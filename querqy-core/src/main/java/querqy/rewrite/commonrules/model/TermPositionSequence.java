/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.LinkedList;
import java.util.List;

import querqy.model.Term;

/**
 * @author rene
 *
 */
public class TermPositionSequence extends LinkedList<List<Term>>{
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    
    public TermPositionSequence() { }
    
    public void nextPosition() {
        add(new LinkedList<Term>());
    }
    
    /**
     * 
     * Adds a term at the current position
     * 
     * @param term
     */
    public void addTerm(Term term) {
        getLast().add(term);
    }
    
    

}
