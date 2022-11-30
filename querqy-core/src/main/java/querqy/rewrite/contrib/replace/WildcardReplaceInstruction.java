package querqy.rewrite.contrib.replace;

import querqy.CompoundCharSequence;
import querqy.rewrite.logging.ActionLog;
import querqy.rewrite.logging.MatchLog;

import java.util.LinkedList;
import java.util.List;

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
                      final List<ActionLog> actionLogs) {
        removeTermFromSequence(seq, start, exclusiveOffset, seq, actionLogs, MatchLog.MatchType.AFFIX);
        termCreators.forEach(termCreator -> seq.add(start, termCreator.createTerm(wildcardMatch)));
    }
}
