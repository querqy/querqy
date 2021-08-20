package querqy.rewrite.contrib.replace;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class TermsReplaceInstruction extends ReplaceInstruction {

    private final List<? extends CharSequence> replacementTerms;

    public TermsReplaceInstruction(final List<? extends CharSequence> replacementTerms) {
        this.replacementTerms = replacementTerms;
    }

    @Override
    public void apply(final List<CharSequence> seq, final int start, final int exclusiveOffset,
                      final CharSequence wildcardMatch, final Map<String, Set<CharSequence>> appliedRules) {
        removeTermFromSequence(seq, start, exclusiveOffset, replacementTerms, appliedRules);
        seq.addAll(start, replacementTerms);
    }
}
