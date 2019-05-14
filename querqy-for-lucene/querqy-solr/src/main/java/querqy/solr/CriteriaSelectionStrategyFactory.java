package querqy.solr;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.util.NamedList;
import querqy.rewrite.commonrules.SelectionStrategyFactory;
import querqy.rewrite.commonrules.model.SelectionStrategy;

public class CriteriaSelectionStrategyFactory implements FactoryAdapter<SelectionStrategyFactory> {

    @Override
    public SelectionStrategyFactory createFactory(final String strategyId, final NamedList<?> args,
                                                  final ResourceLoader resourceLoader) {
        // TODO: pass strategyId
        return new querqy.rewrite.commonrules.CriteriaSelectionStrategyFactory();
    }

    @Override
    public Class<?> getCreatedClass() {
        return SelectionStrategy.class;
    }


}
