package querqy.lucene.contrib.rewrite.wordbreak;

import querqy.CharSequenceUtil;

public class CompoundSuggestion implements Comparable<CompoundSuggestion> {

    final CharSequence[] suggestions;
    final float score;

    public CompoundSuggestion(final CharSequence[] suggestions, final float score) {
        this.suggestions = suggestions;
        this.score = score;
    }


    @Override
    public int compareTo(final CompoundSuggestion other) {

        if (other == this) {
            return 0;
        }
        int c = Float.compare(score, other.score); // greater is better
        if (c == 0) {
            c = Integer.compare(suggestions.length, other.suggestions.length); // shorter is better
            if (c == 0) {
                for (int i = 0; i < suggestions.length && c == 0; i++) {
                    c = CharSequenceUtil.compare(suggestions[i], other.suggestions[i]);
                }
            }
        }

        return c;
    }

}
