package querqy.solr.rewriter.wordbreak;

import org.apache.lucene.index.IndexReader;
import org.apache.solr.request.SolrRequestInfo;
import querqy.lucene.contrib.rewrite.wordbreak.Morphology;
import querqy.rewrite.RewriterFactory;
import querqy.solr.rewriter.ClassicConfigurationParser;
import querqy.solr.utils.ConfigUtils;
import querqy.solr.SolrRewriterFactoryAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class WordBreakCompoundRewriterFactory extends SolrRewriterFactoryAdapter implements ClassicConfigurationParser {

    public static final String CONF_DICTIONARY_FIELD = "dictionaryField";
    public static final String CONF_MIN_SUGGESTION_FREQ = "minSuggestionFrequency";
    public static final String CONF_MAX_COMBINE_WORD_LENGTH = "maxCombineWordLength";
    public static final String CONF_MIN_BREAK_LENGTH = "minBreakLength";
    public static final String CONF_LOWER_CASE_INPUT = "lowerCaseInput";
    public static final String CONF_REVERSE_COMPOUND_TRIGGER_WORDS = "reverseCompoundTriggerWords";
    public static final String CONF_ALWAYS_ADD_REVERSE_COMPOUNDS = "alwaysAddReverseCompounds";
    public static final String CONF_MORPHOLOGY = "morphology";
    public static final String CONF_DECOMPOUND = "decompound";
    public static final String CONF_DECOMPOUND_MAX_EXPANSIONS = "maxExpansions";
    public static final String CONF_DECOMPOUND_VERIFY_COLLATION = "verifyCollation";
    public static final String CONF_PROTECTED_WORDS = "protectedWords";


    private static final int DEFAULT_MIN_SUGGESTION_FREQ = 1;
    private static final int DEFAULT_MAX_COMBINE_WORD_LENGTH = 30;
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
        final Integer minSuggestionFreq = ConfigUtils.getArg(config, CONF_MIN_SUGGESTION_FREQ,
                DEFAULT_MIN_SUGGESTION_FREQ);

        // the maximum length of a combined term
        final Integer maxCombineLength = ConfigUtils.getArg(config, CONF_MAX_COMBINE_WORD_LENGTH,
                DEFAULT_MAX_COMBINE_WORD_LENGTH);

        // the minimum break term length
        final Integer minBreakLength = ConfigUtils.getArg(config, CONF_MIN_BREAK_LENGTH, DEFAULT_MIN_BREAK_LENGTH);

        // the index "dictionary" field to verify compounds / constituents
        final String indexField = (String) config.get(CONF_DICTIONARY_FIELD);

        // whether query strings should be turned into lower case before trying to compound/decompound
        final boolean lowerCaseInput = ConfigUtils.getArg(config, CONF_LOWER_CASE_INPUT, Boolean.FALSE);

        // terms triggering a reversal of the surrounding compound, e.g. "tasche AUS samt" -> samttasche
        final List<String> reverseCompoundTriggerWords = (List<String>) config.get(CONF_REVERSE_COMPOUND_TRIGGER_WORDS);

        final Map<String, Object> decompoundConf = ConfigUtils.getArg(config, CONF_DECOMPOUND, Collections.emptyMap());

        final int maxDecompoundExpansions = ConfigUtils.getArg(decompoundConf, CONF_DECOMPOUND_MAX_EXPANSIONS,
                DEFAULT_MAX_DECOMPOUND_EXPANSIONS);

        final boolean verifyDecompoundCollation =  ConfigUtils.getArg(decompoundConf, CONF_DECOMPOUND_VERIFY_COLLATION,
                DEFAULT_VERIFY_DECOMPOUND_COLLATION);

        if (maxDecompoundExpansions < 0) {
            throw new IllegalArgumentException("decompound.maxExpansions >= 0 expected. Found: "
                    + maxDecompoundExpansions);
        }


        // define whether we should always try to add a reverse compound
        final boolean alwaysAddReverseCompounds = ConfigUtils.getArg(config, CONF_ALWAYS_ADD_REVERSE_COMPOUNDS,
                Boolean.FALSE);

        final Morphology morphology = ConfigUtils.getEnumArg(config, CONF_MORPHOLOGY, Morphology.class)
                .orElse(Morphology.DEFAULT);

        // terms that are "protected", i.e. false positives that should never be split and never be result of a combination
        final List<String> protectedWords = ConfigUtils.getArg(config, CONF_PROTECTED_WORDS, new ArrayList<>());

        // the indexReader has to be supplied on a per-request basis from a request thread-local
        final Supplier<IndexReader> indexReaderSupplier = () ->
                SolrRequestInfo.getRequestInfo().getReq().getSearcher().getIndexReader();

        delegate  = new querqy.lucene.contrib.rewrite.wordbreak.WordBreakCompoundRewriterFactory(rewriterId,
                indexReaderSupplier, morphology, indexField, lowerCaseInput, minSuggestionFreq, maxCombineLength,
                minBreakLength,reverseCompoundTriggerWords, alwaysAddReverseCompounds, maxDecompoundExpansions,
                verifyDecompoundCollation, protectedWords);
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
