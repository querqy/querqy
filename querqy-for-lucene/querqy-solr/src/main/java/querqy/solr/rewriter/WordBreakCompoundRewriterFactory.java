package querqy.solr.rewriter;

import org.apache.lucene.index.IndexReader;
import org.apache.solr.request.SolrRequestInfo;
import querqy.lucene.contrib.rewrite.wordbreak.Morphology;
import querqy.rewrite.RewriterFactory;
import querqy.solr.utils.ConfigUtils;
import querqy.solr.SolrRewriterFactoryAdapter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class WordBreakCompoundRewriterFactory extends SolrRewriterFactoryAdapter {

    private static final int DEFAULT_MIN_SUGGESTION_FREQ = 1;
    private static final int DEFAULT_MAX_COMBINE_LENGTH = 30;
    private static final int DEFAULT_MIN_BREAK_LENGTH = 3;
    private static final int DEFAULT_MAX_DECOMPOUND_EXPANSIONS = 3;
    private static final boolean DEFAULT_VERIFY_DECOMPOUND_COLLATION = false;


    private querqy.lucene.contrib.rewrite.wordbreak.WordBreakCompoundRewriterFactory delegate = null;

    public WordBreakCompoundRewriterFactory(final String rewriterId) {
        super(rewriterId);
    }

    @Override
    public void configure(final Map<String, Object> config) {
        // the minimum frequency of the term in the index' dictionary field to be considered a valid compound
        // or constituent
        final Integer minSuggestionFreq = ConfigUtils.getArg(config, "minSuggestionFrequency",
                DEFAULT_MIN_SUGGESTION_FREQ);

        // the maximum length of a combined term
        final Integer maxCombineLength = ConfigUtils.getArg(config, "maxCombineWordLength", DEFAULT_MAX_COMBINE_LENGTH);

        // the minimum break term length
        final Integer minBreakLength = ConfigUtils.getArg(config, "minBreakLength", DEFAULT_MIN_BREAK_LENGTH);

        // the index "dictionary" field to verify compounds / constituents
        final String indexField = (String) config.get("dictionaryField");

        // whether query strings should be turned into lower case before trying to compound/decompound
        final boolean lowerCaseInput = ConfigUtils.getArg(config, "lowerCaseInput", Boolean.FALSE);

        // terms triggering a reversal of the surrounding compound, e.g. "tasche AUS samt" -> samttasche
        final List<String> reverseCompoundTriggerWords = (List<String>) config.get("reverseCompoundTriggerWords");

        final Map<String, Object> decompoundConf = ConfigUtils.getArg(config, "decompound", Collections.emptyMap());

        final int maxDecompoundExpansions = ConfigUtils.getArg(decompoundConf, "maxExpansions",
                DEFAULT_MAX_DECOMPOUND_EXPANSIONS);

        final boolean verifyDecompoundCollation =  ConfigUtils.getArg(decompoundConf, "verifyCollation",
                DEFAULT_VERIFY_DECOMPOUND_COLLATION);

        if (maxDecompoundExpansions < 0) {
            throw new IllegalArgumentException("decompound.maxExpansions >= 0 expected. Found: "
                    + maxDecompoundExpansions);
        }


        // define whether we should always try to add a reverse compound
        final boolean alwaysAddReverseCompounds = ConfigUtils.getArg(config, "alwaysAddReverseCompounds", Boolean.FALSE);

        final Morphology morphology = ConfigUtils.getEnumArg(config, "morphology", Morphology.class)
                .orElse(Morphology.DEFAULT);

        // the indexReader has to be supplied on a per-request basis from a request thread-local
        final Supplier<IndexReader> indexReaderSupplier = () ->
                SolrRequestInfo.getRequestInfo().getReq().getSearcher().getIndexReader();

        delegate  = new querqy.lucene.contrib.rewrite.wordbreak.WordBreakCompoundRewriterFactory(rewriterId,
                indexReaderSupplier, morphology, indexField, lowerCaseInput, minSuggestionFreq, maxCombineLength,
                minBreakLength,reverseCompoundTriggerWords, alwaysAddReverseCompounds, maxDecompoundExpansions,
                verifyDecompoundCollation);
    }

    @Override
    public List<String> validateConfiguration(final Map<String, Object> config) {
        try {
            ConfigUtils.getEnumArg(config, "morphology", Morphology.class);
        } catch (final Exception e) {
            return Collections.singletonList("Cannot load morphology: " + config.get("morphology"));
        }

        final Map<String, Object> decompoundConf = ConfigUtils.getArg(config, "decompound", Collections.emptyMap());
        final int maxDecompoundExpansions = ConfigUtils.getArg(decompoundConf, "maxExpansions",
                DEFAULT_MAX_DECOMPOUND_EXPANSIONS);
        if (maxDecompoundExpansions < 0) {
            return Collections.singletonList("maxDecompoundExpansions >= 0 expected");
        }

        final Optional<String> optValue = ConfigUtils.getStringArg(config, "dictionaryField").map(String::trim)
                .filter(s -> !s.isEmpty());
        // TODO: can we validate the dictionary field against the schema?
        return optValue.isPresent() ? null : Collections.singletonList("Missing config:  dictionaryField");
    }

    @Override
    public RewriterFactory getRewriterFactory() {
        return delegate;
    }
}
