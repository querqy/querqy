package querqy.rewrite.commonrules.model;

import java.util.List;

import querqy.model.Term;

public interface RulesCollection {

    public abstract List<Action> getRewriteActions(
            PositionSequence<Term> sequence);

}