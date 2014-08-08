/**
 * 
 */
package querqy.solr;

import java.io.IOException;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.util.NamedList;

import querqy.rewrite.RewriterFactory;
import querqy.rewrite.lucene.LuceneSynonymsRewriterFactory;

/**
 * @author rene
 *
 */
public class SolrSynonymsRewriterFactory implements RewriterFactoryAdapter {

    /* (non-Javadoc)
     * @see querqy.solr.SolrRewriterFactory#createRewriterFactory(org.apache.solr.common.util.NamedList, org.apache.lucene.analysis.util.ResourceLoader)
     */
    @Override
    public RewriterFactory createRewriterFactory(NamedList<?> args, ResourceLoader resourceLoader) throws IOException {
        String synonymResoureName = (String) args.get("synonyms");
        if (synonymResoureName == null) {
            throw new IllegalArgumentException("Property 'synonyms' not configured");
        }
        Boolean expand = args.getBooleanArg("expand");
        if (expand == null) {
        	expand = false;
        }
        
        Boolean ignoreCase = args.getBooleanArg("ignoreCase");
        if (ignoreCase == null) {
        	ignoreCase = true;
        }
        return new LuceneSynonymsRewriterFactory(resourceLoader.openResource(synonymResoureName), expand, ignoreCase);
        
    }

}
