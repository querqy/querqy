package querqy.rewrite.commonrules.model;

public interface RulesCollectionBuilder {

    void addRule(Input input, Instructions instructions);

    RulesCollection build();

}