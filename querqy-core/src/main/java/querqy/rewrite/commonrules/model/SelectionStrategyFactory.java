package querqy.rewrite.commonrules.model;

import querqy.utils.Constants;

public class SelectionStrategyFactory {

    private static SelectionStrategyFactory ourInstance = new SelectionStrategyFactory();

    public static SelectionStrategyFactory getInstance() {
        return ourInstance;
    }

    private SelectionStrategyFactory() {
    }

    public SelectionStrategy getSelectionStrategy(String type) {
        switch (type) {
            case Constants
                    .DEFAULT_SELECTION_STRATEGY:
                return new DefaultSelectionStrategy();
            case Constants.CUSTOM_SELECTION_STRATEGY:
                return new CustomSelectionStrategy();
            default:
                return new DefaultSelectionStrategy();
        }
    }
}
