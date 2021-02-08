package querqy.rewrite.commonrules.model;

import querqy.model.Input;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInputLiteral;

public interface RulesCollectionBuilder {

    void addRule(Input.SimpleInput input, Instructions instructions);

    void addRule(Input.SimpleInput input, BooleanInputLiteral literal);

    RulesCollection build();

}