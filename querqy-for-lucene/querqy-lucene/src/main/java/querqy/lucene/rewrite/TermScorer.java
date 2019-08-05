package querqy.lucene.rewrite;

/*
 * Copied from org.apache.lucene.search.TermScorer, which is only package-visible
 */
import java.io.IOException;

import org.apache.lucene.index.ImpactsEnum;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.SlowImpactsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.ImpactsDISI;
import org.apache.lucene.search.LeafSimScorer;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

/** Expert: A <code>Scorer</code> for documents matching a <code>Term</code>.
 */
final class TermScorer extends Scorer {
    private final PostingsEnum postingsEnum;
    private final ImpactsEnum impactsEnum;
    private final DocIdSetIterator iterator;
    private final LeafSimScorer docScorer;
    private final ImpactsDISI impactsDisi;

    /**
     * Construct a {@link org.apache.lucene.search.TermScorer} that will iterate all documents.
     */
    TermScorer(Weight weight, PostingsEnum postingsEnum, LeafSimScorer docScorer) {
        super(weight);
        iterator = this.postingsEnum = postingsEnum;
        impactsEnum = new SlowImpactsEnum(postingsEnum);
        impactsDisi = new ImpactsDISI(impactsEnum, impactsEnum, docScorer.getSimScorer());
        this.docScorer = docScorer;
    }

    /**
     * Construct a {@link org.apache.lucene.search.TermScorer} that will use impacts to skip blocks of
     * non-competitive documents.
     */
    TermScorer(Weight weight, ImpactsEnum impactsEnum, LeafSimScorer docScorer) {
        super(weight);
        postingsEnum = this.impactsEnum = impactsEnum;
        impactsDisi = new ImpactsDISI(impactsEnum, impactsEnum, docScorer.getSimScorer());
        iterator = impactsDisi;
        this.docScorer = docScorer;
    }

    @Override
    public int docID() {
        return postingsEnum.docID();
    }

    final int freq() throws IOException {
        return postingsEnum.freq();
    }

    @Override
    public DocIdSetIterator iterator() {
        return iterator;
    }

    @Override
    public float score() throws IOException {
        assert docID() != DocIdSetIterator.NO_MORE_DOCS;
        return docScorer.score(postingsEnum.docID(), postingsEnum.freq());
    }

    @Override
    public int advanceShallow(int target) throws IOException {
        return impactsDisi.advanceShallow(target);
    }

    @Override
    public float getMaxScore(int upTo) throws IOException {
        return impactsDisi.getMaxScore(upTo);
    }

    @Override
    public void setMinCompetitiveScore(float minScore) {
        impactsDisi.setMinCompetitiveScore(minScore);
    }

    /** Returns a string representation of this <code>TermScorer</code>. */
    @Override
    public String toString() { return "scorer(" + weight + ")[" + super.toString() + "]"; }
}
