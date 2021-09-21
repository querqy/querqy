package querqy.rewrite.commonrules.model;

import querqy.model.Input;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInputLiteral;
import querqy.rewrite.rules.rule.Rule;

public interface RulesCollectionBuilder {

    void addRule(Input.SimpleInput input, Instructions instructions);

    void addRule(Input.SimpleInput input, BooleanInputLiteral literal);

    void addRule(final Rule rule);

    RulesCollection build();

}