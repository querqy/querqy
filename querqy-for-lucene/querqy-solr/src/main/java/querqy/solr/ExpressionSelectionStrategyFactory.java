package querqy.solr;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.util.NamedList;
import querqy.rewrite.commonrules.select.SelectionStrategyFactory;
import querqy.rewrite.commonrules.select.SelectionStrategy;
import querqy.rewrite.commonrules.select.ExpressionCriteriaSelectionStrategyFactory;

public class ExpressionSelectionStrategyFactory implements FactoryAdapter<SelectionStrategyFactory> {

    @Override
    public SelectionStrategyFactory createFactory(final String strategyId, final NamedList<?> args,
                                                  final ResourceLoader resourceLoader) {
        // TODO: pass strategyId
        return new ExpressionCriteriaSelectionStrategyFactory();
    }

    @Override
    public Class<?> getCreatedClass() {
        return SelectionStrategy.class;
    }
}
