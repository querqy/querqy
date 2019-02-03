package querqy.rewrite.commonrules;

import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.commonrules.model.ConfigurationOrderSelectionStrategy;
import querqy.rewrite.commonrules.model.SelectionStrategy;

public interface SelectionStrategyFactory {

    SelectionStrategy DEFAULT_SELECTION_STRATEGY = new ConfigurationOrderSelectionStrategy();

    SelectionStrategy createSelectionStrategy(SearchEngineRequestAdapter searchEngineRequestAdapter);


}
