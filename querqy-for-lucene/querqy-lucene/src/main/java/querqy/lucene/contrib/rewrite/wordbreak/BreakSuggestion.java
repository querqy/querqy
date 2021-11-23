package querqy.lucene.contrib.rewrite.wordbreak;

import querqy.CharSequenceUtil;

public class BreakSuggestion implements Comparable<BreakSuggestion> {

    final CharSequence[] sequence;
    final float score;

    BreakSuggestion(final CharSequence[] sequence, final float score) {
        this.sequence = sequence;
        this.score = score;
    }


    @Override
    public int compareTo(final BreakSuggestion other) {

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

}
