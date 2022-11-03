package querqy.rewrite.contrib.replace;

import querqy.rewrite.logging.ActionLogging;
import querqy.rewrite.logging.InstructionLogging;
import querqy.rewrite.logging.MatchLogging;

import java.util.List;
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
     * @param actionLoggings   Debug information about replaced terms and their replacement
     */
    abstract public void apply(final List<CharSequence> seq, final int start, final int exclusiveOffset,
                               final CharSequence wildcardMatch, List<ActionLogging> actionLoggings);

    public void apply(final List<CharSequence> seq, final int start, final int exclusiveOffset) {
        this.apply(seq, start, exclusiveOffset, "", null);
    }

    public void apply(final List<CharSequence> seq, final int start, final int exclusiveOffset,
                      final CharSequence wildcardMatch) {
        this.apply(seq, start, exclusiveOffset, wildcardMatch, null);
    }

    public void apply(final List<CharSequence> seq, final int start, final int exclusiveOffset,
                      final List<ActionLogging> actionLoggings) {
        this.apply(seq, start, exclusiveOffset, "", actionLoggings);
    }

    /**
     * Removes the term from the seq which is defined by start end exclusiveOffset.
     *
     * @param seq              List to replace the tokens in
     * @param start            Startposition of the term in the list
     * @param exclusiveOffset  Endposition of the term in the list without offset.
     * @param replacementTerms Terms that should be used as replacement
     * @param actionLoggings   Debug information about replaced terms and their replacement
     */
    // TODO: this definitely needs to be refactored, but requires more comprehensive refactoring in the replace rewriter
    public void removeTermFromSequence(final List<CharSequence> seq, final int start,
                                       final int exclusiveOffset, List<? extends CharSequence> replacementTerms,
                                       final List<ActionLogging> actionLoggings,
                                       final MatchLogging.MatchType matchType) {
        final List<CharSequence> removedTerms = IntStream.range(0, exclusiveOffset)
                .mapToObj(i -> seq.remove(start)).collect(Collectors.toList());

        final String removedTermsInfo = String.join(" ", removedTerms);
        final String replacementTermsInfo = String.join(" ", replacementTerms);

        if (actionLoggings != null) {
            actionLoggings.add(
                    ActionLogging.builder()
                            .message(String.format("%s => %s", removedTermsInfo, replacementTermsInfo))
                            .match(
                                    MatchLogging.builder()
                                            .type(matchType)
                                            .term(removedTermsInfo)
                                            .build()
                            )
                            .instructions(List.of(
                                    InstructionLogging.builder()
                                            .type("replace")
                                            .value(replacementTermsInfo)
                                            .build()
                            ))
                            .build()
            );
        }
    }
}
