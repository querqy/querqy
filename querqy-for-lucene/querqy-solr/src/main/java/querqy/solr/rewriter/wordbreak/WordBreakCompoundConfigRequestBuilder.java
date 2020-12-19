package querqy.solr.rewriter.wordbreak;

import static querqy.solr.rewriter.wordbreak.WordBreakCompoundRewriterFactory.CONF_ALWAYS_ADD_REVERSE_COMPOUNDS;
import static querqy.solr.rewriter.wordbreak.WordBreakCompoundRewriterFactory.CONF_DECOMPOUND;
import static querqy.solr.rewriter.wordbreak.WordBreakCompoundRewriterFactory.CONF_DECOMPOUND_MAX_EXPANSIONS;
import static querqy.solr.rewriter.wordbreak.WordBreakCompoundRewriterFactory.CONF_DECOMPOUND_VERIFY_COLLATION;
import static querqy.solr.rewriter.wordbreak.WordBreakCompoundRewriterFactory.CONF_DICTIONARY_FIELD;
import static querqy.solr.rewriter.wordbreak.WordBreakCompoundRewriterFactory.CONF_LOWER_CASE_INPUT;
import static querqy.solr.rewriter.wordbreak.WordBreakCompoundRewriterFactory.CONF_MAX_COMBINE_WORD_LENGTH;
import static querqy.solr.rewriter.wordbreak.WordBreakCompoundRewriterFactory.CONF_MIN_BREAK_LENGTH;
import static querqy.solr.rewriter.wordbreak.WordBreakCompoundRewriterFactory.CONF_MIN_SUGGESTION_FREQ;
import static querqy.solr.rewriter.wordbreak.WordBreakCompoundRewriterFactory.CONF_MORPHOLOGY;
import static querqy.solr.rewriter.wordbreak.WordBreakCompoundRewriterFactory.CONF_REVERSE_COMPOUND_TRIGGER_WORDS;

import querqy.lucene.contrib.rewrite.wordbreak.Morphology;
import querqy.solr.RewriterConfigRequestBuilder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WordBreakCompoundConfigRequestBuilder extends RewriterConfigRequestBuilder {

    private String dictionaryField;
    private Integer minSuggestionFrequency;
    private Integer maxCombineWordLength;
    private Integer minBreakLength;
    private Boolean lowerCaseInput;
    private String[] reverseCompoundTriggerWords;
    private Boolean alwaysAddReverseCompounds;
    private Integer decompoundMaxExpansions;
    private Boolean decompoundVerifyCollation;
    private Morphology morphology;

    public WordBreakCompoundConfigRequestBuilder() {
        super(WordBreakCompoundRewriterFactory.class);
    }

    @Override
    public Map<String, Object> buildConfig() {

        final Map<String, Object> config = new HashMap<>();

        if (dictionaryField == null) {
            throw new RuntimeException(CONF_DICTIONARY_FIELD + " must not be null");
        }
        config.put(CONF_DICTIONARY_FIELD, dictionaryField);

        if (minSuggestionFrequency != null) {
            config.put(CONF_MIN_SUGGESTION_FREQ, minSuggestionFrequency);
        }

        if (maxCombineWordLength != null) {
            config.put(CONF_MAX_COMBINE_WORD_LENGTH, maxCombineWordLength);
        }

        if (minBreakLength != null) {
            config.put(CONF_MIN_BREAK_LENGTH, minBreakLength);
        }

        if (lowerCaseInput != null) {
            config.put(CONF_LOWER_CASE_INPUT, lowerCaseInput);
        }

        if (reverseCompoundTriggerWords != null) {
            config.put(CONF_REVERSE_COMPOUND_TRIGGER_WORDS, Arrays.asList(reverseCompoundTriggerWords));
        }

        if (alwaysAddReverseCompounds != null) {
            config.put(CONF_ALWAYS_ADD_REVERSE_COMPOUNDS, alwaysAddReverseCompounds);
        }

        if (morphology != null) {
            config.put(CONF_MORPHOLOGY, morphology.name());
        }

        Map<String, Object> decompoundConf = null;

        if (decompoundMaxExpansions != null) {
            decompoundConf = new HashMap<>();
            decompoundConf.put(CONF_DECOMPOUND_MAX_EXPANSIONS, decompoundMaxExpansions);
        }
        if (decompoundVerifyCollation != null) {
            if (decompoundConf == null) {
                decompoundConf = new HashMap<>();
            }
            decompoundConf.put(CONF_DECOMPOUND_VERIFY_COLLATION,
                    decompoundVerifyCollation);
        }

        if (decompoundConf != null) {
            config.put(CONF_DECOMPOUND, decompoundConf);
        }

        return config;

    }

    public WordBreakCompoundConfigRequestBuilder dictionaryField(final String dictionaryField) {
        if (dictionaryField == null) {
            throw new IllegalArgumentException("dictionaryField must not be null");
        }
        final String fieldName = dictionaryField.trim();
        if (fieldName.isEmpty()) {
            throw new IllegalArgumentException("dictionaryField must not be empty");
        }
        this.dictionaryField = fieldName;
        return this;
    }

    public WordBreakCompoundConfigRequestBuilder minSuggestionFrequency(final Integer minSuggestionFrequency) {
        this.minSuggestionFrequency = minSuggestionFrequency;
        return this;
    }

    public WordBreakCompoundConfigRequestBuilder maxCombineWordLength(final Integer maxCombineWordLength) {
        this.maxCombineWordLength = maxCombineWordLength;
        return this;
    }

    public WordBreakCompoundConfigRequestBuilder minBreakLength(final Integer minBreakLength) {
        if (minBreakLength != null && minBreakLength < 1) {
            throw new IllegalArgumentException("minBreakLength must be >=1 or null");
        }
        this.minBreakLength = minBreakLength;
        return this;
    }

    public WordBreakCompoundConfigRequestBuilder lowerCaseInput(final Boolean lowerCaseInput) {
        this.lowerCaseInput = lowerCaseInput;
        return this;
    }

    public WordBreakCompoundConfigRequestBuilder reverseCompoundTriggerWords(
            final String... reverseCompoundTriggerWords) {
        this.reverseCompoundTriggerWords = reverseCompoundTriggerWords;
        return this;
    }

    public WordBreakCompoundConfigRequestBuilder reverseCompoundTriggerWords(
            final List<String> reverseCompoundTriggerWords) {
        this.reverseCompoundTriggerWords = reverseCompoundTriggerWords.toArray(new String[0]);
        return this;
    }

    public WordBreakCompoundConfigRequestBuilder alwaysAddReverseCompounds(final Boolean alwaysAdd) {
        this.alwaysAddReverseCompounds = alwaysAdd;
        return this;
    }

    public WordBreakCompoundConfigRequestBuilder maxDecompoundExpansions(final Integer maxDecompoundExpansions) {
        if (maxDecompoundExpansions != null && maxDecompoundExpansions < 0) {
            throw new IllegalArgumentException("maxDecompoundExpansions must be >=0 or null");
        }
        this.decompoundMaxExpansions = maxDecompoundExpansions;
        return this;
    }

    public WordBreakCompoundConfigRequestBuilder verifyDecompoundCollation(final Boolean verify) {
        this.decompoundVerifyCollation = verify;
        return this;
    }

    public WordBreakCompoundConfigRequestBuilder morphology(final Morphology morphology) {
        this.morphology = morphology;
        return this;
    }

}
