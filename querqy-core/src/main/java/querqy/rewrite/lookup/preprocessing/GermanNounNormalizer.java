package querqy.rewrite.lookup.preprocessing;

import querqy.CompoundCharSequence;
import querqy.trie.State;
import querqy.trie.TrieMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GermanNounNormalizer implements LookupPreprocessor {

    public static final int MIN_INPUT_LENGTH = 4;
    static final int MIN_INPUT_LENGTH_TO_STRIP_OFF_S = 5;


    private static final String IDENTITY = "";

    // TODO it would be nicer to create these maps in a factory and then pass them to the constructor. Let's
    // review this once we know how we plug Normalizers together.
    private static final TrieMap<String> MAP = loadTrieMap(false);

    /**
     * Keys are reversed in this map to facilitate the lookup by suffixes.
     */
    private static final TrieMap<String> REVERSE_MAP = loadTrieMap(true);

    static TrieMap<String> loadTrieMap(final boolean reverseKeys) {

        try(final BufferedReader reader = new BufferedReader(new InputStreamReader(
                GermanNounNormalizer.class.getClassLoader().getResourceAsStream("de-nouns.txt"), UTF_8))) {

            final TrieMap<String> map = new TrieMap<>();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                final int comment_pos = line.indexOf('#');
                if (comment_pos > -1) {
                    line = line.substring(0, comment_pos).trim();
                }
                if (line.length() > 0) {
                    final String[] parts = line.split(",");
                    // sg = singular (= canonical form), pl = plural
                    // some lines might only have one form
                    final String sg = line.charAt(0) == ',' ? "" : parts[0];
                    final String pl = line.charAt(line.length() - 1) == ',' ? "" : parts[1];

                    updateMap(sg, pl, map, reverseKeys);

                }
            }

            return map;

        } catch (final IOException e) {
            throw new RuntimeException("Could not load noun list", e);
        }
    }

    /**
     *
     * @param sg Singular = canonical form if not empty
     * @param pl Plural
     * @param map The map to update
     * @param reverseKeys Reverse mapping keys iff true
     */
    static void updateMap(final String sg, final String pl, final TrieMap<String> map,
                          final boolean reverseKeys) {
        final String existingSgMapping = getFromMap(sg, map, reverseKeys);

        // using != to compare with the IDENTITY constant is what we want
        if ((sg.length() > 0) && IDENTITY != existingSgMapping) {
            map.put(reverseKeys ? new ReverseCharSequence(sg) : sg, IDENTITY);
        }

        final String existingPlMapping = getFromMap(pl, map, reverseKeys);
        if ((pl.length() > 0) && IDENTITY != existingPlMapping) {
            if (sg.length() > 0) {
                map.put(reverseKeys ? new ReverseCharSequence(pl) : pl, sg);
            } else {
                map.put(reverseKeys ? new ReverseCharSequence(pl) : pl, IDENTITY);
            }
        }
    }

    static String getFromMap(final CharSequence key, final TrieMap<String> map, final boolean reverseKey) {
        final State<String> state = map.get(reverseKey ? new ReverseCharSequence(key) : key)
                .getStateForCompleteSequence();
        return  state.isFinal() ? state.value : null;
    }

    /**
     * We try to find the canonical word form for the input word string. We first do an exact look-up in a word list.
     * If we can't find any match, we try to find the longest suffix that has a match in the word list, assuming that
     * the input is a compound word. If we find a match, we replace the suffix with the canonical word form.
     * If we still couldn't find anything, we strip off the letter 's' at the end of the input, if there is any,
     * assuming that we are dealing with an English loan word. If there is no '-s' we give up and just return the input
     * as the canonical form.
     *
     * @param input The input word
     * @return The normalized form.
     */
    @Override
    public CharSequence process(final CharSequence input) {

        if (!isToBeNormalized(input)) {
            return input;
        }

        return lookupInput(input)
                .or(() -> lookupCompoundInput(input))
                .or(() -> trySZLigature(input))
                .or(() -> applyRules(input))
                .orElse(input);

    }

    protected Optional<CharSequence> applyRules(final CharSequence input) {
        final int inputLength = input.length();
        return (inputLength  >= MIN_INPUT_LENGTH_TO_STRIP_OFF_S && input.charAt(inputLength - 1) == 's')
                ? Optional.of(input.subSequence(0, inputLength - 1)) : Optional.empty();
    }

    protected Optional<CharSequence> trySZLigature(final CharSequence input) {
        for (final CharSequence variant : getSZLigatureVariants(input)) {
            final Optional<CharSequence> optResult = lookupInput(variant).or(() -> lookupCompoundInput(variant));
            if (optResult.isPresent()) {
                return optResult;
            }
        }

        return Optional.empty();
    }

    protected List<CharSequence> getSZLigatureVariants(final CharSequence input) {

        for (int i = 0, last = input.length() - 1; i < last; i++) {
            if (input.charAt(i) == 's' && input.charAt(i + 1) == 's') {
                final List<CharSequence> results = new ArrayList<>();
                if (i == 0) { // starting with "ss" (just a theoretical case)
                    if (last == 1) {
                        // input=="ss"
                        return Collections.singletonList("ß");
                    } else {
                        final CharSequence suffix = input.subSequence(i + 2, input.length());
                        results.add(new CompoundCharSequence(null, "ß", suffix));

                        // input == "ss" plus a suffix that might contain another "ss"

                        if (i + 4 <= last) {
                            for (final CharSequence replacement : getSZLigatureVariants(suffix)) {
                                results.add(new CompoundCharSequence(null, "ß", replacement));
                                results.add(new CompoundCharSequence(null, "ss", replacement));
                            }
                        } // else the input would be too short for another "ss" (ignoring input == "ssss")

                        return results;
                    }
                } else {
                    final CharSequence prefix = input.subSequence(0, i);
                    if (last == i + 1) {
                        return Collections.singletonList(new CompoundCharSequence(null, prefix, "ß"));
                    } else {
                        final CharSequence suffix = input.subSequence(i + 2, input.length());
                        results.add(new CompoundCharSequence(null, prefix, "ß", suffix));
                        if (i + 4 <= last) {
                            for (final CharSequence replacement : getSZLigatureVariants(suffix)) {
                                results.add(new CompoundCharSequence(null, prefix, "ß", replacement));
                                results.add(new CompoundCharSequence(null, prefix, "ss", replacement));
                            }
                        }
                        return results;
                    }
                }
            }
        }

        return Collections.emptyList();
    }

    protected Optional<CharSequence> lookupCompoundInput(final CharSequence input) {
        final int minBaseWordLength = 4;
        final int minPrefixLength = 3;
        // find the longest input matching from the end
        final ReverseCharSequence seq = new ReverseCharSequence(input);
        // start at the minBaseWordLength:
        int baseWordPos = -1;
        State<String> baseWordState = null;

        final CharSequence suffix = seq.subSequence(0, minBaseWordLength);
        State<String> state = REVERSE_MAP.get(suffix).getStateForCompleteSequence();
        if (state.isFinal()) {
            baseWordState = state;
            baseWordPos = minBaseWordLength;
        }

        int pos = minBaseWordLength;
        while (state.isKnown && pos < input.length() - minPrefixLength) {
            state = state.node.getNext(seq.charAt(pos)).getStateForCompleteSequence();
            if (state.isFinal()) {
                baseWordState = state;
                baseWordPos = pos;
            }
            pos++;
        }

        if (baseWordState != null) {
            final CharSequence value = baseWordState.value == IDENTITY
                    ? input.subSequence(input.length() - baseWordPos - 1, input.length())
                    : baseWordState.value;
            return Optional.of(new CompoundCharSequence(null, input.subSequence(0, input.length() - baseWordPos - 1), value));
        }

        return Optional.empty();
    }


    protected Optional<CharSequence> lookupInput(final CharSequence input) {
        final String value = getFromMap(input, MAP, false);
        if (value != null) {
            if (IDENTITY == value) {
                return Optional.of(input);
            } else {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    /**
     * Don't normalize any input that is shorter than {@link GermanNounNormalizer#MIN_INPUT_LENGTH} or that contains a
     * digit.
     *
     * @param input The input word string
     * @return True if the word should be normalized, otherwise false.
     */
    protected boolean isToBeNormalized(final CharSequence input) {
        if (input.length() < MIN_INPUT_LENGTH) {
            return false;
        }

        for (int i = 0, len = input.length(); i < len; i++) {
            if (Character.isDigit(input.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    public static GermanNounNormalizer create() {
        return new GermanNounNormalizer();
    }



}
