package querqy.rewrite.contrib;

import querqy.ComparableCharSequence;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.Term;
import querqy.parser.QuerqyParser;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.trie.TrieMap;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;

public class ReplaceRewriterFactory extends RewriterFactory {

    private final TrieMap<List<ComparableCharSequence>> replaceRules;
    private final boolean ignoreCase;

    public ReplaceRewriterFactory(final String id,
                                  final InputStreamReader reader,
                                  final boolean ignoreCase,
                                  final String inputDelimiter,
                                  final QuerqyParser querqyParser) throws IOException {
        super(id);
        this.ignoreCase = ignoreCase;
        replaceRules = new ReplaceRewriterParser(reader, this.ignoreCase, inputDelimiter, querqyParser).parseConfig();
    }

    @Override
    public QueryRewriter createRewriter(ExpandedQuery input, SearchEngineRequestAdapter searchEngineRequestAdapter) {
        return new ReplaceRewriter(this.replaceRules, this.ignoreCase);
    }

    @Override
    public Set<Term> getGenerableTerms() {
        return QueryRewriter.EMPTY_GENERABLE_TERMS;
    }
}
