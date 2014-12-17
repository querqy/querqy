package querqy.rewrite.commonrules.model;

public interface RulesCollectionBuilder {

    public abstract void addRule(Input input, Instructions instructions);

    public abstract RulesCollection build();

}