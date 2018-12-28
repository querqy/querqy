package querqy.lucene;


/**
 * Copied from org.apache.solr.search.FieldParams.
 */
public class PhraseBoostFieldParams {

    private final int wordGrams;  // make bigrams if 2, trigrams if 3, or all if 0
    private final int slop;
    private final float boost;
    private final String field;

    public PhraseBoostFieldParams(String field, int wordGrams, int slop, float boost) {
        this.wordGrams = wordGrams;
        this.slop      = slop;
        this.boost     = boost;
        this.field     = field;
    }

    public int getWordGrams() {
        return wordGrams;
    }
    public int getSlop() {
        return slop;
    }
    public float getBoost() {
        return boost;
    }
    public String getField() {
        return field;
    }

}