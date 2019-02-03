package querqy.solr;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.util.NamedList;
import querqy.rewrite.commonrules.SelectionStrategyFactory;

public class CriteriaSelectionStrategyFactory implements FactoryAdapter<SelectionStrategyFactory> {

    @Override
    public SelectionStrategyFactory createFactory(final NamedList<?> args, final ResourceLoader resourceLoader) {
        // TODO: configure id field using args
        return new querqy.rewrite.commonrules.CriteriaSelectionStrategyFactory();
    }
}
