/**
 * 
 */
package querqy.solr;

import java.io.IOException;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.util.NamedList;

import querqy.rewrite.RewriterFactory;

/**
 * @author rene
 *
 */
public class SimpleCommonRulesRewriterFactory implements RewriterFactoryAdapter {

    /* (non-Javadoc)
     * @see querqy.solr.RewriterFactoryAdapter#createRewriterFactory(org.apache.solr.common.util.NamedList, org.apache.lucene.analysis.util.ResourceLoader)
     */
    @Override
    public RewriterFactory createRewriterFactory(NamedList<?> args,
            ResourceLoader resourceLoader) throws IOException {
        String rulesResourceName = (String) args.get("rules");
        if (rulesResourceName == null) {
            throw new IllegalArgumentException("Property 'rules' not configured");
        }
        return new querqy.antlr.rewrite.commonrules.SimpleCommonRulesRewriterFactory(resourceLoader.openResource(rulesResourceName));
    }

}
