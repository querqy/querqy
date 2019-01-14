package querqy.rewrite.commonrules.model;

public interface RulesCollectionBuilder {

    public abstract void addRule(Input input, Properties properties);

    public abstract RulesCollection build();

}