/**
 * 
 */
package querqy.solr;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.index.Term;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.SolrIndexSearcher;

import querqy.parser.QuerqyParser;
import querqy.rewrite.RewriteChain;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.lucene.IndexStats;

/**
 * @author rene
 *
 */
public class QuerqyQParserPlugin extends QParserPlugin implements ResourceLoaderAware {

    protected NamedList<?> initArgs = null;
    protected RewriteChain rewriteChain = null;
    protected Class<? extends QuerqyParser> querqyParserClass;
    
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
    	
    	QuerqyParser querqyParser = createQuerqyParser(qstr, localParams, params, req);
    	
		return new QuerqyQParser(qstr, localParams, params, req, rewriteChain, new SolrIndexStats(req.getSearcher()), querqyParser);
		
    }


    @Override
    public void inform(ResourceLoader loader) throws IOException {
    	
        rewriteChain = loadRewriteChain(loader);
        querqyParserClass = loadQuerqyParserClass(loader);
        
    }
    
    public QuerqyParser createQuerqyParser(String qstr, SolrParams localParams, SolrParams params,
            SolrQueryRequest req) {
    	try {
    		return querqyParserClass.newInstance();
    	} catch (InstantiationException|IllegalAccessException e) {
    		throw new RuntimeException("Could not create QuerqyParser", e);
    	} 
    }
    
    public RewriteChain loadRewriteChain(ResourceLoader loader) throws IOException {
    	
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
        
        return new RewriteChain(factories);
    }
    
    public Class<? extends QuerqyParser> loadQuerqyParserClass(ResourceLoader loader) throws IOException  {
    	
    	NamedList<?> parserConfig = (NamedList<?>) initArgs.get("parser");
    	if (parserConfig == null) {
    		throw new IOException("Missing querqy parser configuration");
    	}
    	
    	String className = (String) parserConfig.get("class");
    	if (className == null) {
    		throw new IOException("Missing attribute 'class' in querqy parser configuration");
    	}
    	
    	return loader.findClass(className, QuerqyParser.class);
    }
    
    class SolrIndexStats implements IndexStats {
        final SolrIndexSearcher searcher;
        public SolrIndexStats(SolrIndexSearcher searcher) {
            this.searcher = searcher;
        }
        @Override
        public int df(Term term) {
            try {
                return searcher.docFreq(term);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        
    }

}
