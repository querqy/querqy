package querqy.rewrite.replace;

import querqy.model.ExpandedQuery;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.SearchEngineRequestAdapter;

import java.io.IOException;
import java.io.InputStreamReader;

public class RegexReplaceRewriterFactory extends RewriterFactory {

    private final RegexReplacing replacing;

    public RegexReplaceRewriterFactory(final String rewriterId, final InputStreamReader reader,
                                       final boolean ignoreCase) throws IOException {
        super(rewriterId);

        this.replacing = new RegexReplaceRewriterRulesParser(reader, ignoreCase).parserConfig();

    }

    @Override
    public QueryRewriter createRewriter(final ExpandedQuery input,
                                        final SearchEngineRequestAdapter searchEngineRequestAdapter) {

        return new RegexReplaceRewriter(replacing);
    }
}
