package querqy.rewrite.contrib.replace;

import querqy.rewrite.logging.ActionLog;
import querqy.rewrite.logging.MatchLog;

import java.util.List;

public class TermsReplaceInstruction extends ReplaceInstruction {

    private final List<? extends CharSequence> replacementTerms;

    public TermsReplaceInstruction(final List<? extends CharSequence> replacementTerms) {
        this.replacementTerms = replacementTerms;
    }

    @Override
    public void apply(final List<CharSequence> seq, final int start, final int exclusiveOffset,
                      final CharSequence wildcardMatch, final List<ActionLog> actionLoggings) {
        removeTermFromSequence(seq, start, exclusiveOffset, replacementTerms, actionLoggings, MatchLog.MatchType.EXACT);
        seq.addAll(start, replacementTerms);
    }
}
