package querqy.lucene.contrib.rewrite.wordbreak;

import querqy.CharSequenceUtil;

import java.util.Arrays;
import java.util.Objects;

public class Suggestion implements Comparable<Suggestion> {

    final CharSequence[] sequence;
    final float score;

    Suggestion(final CharSequence[] sequence, final float score) {
        this.sequence = sequence;
        this.score = score;
    }


    @Override
    public int compareTo(final Suggestion other) {

        if (other == this) {
            return 0;
        }
        int c = Float.compare(score, other.score); // greater is better
        if (c == 0) {
            c = Integer.compare(sequence.length, other.sequence.length); // shorter is better
            if (c == 0) {
                for (int i = 0; i < sequence.length && c == 0; i++) {
                    c = CharSequenceUtil.compare(sequence[i], other.sequence[i]);
                }
            }
        }

        return c;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Suggestion that = (Suggestion) o;
        return Float.compare(that.score, score) == 0 && Arrays.equals(sequence, that.sequence);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(score);
        result = 31 * result + Arrays.hashCode(sequence);
        return result;
    }
}
