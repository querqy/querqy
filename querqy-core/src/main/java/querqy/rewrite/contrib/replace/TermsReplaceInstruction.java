package querqy.rewrite.contrib.replace;

import java.util.List;
import java.util.stream.IntStream;

public class TermsReplaceInstruction implements ReplaceInstruction {

    private final List<CharSequence> replacementTerms;

    public TermsReplaceInstruction(final List<CharSequence> replacementTerms) {
        this.replacementTerms = replacementTerms;
    }

    @Override
    public void apply(List<CharSequence> seq, int start, int exclusiveOffset, CharSequence wildcardMatch) {
        IntStream.range(0, exclusiveOffset).forEach(i -> seq.remove(start));
        seq.addAll(start, replacementTerms);
    }
}
