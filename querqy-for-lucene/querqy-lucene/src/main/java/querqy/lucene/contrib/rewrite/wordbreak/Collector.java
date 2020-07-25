package querqy.lucene.contrib.rewrite.wordbreak;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

public class Collector {

    private final Queue<MorphologicalWordBreaker.BreakSuggestion> collection;
    private final int minSuggestionFrequency;
    private final boolean verifyCollation;
    private final IndexReader indexReader;
    private final String dictionaryField;
    private final float weightDfObservation;
    private final float totalDocsNorm;
    private final int maxDecompoundExpansions;
    private final IndexSearcher searcher;

    public Collector(final int minSuggestionFrequency,
                     final int maxDecompoundExpansions,
                     final boolean verifyCollation, final IndexReader indexReader, final String dictionaryField,
                     final float weightDfObservation) {

        final int queueInitialCapacity = Math.min(maxDecompoundExpansions, 10);
        collection = new PriorityQueue<>(queueInitialCapacity);

        this.minSuggestionFrequency = minSuggestionFrequency;
        this.maxDecompoundExpansions = maxDecompoundExpansions;
        this.verifyCollation = verifyCollation;
        this.indexReader = indexReader;
        searcher = new IndexSearcher(indexReader);
        this.dictionaryField = dictionaryField;
        this.weightDfObservation = weightDfObservation;
        this.totalDocsNorm = 2f * (float) Math.log(1 + indexReader.numDocs());
    }


    public boolean collect(final CharSequence left, final CharSequence right, final Term rightTerm, final int rightDf, final float strategyWeight) {

        final Term leftTerm = new Term(dictionaryField, new BytesRef(left));
        final int leftDf;
        try {
            leftDf = indexReader.docFreq(leftTerm);
            if (leftDf >= minSuggestionFrequency) {

                final float score = weightDfObservation == 0f ? strategyWeight
                        : strategyWeight / ((float) Math.pow(totalDocsNorm - Math.log(leftDf + 1) - Math.log(rightDf + 1), weightDfObservation));

                if (verifyCollation) {

                    if (((collection.size() < maxDecompoundExpansions) || (score > collection.element().score))
                            && hasMinMatches(1, leftTerm, rightTerm)) {
                        collection.offer(new MorphologicalWordBreaker.BreakSuggestion(new CharSequence[]{left, right}, score));
                        if (collection.size() > maxDecompoundExpansions) {
                            collection.poll();
                        }
                        return true;
                    }

                } else {
                    collection.offer(new MorphologicalWordBreaker.BreakSuggestion(new CharSequence[]{left, right}, score));
                    if (collection.size() > maxDecompoundExpansions) {
                        collection.poll();
                    }
                    return true;
                }

            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        return false;

    }

    public List<CharSequence[]> flushResults() {
        if (collection.isEmpty()) {
            return Collections.emptyList();
        }

        final LinkedList<CharSequence[]> result = new LinkedList<>();
        while (collection.size() > 0) {
            result.addFirst(collection.remove().sequence);
        }

        return result;
    }

    private boolean hasMinMatches(final int minCount, final Term... suggestion)
            throws IOException {

        if (suggestion.length != 2) {
            throw new IllegalArgumentException("Can only handle exactly two terms");
        }

        final IndexReaderContext topReaderContext = searcher.getTopReaderContext();
        final IndexReader indexReader = topReaderContext.reader();
        // FIXME: deleted documents!
        final int numDocs = indexReader.numDocs();
        if (minCount > numDocs) {
            return false;
        }

        final Term term1 = suggestion[0];
        final Term term2 = suggestion[1];

        final int df1 = indexReader.docFreq(term1);
        if (minCount > df1) {
            return false;
        }

        final int df2 = indexReader.docFreq(term2);
        if (minCount > df2) {
            return false;
        }

        int count = 0;

        for (final LeafReaderContext context : topReaderContext.leaves()) {

            final Terms terms1 = context.reader().terms(term1.field());
            final Terms terms2 = context.reader().terms(term2.field());

            final TermsEnum termsEnum1 = terms1.iterator();
            if (!termsEnum1.seekExact(term1.bytes())) {
                continue;
            }

            final TermsEnum termsEnum2 = terms2.iterator();
            if (!termsEnum2.seekExact(term2.bytes())) {
                continue;
            }

            final PostingsEnum postings1 = termsEnum1.postings(null, PostingsEnum.NONE);
            final PostingsEnum postings2 = termsEnum2.postings(null, PostingsEnum.NONE);

            int doc1 = postings1.nextDoc();
            while (doc1 != DocIdSetIterator.NO_MORE_DOCS) {
                int doc2 = postings2.advance(doc1);
                if (doc2 == DocIdSetIterator.NO_MORE_DOCS) {
                    break;
                }
                if (doc2 == doc1) {
                    count++;
                    if (count >= minCount) {
                        return true;
                    }
                } else if (doc2 > doc1) {
                    doc1 = postings1.advance(doc2);
                    if (doc2 == doc1) {
                        count++;
                        if (count >= minCount) {
                            return true;
                        }
                    }
                }
            }

        }

        return false;

    }

}
