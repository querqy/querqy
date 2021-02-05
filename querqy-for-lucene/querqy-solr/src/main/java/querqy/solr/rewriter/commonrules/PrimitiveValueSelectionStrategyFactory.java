package querqy.solr.rewriter.commonrules;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.util.NamedList;
import querqy.rewrite.commonrules.select.SelectionStrategyFactory;
import querqy.rewrite.commonrules.select.SelectionStrategy;
import querqy.solr.FactoryAdapter;

import java.util.Map;

public class PrimitiveValueSelectionStrategyFactory implements FactoryAdapter<SelectionStrategyFactory> {

    @Override
    public SelectionStrategyFactory createFactory(final String strategyId, final Map<String, Object> args) {
        // TODO: pass strategyId
        return new querqy.rewrite.commonrules.select.PrimitiveValueSelectionStrategyFactory();
    }

    @Override
    public Class<?> getCreatedClass() {
        return SelectionStrategy.class;
    }


}
