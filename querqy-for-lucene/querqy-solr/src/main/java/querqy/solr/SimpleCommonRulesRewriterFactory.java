/**
 *
 */
package querqy.solr;

import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.util.NamedList;

import querqy.rewrite.RewriterFactory;
import querqy.rewrite.commonrules.QuerqyParserFactory;
import querqy.rewrite.commonrules.WhiteSpaceQuerqyParserFactory;

/**
 * @author René Kriegler, @renekrie
 */
public class SimpleCommonRulesRewriterFactory implements RewriterFactoryAdapter {

    /*
     * (non-Javadoc)
     *
     * @see
     * querqy.solr.RewriterFactoryAdapter#createRewriterFactory(org.apache.solr
     * .common.util.NamedList, org.apache.lucene.analysis.util.ResourceLoader)
     */
    @Override
    public RewriterFactory createRewriterFactory(final NamedList<?> args,
                                                 final ResourceLoader resourceLoader) throws IOException {

        final String rulesResourceName = (String) args.get("rules");
        if (rulesResourceName == null) {
            throw new IllegalArgumentException("Property 'rules' not configured");
        }

        final Boolean ignoreCase = args.getBooleanArg("ignoreCase");

        // querqy parser for queries that are part of the instructions in the
        // rules
        String rulesQuerqyParser = (String) args.get("querqyParser");
        QuerqyParserFactory querqyParser = null;
        if (rulesQuerqyParser != null) {
            rulesQuerqyParser = rulesQuerqyParser.trim();
            if (rulesQuerqyParser.length() > 0) {
                querqyParser = resourceLoader.newInstance(rulesQuerqyParser, QuerqyParserFactory.class);
            }
        }

        if (querqyParser == null) {
            querqyParser = new WhiteSpaceQuerqyParserFactory();
        }

        return new querqy.rewrite.commonrules.SimpleCommonRulesRewriterFactory(
                new InputStreamReader(resourceLoader.openResource(rulesResourceName), "UTF-8"),
                querqyParser,
                ignoreCase == null || ignoreCase);
    }

}
