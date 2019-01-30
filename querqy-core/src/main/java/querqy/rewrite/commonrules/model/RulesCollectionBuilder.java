package querqy.rewrite.commonrules.model;

import querqy.rewrite.commonrules.Properties;

public interface RulesCollectionBuilder {

    @Deprecated
    public abstract void addRule(Input input, Instructions instructions);

    public abstract RulesCollection build();

    public abstract void addRule(Input input, Properties properties);

}