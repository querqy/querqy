package querqy.lucene.contrib.rewrite.wordbreak;

import org.apache.lucene.index.Term;
import querqy.lucene.contrib.rewrite.wordbreak.Collector.CollectionState;

import java.util.List;
import java.util.Optional;
import java.util.Queue;

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

    public SuffixGroup(final CharSequence suffix, final List<WordGeneratorAndWeight> generatorAndWeights,
                       final SuffixGroup ... next) {
        this.suffix = suffix;
        this.generatorAndWeights = generatorAndWeights;
        this.next = next;
        this.suffixLength = suffix == null ? 0 : suffix.length();
    }

    /**
     *
     * @param left The left split (the modifier)
     * @param matchingFromEndOfLeft number of characters that are know to match from the end of the left split
     * @param right The head character sequence
     * @param rightTerm The head character sequence as a term in the dictionary field
     * @param rightDf The document frequency of the rightTerm
     * @param minLength The minimum head/modifier length
     * @param collector The collector that will be presented the candidates
     * @return true iff the suffix of this SuffixGroup matched
     */
    public CollectionState collect(final CharSequence left, final int matchingFromEndOfLeft,
                                             final CharSequence right, final Term rightTerm, final int rightDf,
                                             final int minLength, final Collector collector) {

        final int leftLength = left.length();

        if (left.length() <= suffixLength) {
            return collector.maxEvaluationsReached()
                    ? CollectionState.NOT_MATCHED_MAX_EVALUATIONS_REACHED
                    : CollectionState.NOT_MATCHED_MAX_EVALUATIONS_NOT_REACHED;

        }

        if (suffixLength > 0 && left.length() > suffixLength) {

            for (int i = 1 + matchingFromEndOfLeft; i <= suffixLength; i++) {

                if (left.charAt(leftLength - i) != suffix.charAt(suffixLength - i)) {
                    return collector.maxEvaluationsReached()
                            ? CollectionState.NOT_MATCHED_MAX_EVALUATIONS_REACHED
                            : CollectionState.NOT_MATCHED_MAX_EVALUATIONS_NOT_REACHED;
                }
            }

        }

        boolean matched = false;

        final CharSequence reduced = suffixLength == 0 ? left : left.subSequence(0, leftLength - suffixLength);
        for (final WordGeneratorAndWeight generatorAndWeight: generatorAndWeights) {
            final Optional<CharSequence> modifierOpt = generatorAndWeight.generator.generateModifier(reduced);
            if (modifierOpt.isPresent()) {
                final CharSequence modifier = modifierOpt.get();
                if (modifier.length() >= minLength) {
                    final CollectionState collectionState = collector.collect(modifier, right, rightTerm,
                            rightDf, generatorAndWeight.weight);
                    matched |= collectionState.getMatched().orElse(false);
                    if (collectionState.isMaxEvaluationsReached()) {
                        return matched
                                ? CollectionState.MATCHED_MAX_EVALUATIONS_REACHED
                                : CollectionState.NOT_MATCHED_MAX_EVALUATIONS_REACHED;
                    }
                }
            }
        }

        if (next != null) {
            for (final SuffixGroup group : next) {
                final CollectionState collectionState = group.collect(left, suffixLength, right, rightTerm, rightDf, minLength, collector);

                final boolean wasMatched = collectionState.getMatched().orElse(false);
                if (collectionState.isMaxEvaluationsReached()) {
                    return matched || wasMatched
                            ? CollectionState.MATCHED_MAX_EVALUATIONS_REACHED
                            : CollectionState.NOT_MATCHED_MAX_EVALUATIONS_REACHED;
                }
                if (wasMatched) {
                    return CollectionState.MATCHED_MAX_EVALUATIONS_NOT_REACHED;
                }
            }
        }

        return matched
                ? CollectionState.MATCHED_MAX_EVALUATIONS_NOT_REACHED
                : CollectionState.NOT_MATCHED_MAX_EVALUATIONS_NOT_REACHED;
    }

    public void collect(querqy.model.Term left, querqy.model.Term right, Queue<MorphologicalWordBreaker.BreakSuggestion> collector) {
        final int minLength = 1;

        for (final WordGeneratorAndWeight generatorAndWeight: generatorAndWeights) {
            final Optional<CharSequence> modifierOpt = generatorAndWeight.generator.generateModifier(left);
            if (!modifierOpt.isPresent()) continue;
            final CharSequence modifier = modifierOpt.get();
            if (modifier.length() < minLength) continue;

            CharSequence l = modifierOpt.get();
            CharSequence r = right.getValue();
            collector.offer(new MorphologicalWordBreaker.BreakSuggestion(new CharSequence[]{l, r}, generatorAndWeight.weight));
        }
    }
}
