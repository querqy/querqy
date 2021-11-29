package querqy.lucene.contrib.rewrite.wordbreak;

import querqy.CompoundCharSequence;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>A SuffixGroup represents all word forms that can be generated once a suffix has been stripped off.</p>
 *
 * <p>For example, German has a suffix -en that is added to the modifier word in compounding:</p>
 * <pre>
 *     strauß + ei -&gt; straußenei (strauß +en ei)
 * </pre>
 * There are further structures that use this suffix:
 * <pre>
 *     stadion + verbot -&gt; stadienverbot (stadion -on +en verbot)
 *     aphorismus + schatz -&gt; aphorismenschat (aphorismus -us +en schatz)
 *     ...
 * </pre>
 *
 * <p>All word forms using the +en would be combined into a single SuffixGroup and the different structures (Null, -on,
 * -us etc.) will be kept in the {@link #generatorAndWeights} list.</p>
 *
 * <p>Suffixes can overlap: -ien/-nen are both contained in -en, -en is contained in -n, and finally, all suffixes are
 * contained in the null (or, zero-length) suffix. This relationship is expressed in the {@link #next} property of
 * a SuffixGroup. -n is a 'next' of the zero-length SuffixGroup, -en is a 'next' of -n, and -ien and -nen are both
 * 'nexts' of -en. This organisation of suffixed helps speed up the lookup, as we can stop the lookup as soon as the
 * first element in the 'next' list could be matched in the index.</p>
 *
 * @author renekrie
 */
public class SuffixGroup {


    private final CharSequence suffix;
    private final int suffixLength;
    private final List<WordGeneratorAndWeight> generatorAndWeights;
    private final List<SuffixGroup> next;

    public SuffixGroup(final CharSequence suffix, final List<WordGeneratorAndWeight> generatorAndWeights,
                       final SuffixGroup... next) {
        this.suffix = suffix;
        this.generatorAndWeights = generatorAndWeights;
        this.next = Arrays.asList(next);
        this.suffixLength = suffix == null ? 0 : suffix.length();
    }


    public List<Suggestion> generateSuggestions(final CharSequence left) {
        return generateSuggestions(left, 0);
    }

    private List<Suggestion> generateSuggestions(final CharSequence left, final int matchingFromEndOfLeft) {
        final int leftLength = left.length();
        if (left.length() <= suffixLength) {
            return Collections.emptyList();
        }

        if (suffixLength > 0 && left.length() > suffixLength) {
            for (int i = 1 + matchingFromEndOfLeft; i <= suffixLength; i++) {
                if (left.charAt(leftLength - i) != suffix.charAt(suffixLength - i)) {
                    return Collections.emptyList();
                }
            }
        }

        final CharSequence reduced = suffixLength == 0 ? left : left.subSequence(0, leftLength - suffixLength);

        final List<Suggestion> res =
                generatorAndWeights.stream()
                        .map(generatorAndWeights -> generatorAndWeights.generateSuggestion(reduced))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());

        final List<Suggestion> suggestions = next.stream()
                .map(sg -> sg.generateSuggestions(left, suffixLength))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        res.addAll(suggestions);

        return res;
    }

    public List<Suggestion> generateCompoundSuggestions(final CharSequence left, final CharSequence right) {
        final CharSequence[][] combineTerms = {
                {left, right},
                {right, left}
        };

        return Arrays.stream(combineTerms)
                .map(terms -> generateSuggestions(terms[0], 0)
                        .stream().map(
                                suggestion -> new Suggestion(
                                        new CharSequence[]{new CompoundCharSequence(null, suggestion.sequence[0], terms[1])},
                                        suggestion.score)
                        )).flatMap(Stream::distinct).collect(Collectors.toList());
    }

}
