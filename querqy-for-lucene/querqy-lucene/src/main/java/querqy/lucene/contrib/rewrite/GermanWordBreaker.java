package querqy.lucene.contrib.rewrite;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;
import querqy.CompoundCharSequence;
import querqy.LowerCaseCharSequence;
import querqy.SimpleComparableCharSequence;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

public class GermanWordBreaker implements LuceneWordBreaker {

    private final int minSuggestionFrequency;
    private final int minBreakLength;
    private final String dictionaryField;
    private final boolean lowerCaseInput;

    public GermanWordBreaker(final String dictionaryField, final boolean lowerCaseInput,
                             final int minSuggestionFrequency, final int minBreakLength) {
        if (minBreakLength < 1) {
            throw new IllegalArgumentException("minBreakLength > 0 expected");
        }

        if (minSuggestionFrequency < 1) {
            throw new IllegalArgumentException("minSuggestionFrequency > 0 expected");
        }

        this.dictionaryField = dictionaryField;
        this.lowerCaseInput = lowerCaseInput;
        this.minSuggestionFrequency = minSuggestionFrequency;
        this.minBreakLength = minBreakLength;
    }

    @Override
    public List<CharSequence[]> breakWord(final CharSequence word, final IndexReader indexReader,
                                          final int maxDecompoundExpansions, final boolean verifyCollation)
            throws IOException {

        if (maxDecompoundExpansions < 1) {
            return Collections.emptyList();
        }

        final int queueInitialCapacity = Math.min(maxDecompoundExpansions, 10);
        final Queue<BreakSuggestion> suggestions = new PriorityQueue<>(queueInitialCapacity, QUEUE_COMPARATOR);
        collectSuggestions(word, indexReader, maxDecompoundExpansions, verifyCollation, suggestions);

        if (suggestions.isEmpty()) {
            return Collections.emptyList();
        }

        final LinkedList<CharSequence[]> result = new LinkedList<>();
        while (suggestions.size() > 0) {
            result.addFirst(suggestions.remove().sequence);
        }

        return result;

    }

    /**
     * 22759  Kohlsuppe
     * 9637 s Staatsfeind
     * 5307 n Soziologenkongre
     * 4316 en Straußenei
     * 2610 nen Wochnerinnenheim
     * 618 	us en Aphorismenschatz
     * 348 	um en Museenverwaltung
     * 255 	um a Aphrodisiakaverkaufer
     * 122 	e Kirchhof
     * 95 	a en Madonnenkult
     * 87 e Hundehalter
     * 73 \ e Ganseklein
     * 59 	on en Stadienverbot
     * 43 es Geisteshaltung
     * 38 \ er Blatterwald
     * 33 	en Sudwind
     * 28 	on a Pharmakaanalyse
     * 25 er Geisterstunde
     * 19 ien Prinzipienreiter
     * 11 	e i Carabinierischule
     */

    final static float NORM_PRIOR = 22759f;

    final static float PRIOR_0 = 1f;

    final static float PRIOR_PLUS_E = 87f/NORM_PRIOR;
    final static float PRIOR_PLUS_N = 5307f/NORM_PRIOR;
    final static float PRIOR_PLUS_S = 9637f/NORM_PRIOR;

    final static float PRIOR_PLUS_EN = 4316f/NORM_PRIOR;
    final static float PRIOR_PLUS_ER = 25f/NORM_PRIOR;
    final static float PRIOR_PLUS_ES = 43f/NORM_PRIOR;

    final static float PRIOR_MINUS_E = 122f/NORM_PRIOR;

    // -us +en, -um +en, -a +en, -on +en
    final static float PRIOR_MINUS_US_PLUS_EN = 618f/NORM_PRIOR;
    final static float PRIOR_MINUS_UM_PLUS_EN = 348f/NORM_PRIOR;
    final static float PRIOR_MINUS_A_PLUS_EN = 95f/NORM_PRIOR;
    final static float PRIOR_MINUS_ON_PLUS_EN = 59f/NORM_PRIOR;

    protected void collectSuggestions(final CharSequence word, final IndexReader indexReader,
                                      final int maxDecompoundExpansions, final boolean verifyCollation,
                                      final Queue<BreakSuggestion> collection) throws IOException {


        final int termLength = Character.codePointCount(word, 0, word.length());
        if (termLength < minBreakLength) {
            return;
        }

        final CharSequence input = lowerCaseInput && (!(word instanceof LowerCaseCharSequence))
                ? new LowerCaseCharSequence(word) : word;


        final IndexSearcher searcher = new IndexSearcher(indexReader);

        // the original left term can be longer than rightOfs because the compounding might have removed characters
        // TODO: find min left size (based on linking morphemes and minBreakLength)
        for (int leftLength = termLength - minBreakLength; leftLength > 0; leftLength--) {

            int splitIndex = Character.offsetByCodePoints(input, 0, leftLength);

            final CharSequence right = input.subSequence(splitIndex, input.length());
            final Term rightTerm = new Term(dictionaryField, new BytesRef(right));

            final int rightDf = indexReader.docFreq(rightTerm);
            if (rightDf < minSuggestionFrequency) {
                continue;
            }

            final CharSequence left = input.subSequence(0, splitIndex);

            if (leftLength + 1 >= Math.max(4, minBreakLength)) {

                // strategy: -e, we assume that the -e could only be stripped off if the word had at least 4
                // or minBreakLength chars
                if (maybeOfferTerm(new CompoundCharSequence(null, left, "e"), right, rightTerm,
                        verifyCollation, PRIOR_MINUS_E, indexReader, searcher, collection)
                        && (collection.size() > maxDecompoundExpansions)) {
                    collection.poll();
                }
            }

            if (leftLength >= minBreakLength) {

                // strategy: no linking element
                if (maybeOfferTerm(left, right, rightTerm, verifyCollation, PRIOR_0, indexReader, searcher, collection)
                        && (collection.size() > maxDecompoundExpansions)) {
                    collection.poll();
                }

                // if we can remove a 1 char linking morpheme (while adding no suffix)
                if (leftLength >= minBreakLength + 1) {

                    final char lastChar = left.charAt(splitIndex - 1);

                    float prior;
                    switch (lastChar) {
                        case 's': prior = PRIOR_PLUS_S; break;
                        case 'n': prior = PRIOR_PLUS_N; break;
                        case 'e': prior = PRIOR_PLUS_E; break;
                        default: prior = -1f;
                    }
                    if (prior > 0f) {
                        // we don't have to care about surrogate chars here
                        // (our morphemes are not surrogate pairs or part of it)
                        final CharSequence shorterByOne = input.subSequence(0, splitIndex - 1);

                        if (maybeOfferTerm(shorterByOne, right, rightTerm, verifyCollation, prior, indexReader,
                                searcher, collection) && (collection.size() > maxDecompoundExpansions)) {
                            collection.poll();
                        }

                    }

                    if (leftLength >= minBreakLength + 2) {
                        final char penultimateChar = left.charAt(splitIndex - 2);
                        if (penultimateChar == 'e') {
                            // +es, +en, +er
                            switch (lastChar) {
                                case 's': prior = PRIOR_PLUS_ES; break;
                                case 'n': prior = PRIOR_PLUS_EN; break;
                                case 'r': prior = PRIOR_PLUS_ER; break;
                                default: prior = -1f;
                            }
                            if (prior > 0f) {
                                // we don't have to care about surrogate chars here
                                // (our morphemes are not surrogate pairs or part of it)
                                final CharSequence shorterByTwo = input.subSequence(0, splitIndex - 2);

                                if (maybeOfferTerm(shorterByTwo, right, rightTerm, verifyCollation, prior, indexReader,
                                        searcher, collection) && (collection.size() > maxDecompoundExpansions)) {
                                    collection.poll();
                                }

                                if (lastChar == 'n') {
                                    // -us +en, -um +en, -a +en, -on +en

                                    // can we use a prefix lookup in the index for shorterByTwo?
                                    if (maybeOfferTerm(new CompoundCharSequence(null, shorterByTwo, "us"),
                                            right, rightTerm, verifyCollation, PRIOR_MINUS_US_PLUS_EN, indexReader,
                                            searcher, collection) && (collection.size() > maxDecompoundExpansions)) {
                                        collection.poll();
                                    }

                                    if (maybeOfferTerm(new CompoundCharSequence(null, shorterByTwo, "um"),
                                            right, rightTerm, verifyCollation, PRIOR_MINUS_UM_PLUS_EN, indexReader,
                                            searcher, collection) && (collection.size() > maxDecompoundExpansions)) {
                                        collection.poll();
                                    }

                                    if (maybeOfferTerm(new CompoundCharSequence(null, shorterByTwo, "a"),
                                            right, rightTerm, verifyCollation, PRIOR_MINUS_A_PLUS_EN, indexReader,
                                            searcher, collection) && (collection.size() > maxDecompoundExpansions)) {
                                        collection.poll();
                                    }

                                    if (maybeOfferTerm(new CompoundCharSequence(null, shorterByTwo, "on"),
                                            right, rightTerm, verifyCollation, PRIOR_MINUS_ON_PLUS_EN, indexReader,
                                            searcher, collection) && (collection.size() > maxDecompoundExpansions)) {
                                        collection.poll();
                                    }

                                }

                            }


                        }

                    }

                }

            }



        }



    }

    private boolean maybeOfferTerm(final CharSequence left, final CharSequence right, final Term rightTerm,
                                   final boolean verifyCollation, float prior, final IndexReader indexReader,
                                   final IndexSearcher searcher, final Queue<BreakSuggestion> collection)
            throws IOException {

        final Term leftTerm = new Term(dictionaryField, new BytesRef(left));
        final int leftDf = indexReader.docFreq(leftTerm);
        if (leftDf >= minSuggestionFrequency) {

            if (verifyCollation) {

                final int numMatches = countCollatedMatches(searcher, leftTerm, rightTerm);
                if (numMatches >= minSuggestionFrequency) {
                    collection.offer(
                            new BreakSuggestion(new CharSequence[]{left, right}, prior * (float) numMatches));
                    return true;
                }

            } else {
                collection.offer(
                        new BreakSuggestion(new CharSequence[]{left, right}, prior * (float) left.length()));
                return true;
            }

        }

        return false;

    }

    protected int countCollatedMatches(final IndexSearcher searcher, final Term... suggestion) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for (final Term term : suggestion) {
            builder.add(new BooleanClause(new TermQuery(term), BooleanClause.Occur.FILTER));
        }

        try {
            return searcher.count(builder.build());
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    static class BreakSuggestion {

        final CharSequence[] sequence;
        final float score;

        BreakSuggestion(final CharSequence[] sequence, final float score) {
            this.sequence = sequence;
            this.score = score;
        }


    }

    static Comparator<BreakSuggestion> QUEUE_COMPARATOR = (o1, o2) -> {
        final int c = Float.compare(o1.score, o2.score); // greater is better
        return (c == 0 && o1 != o2)
                // Use length as a tie breaker. If length is still a tie, we leave the decision to the priority queue.
                ? Integer.compare(o1.sequence.length, o2.sequence.length) // shorter is better
                : c;

    };

    public static void main(final String[] args) {
        CharSequence s = new SimpleComparableCharSequence("\ud83c\udf09a".toCharArray());
        int end = Character.offsetByCodePoints(s, 0, 1);
        System.out.println(end);

        System.out.println(s.length());
        System.out.println(Character.codePointCount(s, 0, s.length()));
        System.out.println(s);
    }

}
