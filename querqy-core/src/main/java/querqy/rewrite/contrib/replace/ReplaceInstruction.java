package querqy.rewrite.contrib.replace;

import java.util.List;

public interface ReplaceInstruction {

    void apply(final List<CharSequence> seq, final int start, final int exclusiveOffset,
               final CharSequence wildcardMatch);

    default void apply(final List<CharSequence> seq, final int start, final int exclusiveOffset) {
        this.apply(seq, start, exclusiveOffset, "");
    }
}
