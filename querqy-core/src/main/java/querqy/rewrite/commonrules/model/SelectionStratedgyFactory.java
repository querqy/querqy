package querqy.rewrite.commonrules.model;

import querqy.utils.Constants;

public class SelectionStratedgyFactory {

    private static SelectionStratedgyFactory ourInstance = new SelectionStratedgyFactory();

    public static SelectionStratedgyFactory getInstance() {
        return ourInstance;
    }

    private SelectionStratedgyFactory() {
    }

    public SelectionStratedgy getSelectionStratedgy(String type) {
        switch (type) {
            case Constants
                    .DEFAULT_SELECTION_STRATEDGY:
                return new DefaultSelectionStratedgy();
            case Constants.CUSTOM_SELECTION_STRATEDGY:
                return new CustomSelectionStratedgy();
            default:
                return new DefaultSelectionStratedgy();
        }
    }
}
