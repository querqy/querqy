package querqy.rewrite.commonrules.model;

import java.util.List;
import java.util.Set;

import querqy.model.InputSequenceElement;

public interface RulesCollection {

    /** 
     * Find and return all rewrite actions for an input sequence
     * @param sequence
     * @return
     */
    List<Action> getRewriteActions(PositionSequence<InputSequenceElement> sequence);
    
    /**
     * 
     * Get a collection of all instructions from the rules of this RulesCollection
     * 
     * @return
     */
    Set<Instruction> getInstructions();
    

}