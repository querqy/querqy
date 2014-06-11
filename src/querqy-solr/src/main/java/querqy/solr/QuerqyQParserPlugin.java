/**
 * 
 */
package querqy.solr;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.util.plugin.SolrCoreAware;

import querqy.rewrite.RewriteChain;
import querqy.rewrite.RewriterFactory;

/**
 * @author rene
 *
 */
public class QuerqyQParserPlugin extends QParserPlugin implements ResourceLoaderAware {

    protected NamedList<?> initArgs = null;
    protected RewriteChain rewriteChain = null;
    
    /* (non-Javadoc)
     * @see org.apache.solr.util.plugin.NamedListInitializedPlugin#init(org.apache.solr.common.util.NamedList)
     */
    
    @Override
    public void init(@SuppressWarnings("rawtypes") NamedList args) {
        this.initArgs = args;
    }

    /* (non-Javadoc)
     * @see org.apache.solr.search.QParserPlugin#createParser(java.lang.String, org.apache.solr.common.params.SolrParams, org.apache.solr.common.params.SolrParams, org.apache.solr.request.SolrQueryRequest)
     */
    @Override
    public QParser createParser(String qstr, SolrParams localParams, SolrParams params,
            SolrQueryRequest req) {
        return new QuerqyQParser(qstr, localParams, params, req, rewriteChain);
    }


    @Override
    public void inform(ResourceLoader loader) throws IOException {
        NamedList<?> chainConfig = (NamedList<?>) initArgs.get("rewriteChain");
        List<RewriterFactory> factories = new LinkedList<>();
        if (chainConfig != null) {
            @SuppressWarnings("unchecked")
            List<NamedList<?>> rewriterConfigs = (List<NamedList<?>>) chainConfig.getAll("rewriter");
            if (rewriterConfigs != null) {
                for (NamedList<?> config: rewriterConfigs) {
                    RewriterFactoryAdapter factory = loader.newInstance((String) config.get("class"), RewriterFactoryAdapter.class);
                    factories.add(factory.createRewriterFactory(config, loader));
                }
            }
        }
        
        rewriteChain = new RewriteChain(factories);
        
    }

}
