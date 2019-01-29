package querqy.solr.contrib;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.index.IndexReader;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrRequestInfo;
import querqy.rewrite.RewriterFactory;
import querqy.solr.RewriterFactoryAdapter;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

public class WordBreakCompoundRewriterFactory implements RewriterFactoryAdapter {

    private static final int DEFAULT_MIN_SUGGESTION_FREQ = 1;
    private static final int DEFAULT_MAX_COMBINE_LENGTH = 30;
    private static final int DEFAULT_MIN_BREAK_LENGTH = 3;
    private static final int DEFAULT_MAX_DECOMPOUND_EXPANSIONS = 3;

    @Override
    public RewriterFactory createRewriterFactory(final NamedList<?> args, ResourceLoader resourceLoader) {

        // the minimum frequency of the term in the index' dictionary field to be considered a valid compound
        // or constituent
        final Integer minSuggestionFreq = getOrDefault(args, "minSuggestionFrequency", DEFAULT_MIN_SUGGESTION_FREQ);

        // the maximum length of a combined term
        final Integer maxCombineLength = getOrDefault(args, "maxCombineWordLength", DEFAULT_MAX_COMBINE_LENGTH);

        // the minimum break term length
        final Integer minBreakLength = getOrDefault(args, "minBreakLength", DEFAULT_MIN_BREAK_LENGTH);

        // the index "dictionary" field to verify compounds / constituents
        final String indexField = (String) args.get("dictionaryField");

        // terms triggering a reversal of the surrounding compound, e.g. "tasche AUS samt" -> samttasche
        final List<String> reverseCompoundTriggerWords = (List<String>) args.get("reverseCompoundTriggerWords");

        final Integer maxDecompoundExpansions = getOrDefault(args, "decompound.maxExpansions",
                DEFAULT_MAX_DECOMPOUND_EXPANSIONS);

        if (maxDecompoundExpansions < 0) {
            throw new IllegalArgumentException("decompound.maxExpansions >= 0 expected. Found: "
                    + maxDecompoundExpansions);
        }

        final boolean verifyDecompoundCollation = getOrDefault(args, "decompound.verifyCollation", Boolean.FALSE);

        // define whether we should always try to add a reverse compound
        final boolean alwaysAddReverseCompounds = getOrDefault(args, "alwaysAddReverseCompounds", Boolean.FALSE);

        // the indexReader has to be supplied on a per-request basis from a request thread-local
        final Supplier<IndexReader> indexReaderSupplier = () ->
                SolrRequestInfo.getRequestInfo().getReq().getSearcher().getIndexReader();

        return new querqy.lucene.contrib.rewrite.WordBreakCompoundRewriterFactory(indexReaderSupplier, indexField,
                minSuggestionFreq, maxCombineLength, minBreakLength, reverseCompoundTriggerWords,
                alwaysAddReverseCompounds, maxDecompoundExpansions, verifyDecompoundCollation);
    }

    private static <T> T getOrDefault(final NamedList<?> args, String key, T def) {
        Object valueInParameter = args.get(key);
        return valueInParameter == null ? def : (T) valueInParameter;
    }
}
