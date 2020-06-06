package querqy.rewrite.contrib.replace;

import java.util.List;
import java.util.stream.IntStream;

public class TermsReplaceInstruction implements ReplaceInstruction {

    private final List<? extends CharSequence> replacementTerms;

    public TermsReplaceInstruction(final List<? extends CharSequence> replacementTerms) {
        this.replacementTerms = replacementTerms;
    }

    @Override
    public void apply(final List<CharSequence> seq, final int start, final int exclusiveOffset,
                      final CharSequence wildcardMatch) {
        IntStream.range(0, exclusiveOffset).forEach(i -> seq.remove(start));
        seq.addAll(start, replacementTerms);
    }
}
