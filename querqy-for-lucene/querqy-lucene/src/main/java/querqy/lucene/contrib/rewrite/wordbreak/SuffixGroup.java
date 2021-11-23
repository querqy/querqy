package querqy.lucene.contrib.rewrite.wordbreak;

import org.apache.lucene.index.Term;
import querqy.lucene.contrib.rewrite.wordbreak.Collector.CollectionState;

import java.util.*;
import java.util.stream.Collectors;

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
    private final SuffixGroup[] next;
    private final List<SuffixGroup> nextL;

    public SuffixGroup(final CharSequence suffix, final List<WordGeneratorAndWeight> generatorAndWeights,
                       final SuffixGroup... next) {
        this.suffix = suffix;
        this.generatorAndWeights = generatorAndWeights;
        this.next = next;
        this.nextL = Arrays.asList(next);
        this.suffixLength = suffix == null ? 0 : suffix.length();
    }


    public List<BreakSuggestion> generateBreakSuggestions(final CharSequence left) {
        return generateBreakSuggestions(left, 0);
    }

    private List<BreakSuggestion> generateBreakSuggestions(final CharSequence left, final int matchingFromEndOfLeft) {
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

        final List<BreakSuggestion> res =
                generatorAndWeights.stream()
                        .map(generatorAndWeights -> generatorAndWeights.breakSuggestion(reduced))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());

        final List<BreakSuggestion> breakSuggestions = nextL.stream()
                .map(sg -> sg.generateBreakSuggestions(left, suffixLength))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        res.addAll(breakSuggestions);

        return res;
    }

    public void collect(final querqy.model.Term left, final querqy.model.Term right, final Queue<BreakSuggestion> collector) {
        final int minLength = 1;

        for (final WordGeneratorAndWeight generatorAndWeight : generatorAndWeights) {
            final Optional<CharSequence> modifierOpt = generatorAndWeight.generator.generateModifier(left);
            if (!modifierOpt.isPresent()) continue;
            final CharSequence modifier = modifierOpt.get();
            if (modifier.length() < minLength) continue;

            final CharSequence l = modifierOpt.get();
            final CharSequence r = right.getValue();
            collector.offer(new BreakSuggestion(new CharSequence[]{l, r}, generatorAndWeight.weight));
        }
    }
}
