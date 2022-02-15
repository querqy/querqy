package querqy.lucene.contrib.rewrite.wordbreak;

import java.util.List;

public interface Morphology {
    Compound[] suggestCompounds(CharSequence left, CharSequence right);

    List<WordBreak> suggestWordBreaks(CharSequence word, int minBreakLength);
}
