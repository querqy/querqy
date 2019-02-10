package querqy.rewrite.commonrules.model;

import java.util.Collections;
import java.util.Comparator;

public class ConfigurationOrderSelectionStrategy implements SelectionStrategy {

    public static final Comparator<Instructions> COMPARATOR = Comparator.comparingInt(o -> o.ord);


    @Override
    public TopRewritingActionCollector createTopRewritingActionCollector() {
        return new TopRewritingActionCollector(COMPARATOR, -1, Collections.emptyList());
    }


}
