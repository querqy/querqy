package querqy.rewrite.contrib.replace;

import querqy.CompoundCharSequence;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WildcardReplaceInstruction extends ReplaceInstruction {

    @FunctionalInterface
    public interface TermCreator {
        CharSequence createTerm(final CharSequence wildcardMatch);
    }

    private final List<TermCreator> termCreators = new LinkedList<>();

    public WildcardReplaceInstruction(final List<? extends CharSequence> replacementTerms) {

        replacementTerms.stream()
                .map(CharSequence::toString)
                .forEach(replacementTerm -> {
                    final int indexWildcardReplacement = replacementTerm.indexOf("$1");
                    if (indexWildcardReplacement < 0) {
                        termCreators.add(0, wildcardMatch -> replacementTerm);

                    } else {

                        final String leftPart = replacementTerm.substring(0, indexWildcardReplacement);
                        final String rightPart = replacementTerm.substring(indexWildcardReplacement + 2);

                        termCreators.add(0, wildcardMatch ->
                                new CompoundCharSequence(null, leftPart, wildcardMatch, rightPart));
                    }
                });
    }

    @Override
    public void apply(final List<CharSequence> seq,
                      final int start,
                      final int exclusiveOffset,
                      final CharSequence wildcardMatch,
                      final Map<String, Set<CharSequence>> appliedRules) {
        removeTermFromSequence(seq, start, exclusiveOffset, seq, appliedRules);
        termCreators.forEach(termCreator -> seq.add(start, termCreator.createTerm(wildcardMatch)));
    }
}
