package querqy.solr.contrib;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.util.NamedList;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.commonrules.QuerqyParserFactory;
import querqy.rewrite.commonrules.WhiteSpaceQuerqyParserFactory;
import querqy.solr.FactoryAdapter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ReplaceRewriterFactory implements FactoryAdapter<RewriterFactory> {
    @Override
    public RewriterFactory createFactory(String id, NamedList<?> args, ResourceLoader resourceLoader) throws IOException {

        final String rulesResourceName = (String) args.get("rules");
        if (rulesResourceName == null) {
            throw new IllegalArgumentException("Property 'rules' not configured");
        }

        final InputStreamReader reader = new InputStreamReader(resourceLoader.openResource(rulesResourceName), StandardCharsets.UTF_8);

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


        return new querqy.rewrite.contrib.ReplaceRewriterFactory(id, reader, ignoreCase, querqyParser.createParser());
    }

    @Override
    public Class<?> getCreatedClass() {
        return null;
    }
}
