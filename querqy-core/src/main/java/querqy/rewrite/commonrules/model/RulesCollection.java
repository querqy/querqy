package querqy.rewrite.commonrules.model;

import java.util.List;

import querqy.model.InputSequenceElement;

public interface RulesCollection {

    public abstract List<Action> getRewriteActions(
            PositionSequence<InputSequenceElement> sequence);

}