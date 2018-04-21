/**
 * 
 */
package querqy.lucene.rewrite;

/**
 * Copied from org.apache.lucene.search.TermScorer, which is only package-visible
 * 
 */
import java.io.IOException;

import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.Similarity;

/** Expert: A <code>Scorer</code> for documents matching a <code>Term</code>.
 */
final class TermScorer extends Scorer {
  private final PostingsEnum postingsEnum;
  private final Similarity.SimScorer docScorer;

  /**
   * Construct a <code>TermScorer</code>.
   *
   * @param weight
   *          The weight of the <code>Term</code> in the query.
   * @param td
   *          An iterator over the documents matching the <code>Term</code>.
   * @param docScorer
   *          The </code>Similarity.SimScorer</code> implementation
   *          to be used for score computations.
   */
  TermScorer(Weight weight, PostingsEnum td, Similarity.SimScorer docScorer) {
    super(weight);
    this.docScorer = docScorer;
    this.postingsEnum = td;
  }

  @Override
  public int docID() {
    return postingsEnum.docID();
  }


  public int freq() throws IOException {
    return postingsEnum.freq();
  }

  @Override
  public DocIdSetIterator iterator() { return postingsEnum; }

  @Override
  public float score() throws IOException {
    assert docID() != DocIdSetIterator.NO_MORE_DOCS;
    return docScorer.score(postingsEnum.docID(), postingsEnum.freq());
  }


  /** Returns a string representation of this <code>TermScorer</code>. */
  @Override
  public String toString() { return "scorer(" + weight + ")[" + super.toString() + "]"; }
}
