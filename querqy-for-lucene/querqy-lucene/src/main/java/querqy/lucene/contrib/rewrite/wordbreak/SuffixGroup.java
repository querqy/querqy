package querqy.lucene.contrib.rewrite.wordbreak;

import org.apache.lucene.index.Term;

import java.util.List;

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
     * @param left The left split
     * @param minLength
     * @return true iff the suffix of this SuffixGroup matched
     */
    public boolean collect(final CharSequence left, final int matchingFromEndOfLeft, final CharSequence right,
                           final Term rightTerm,  final int rightDf, final int minLength,
                           final Collector collector) {

        final int leftLength = left.length();

        if (left.length() <= suffixLength) {
            return false;
        }

        if (suffixLength > 0 && left.length() > suffixLength) {

            for (int i = 1 + matchingFromEndOfLeft; i <= suffixLength; i++) {

                if (left.charAt(leftLength - i) != suffix.charAt(suffixLength - i)) {
                    return false;
                }
            }

        }

        boolean matched = false;

        final CharSequence reduced = suffixLength == 0 ? left : left.subSequence(0, leftLength - suffixLength);
        for (final WordGeneratorAndWeight generatorAndWeight: generatorAndWeights) {
            matched |= generatorAndWeight.generator.generateModifier(reduced)
                    .filter(modifier -> modifier.length() >= minLength)
                    .map(modifier ->
                            collector.collect(modifier, right, rightTerm, rightDf, generatorAndWeight.weight)
                    ).orElse(false);

        }

        if (next != null) {
            for (final SuffixGroup group : next) {
                if (group.collect(left, suffixLength, right, rightTerm, rightDf,
                        minLength, collector)) {
                    matched = true;
                    // As suffixes are mutually exclusive, we can break after the first match
                    break;
                }
            }
        }

        return matched;
    }

}
