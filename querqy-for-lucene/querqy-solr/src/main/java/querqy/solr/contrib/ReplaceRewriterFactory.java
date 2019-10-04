package querqy.solr.contrib;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.util.NamedList;
import querqy.model.BooleanQuery;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.commonrules.QuerqyParserFactory;
import querqy.rewrite.commonrules.WhiteSpaceQuerqyParserFactory;
import querqy.rewrite.contrib.ReplaceRewriter;
import querqy.solr.FactoryAdapter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ReplaceRewriterFactory implements FactoryAdapter<RewriterFactory> {

    private static final Boolean DEFAULT_IGNORE_CASE = true;
    private static final String DEFAULT_INPUT_DELIMITER = "\t";

    @Override
    public RewriterFactory createFactory(String id, NamedList<?> args, ResourceLoader resourceLoader) throws IOException {

        final String rulesResourceName = (String) args.get("rules");
        if (rulesResourceName == null) {
            throw new IllegalArgumentException("Property 'rules' not configured");
        }

        final InputStreamReader reader = new InputStreamReader(resourceLoader.openResource(rulesResourceName), StandardCharsets.UTF_8);

        final Boolean ignoreCase = args.getBooleanArg("ignoreCase");

        final String inputDelimiter = (String) args.get("inputDelimiter");

        // querqy parser for queries that are part of the instructions in the rules
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


        return new querqy.rewrite.contrib.ReplaceRewriterFactory(id, reader,
                ignoreCase != null ? ignoreCase : DEFAULT_IGNORE_CASE,
                inputDelimiter != null ? inputDelimiter : DEFAULT_INPUT_DELIMITER,
                querqyParser.createParser());
    }

    @Override
    public Class<?> getCreatedClass() {
        return ReplaceRewriter.class;
    }
}
