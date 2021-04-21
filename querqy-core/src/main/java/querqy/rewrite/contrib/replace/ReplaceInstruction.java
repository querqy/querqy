package querqy.rewrite.contrib.replace;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class ReplaceInstruction {

    /**
     * Applies the defined replace rules
     *
     * @param seq              List to replace the tokens in
     * @param start            Startposition of the term in the list
     * @param exclusiveOffset  Endposition of the term in the list without offset.
     * @param wildcardMatch    Wildcard match that should be used to generate a replacement
     * @param appliedRules     Debug information about replaced terms and their replacement
     */
    abstract public void apply(final List<CharSequence> seq, final int start, final int exclusiveOffset,
                               final CharSequence wildcardMatch, final Map<String, Set<CharSequence>> appliedRules);

    public void apply(final List<CharSequence> seq, final int start, final int exclusiveOffset) {
        this.apply(seq, start, exclusiveOffset, "", null);
    }

    public void apply(final List<CharSequence> seq, final int start, final int exclusiveOffset, final CharSequence wildcardMatch) {
        this.apply(seq, start, exclusiveOffset, wildcardMatch, null);
    }

    public void apply(final List<CharSequence> seq, final int start, final int exclusiveOffset, final Map<String, Set<CharSequence>> appliedRules) {
        this.apply(seq, start, exclusiveOffset, "", appliedRules);
    }

    /**
     * Removes the term from the seq which is defined by start end exclusiveOffset.
     *
     * @param seq              List to replace the tokens in
     * @param start            Startposition of the term in the list
     * @param exclusiveOffset  Endposition of the term in the list without offset.
     * @param replacementTerms Terms that should be used as replacement
     * @param appliedRules     Debug information about replaced terms and their replacement
     */
    public void removeTermFromSequence(final List<CharSequence> seq, final int start,
                                       final int exclusiveOffset, List<? extends CharSequence> replacementTerms,
                                       final Map<String, Set<CharSequence>> appliedRules) {
        Set<CharSequence> removedTerms = IntStream.range(0, exclusiveOffset)
                .mapToObj(i -> seq.remove(start)).collect(Collectors.toSet());

        if (appliedRules != null) {
            appliedRules.computeIfAbsent(replacementTerms.toString(), e -> new HashSet<>()).addAll(removedTerms);
        }
    }
}
