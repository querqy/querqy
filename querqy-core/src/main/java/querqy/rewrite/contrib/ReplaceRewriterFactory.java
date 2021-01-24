package querqy.rewrite.contrib;

import querqy.model.ExpandedQuery;
import querqy.model.Term;
import querqy.parser.QuerqyParser;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.contrib.replace.ReplaceInstruction;
import querqy.trie.SequenceLookup;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;

public class ReplaceRewriterFactory extends RewriterFactory {

    private final SequenceLookup<ReplaceInstruction> sequenceLookup;
    private final boolean ignoreCase;

    public ReplaceRewriterFactory(final String id,
                                  final InputStreamReader reader,
                                  final boolean ignoreCase,
                                  final String inputDelimiter,
                                  final QuerqyParser querqyParser) throws IOException {
        super(id);
        this.ignoreCase = ignoreCase;
        sequenceLookup = new querqy.rewrite.contrib.replace.ReplaceRewriterParser(reader, this.ignoreCase, inputDelimiter, querqyParser).parseConfig();
    }

    @Override
    public QueryRewriter createRewriter(ExpandedQuery input, SearchEngineRequestAdapter searchEngineRequestAdapter) {
        return new ReplaceRewriter(sequenceLookup);
    }

    @Override
    public Set<Term> getCacheableGenerableTerms() {
        return QueryRewriter.EMPTY_GENERABLE_TERMS;
    }
}
