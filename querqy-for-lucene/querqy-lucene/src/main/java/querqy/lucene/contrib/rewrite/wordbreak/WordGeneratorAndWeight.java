package querqy.lucene.contrib.rewrite.wordbreak;

public class WordGeneratorAndWeight {

    public final WordGenerator generator;
    public final float weight;

    public WordGeneratorAndWeight(final WordGenerator generator, final float weight) {
        this.generator = generator;
        this.weight = weight;
    }
}
