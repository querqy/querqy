package querqy.rewrite.commonrules.select;

import querqy.rewrite.commonrules.model.Instructions;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ConfigurationOrderSelectionStrategy implements SelectionStrategy {

    public static final List<Comparator<Instructions>> COMPARATORS = Collections.singletonList(
            Sorting.DEFAULT_COMPARATOR);


    @Override
    public FlatTopRewritingActionCollector createTopRewritingActionCollector() {
        return new FlatTopRewritingActionCollector(COMPARATORS, -1, Collections.emptyList());
    }


}
