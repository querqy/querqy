/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.Collection;
import java.util.LinkedList;

/**
 * A list of Instructions. This represents all actions that are triggered by a single matching rule.
 * 
 * @author rene
 *
 */
public class Instructions extends LinkedList<Instruction> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    public Instructions() {
        super();
    }
    
    public Instructions(Collection<Instruction> instructions) {
        super(instructions);
    }
    

}
