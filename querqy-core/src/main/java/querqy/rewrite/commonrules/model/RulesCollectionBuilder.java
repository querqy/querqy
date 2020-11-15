package querqy.rewrite.commonrules.model;

import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInputLiteral;

public interface RulesCollectionBuilder {

    void addRule(Input input, Instructions instructions);

    void addRule(Input input, BooleanInputLiteral literal);

    RulesCollection build();

}