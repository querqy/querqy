package querqy.rewrite.commonrules.select;

import querqy.rewrite.SearchEngineRequestAdapter;

public interface SelectionStrategyFactory {

    SelectionStrategy DEFAULT_SELECTION_STRATEGY = new ConfigurationOrderSelectionStrategy();

    SelectionStrategy createSelectionStrategy(String rewriterId, SearchEngineRequestAdapter searchEngineRequestAdapter);


}
