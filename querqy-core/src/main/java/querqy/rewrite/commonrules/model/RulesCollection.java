package querqy.rewrite.commonrules.model;

import java.util.Set;

import querqy.model.InputSequenceElement;

public interface RulesCollection {

    /** 
     * Find and return all rewrite actions for an input sequence
     * @param sequence
     * @param collector
     */
    void collectRewriteActions(PositionSequence<InputSequenceElement> sequence, TopRewritingActionCollector collector);
    
    /**
     * 
     * Get a collection of all instructions from the rules of this RulesCollection
     * 
     * @return
     */
    Set<Instruction> getInstructions();
    

}