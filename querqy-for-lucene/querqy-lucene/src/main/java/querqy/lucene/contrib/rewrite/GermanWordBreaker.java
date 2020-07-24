package querqy.lucene.contrib.rewrite;

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
import querqy.CharSequenceUtil;
import querqy.CompoundCharSequence;
import querqy.LowerCaseCharSequence;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * S. Langer. 1998. Zur Morphologie und Semantik von Nominalkomposita. Tagungsband der 4. Konferenz zur Verarbeitung
 *   natürlicher Sprache (KONVENS).
 */

public class GermanWordBreaker implements LuceneWordBreaker {

    /**
     * 22759  Kohlsuppe - DONE
     * 9637 +s Staatsfeind - DONE
     * 5307 +n Soziologenkongress- DONE
     * 4316 +en Straußenei - DONE
     * 2610 +nen Wöchnerinnenheim - DONE
     * 618 	-us +en Aphorismenschatz - DONE
     * 348 	-um +en Museenverwaltung - DONE
     * 255 	-um +a Aphrodisiakaverkaufer - DONE
     * 122 	-e Kirchhof - DONE
     * 95 	-a +en Madonnenkult  - DONE
     * 87  +e Hundehalter  - DONE
     * 73  +" +e Gänseklein - DONE
     * 59 	-on +en Stadienverbot - DONE
     * 43  +es Geisteshaltung - DONE
     * 38  +" +er Blätterwald - DONE
     * 33 	-en Südwind - DONE
     * 28 	-on +a Pharmakaanalyse - DONE
     * 25 +er Geisterstunde - DONE
     * 19 ien Prinzipienreiter - DONE
     * 11 	-e +i Carabinierischule -DONE
     */

    /*

     */
    /**
     * We use the frequencies of the compounding strategies [Langer 1998] to create a 'prior weight' for each strategy.
     * The prior can be updated by df-based calculations to rank the de-compounded candidates.
     */

    // count of the most frequent strategy (no morphological change)
    final static float NORM_PRIOR = 22759f;

    final static float PRIOR_0 = 1f;

    final static float PRIOR_PLUS_E = 87f / NORM_PRIOR;
    final static float PRIOR_PLUS_N = 5307f / NORM_PRIOR;
    final static float PRIOR_PLUS_S = 9637f / NORM_PRIOR;
    final static float PRIOR_PLUS_UMLAUT_E = 73f / NORM_PRIOR;

    final static float PRIOR_PLUS_EN = 4316f / NORM_PRIOR;
    final static float PRIOR_PLUS_ER = 25f / NORM_PRIOR;
    final static float PRIOR_PLUS_ES = 43f / NORM_PRIOR;

    final static float PRIOR_PLUS_UMLAUT_ER = 38f / NORM_PRIOR;

    final static float PRIOR_PLUS_NEN = 2610f / NORM_PRIOR;
    final static float PRIOR_PLUS_IEN = 19f / NORM_PRIOR;

    final static float PRIOR_MINUS_E = 122f / NORM_PRIOR;
    final static float PRIOR_MINUS_EN = 33f / NORM_PRIOR;

    // -us +en, -um +en, -a +en, -on +en, -on +a
    final static float PRIOR_MINUS_US_PLUS_EN = 618f / NORM_PRIOR;
    final static float PRIOR_MINUS_UM_PLUS_EN = 348f / NORM_PRIOR;
    final static float PRIOR_MINUS_A_PLUS_EN = 95f / NORM_PRIOR;
    final static float PRIOR_MINUS_ON_PLUS_EN = 59f / NORM_PRIOR;
    final static float PRIOR_MINUS_ON_PLUS_A = 28f / NORM_PRIOR;

    // -e +i
    final static float PRIOR_MINUS_E_PLUS_I = 11f / NORM_PRIOR;
    // -us +a
    final static float PRIOR_MINUS_UM_PLUS_A = 255f / NORM_PRIOR;

    public static final float DEFAULT_WEIGHT_STRATEGY = 0.8f;


    private final int minSuggestionFrequency;
    private final int minBreakLength;
    private final String dictionaryField;
    private final boolean lowerCaseInput;
    final float weight0;
    final float weightPlusE;
    final float weightPlusN;
    final float weightPlusS;
    final float weightPlusEn;
    final float weightPlusEr;
    final float weightPlusUmlautE;
    final float weightPlusUmlautEr;
    final float weightPlusEs;
    final float weightPlusNen;
    final float weightPlusIen;
    final float weightMinusE;
    final float weightMinusEn;
    final float weightMinusUsPlusEn;
    final float weightMinusUmPlusEn;
    final float weightMinusUmPlusA;
    final float weightMinusAPlusEn;
    final float weightMinusEPlusI;
    final float weightMinusOnPlusEn;
    final float weightMinusOnPlusA;
    final float weightDfObservation;

    public GermanWordBreaker(final String dictionaryField, final boolean lowerCaseInput,
                             final int minSuggestionFrequency, final int minBreakLength) {
        this(dictionaryField, lowerCaseInput, minSuggestionFrequency, minBreakLength, DEFAULT_WEIGHT_STRATEGY);
    }

    public GermanWordBreaker(final String dictionaryField, final boolean lowerCaseInput,
                             final int minSuggestionFrequency, final int minBreakLength, final float weightStrategy) {
        if (minBreakLength < 1) {
            throw new IllegalArgumentException("minBreakLength > 0 expected");
        }

        if (minSuggestionFrequency < 1) {
            throw new IllegalArgumentException("minSuggestionFrequency > 0 expected");
        }

        if (weightStrategy < 0f || weightStrategy > 1f) {
            throw new IllegalArgumentException("weightStrategy between 0.0 and 1.0 expected");
        }

        this.dictionaryField = dictionaryField;
        this.lowerCaseInput = lowerCaseInput;
        this.minSuggestionFrequency = minSuggestionFrequency;
        this.minBreakLength = minBreakLength;
        this.weight0 = (float) Math.pow(PRIOR_0, weightStrategy);
        this.weightPlusE = (float) Math.pow(PRIOR_PLUS_E, weightStrategy);
        this.weightPlusN = (float) Math.pow(PRIOR_PLUS_N, weightStrategy);
        this.weightPlusS = (float) Math.pow(PRIOR_PLUS_S, weightStrategy);
        this.weightPlusEn = (float) Math.pow(PRIOR_PLUS_EN, weightStrategy);
        this.weightPlusEr = (float) Math.pow(PRIOR_PLUS_ER, weightStrategy);
        this.weightPlusUmlautE = (float) Math.pow(PRIOR_PLUS_UMLAUT_E, weightStrategy);
        this.weightPlusUmlautEr = (float) Math.pow(PRIOR_PLUS_UMLAUT_ER, weightStrategy);
        this.weightPlusEs = (float) Math.pow(PRIOR_PLUS_ES, weightStrategy);
        this.weightPlusNen = (float) Math.pow(PRIOR_PLUS_NEN, weightStrategy);
        this.weightPlusIen = (float) Math.pow(PRIOR_PLUS_IEN, weightStrategy);
        this.weightMinusE = (float) Math.pow(PRIOR_MINUS_E, weightStrategy);
        this.weightMinusEn = (float) Math.pow(PRIOR_MINUS_EN, weightStrategy);
        this.weightMinusUsPlusEn = (float) Math.pow(PRIOR_MINUS_US_PLUS_EN, weightStrategy);
        this.weightMinusUmPlusEn = (float) Math.pow(PRIOR_MINUS_UM_PLUS_EN, weightStrategy);
        this.weightMinusUmPlusA = (float) Math.pow(PRIOR_MINUS_UM_PLUS_A, weightStrategy);
        this.weightMinusAPlusEn = (float) Math.pow(PRIOR_MINUS_A_PLUS_EN, weightStrategy);
        this.weightMinusEPlusI = (float) Math.pow(PRIOR_MINUS_E_PLUS_I, weightStrategy);
        this.weightMinusOnPlusEn = (float) Math.pow(PRIOR_MINUS_ON_PLUS_EN, weightStrategy);
        this.weightMinusOnPlusA = (float) Math.pow(PRIOR_MINUS_ON_PLUS_A, weightStrategy);
        this.weightDfObservation = 1f - weightStrategy;

    }

    @Override
    public List<CharSequence[]> breakWord(final CharSequence word, final IndexReader indexReader,
                                          final int maxDecompoundExpansions, final boolean verifyCollation)
            throws IOException {

        if (maxDecompoundExpansions < 1) {
            return Collections.emptyList();
        }

        final int queueInitialCapacity = Math.min(maxDecompoundExpansions, 10);
        final Queue<BreakSuggestion> suggestions = new PriorityQueue<>(queueInitialCapacity);

        try {
            collectSuggestions(word, indexReader, maxDecompoundExpansions, verifyCollation, suggestions);
        } catch (final UncheckedIOException e) {
            throw e.getCause();
        }

        if (suggestions.isEmpty()) {
            return Collections.emptyList();
        }

        final LinkedList<CharSequence[]> result = new LinkedList<>();
        while (suggestions.size() > 0) {
            result.addFirst(suggestions.remove().sequence);
        }

        return result;

    }

    protected static class WeightedSequence {
        final CharSequence sequence;
        final float weight;

        public WeightedSequence(final CharSequence sequence, final float weight) {
            this.sequence = sequence;
            this.weight = weight;
        }
    }

    protected static class SuffixForm extends WeightedSequence {

        protected SuffixForm(final CharSequence suffix, final float weight) {
            super(suffix, weight);
        }

        public Optional<WeightedSequence> apply(final CharSequence baseForm) {
            return Optional.of(sequence == null
                    ? new WeightedSequence(baseForm, weight)
                    : new WeightedSequence(new CompoundCharSequence(null, baseForm, sequence), weight));
        }

    }

    protected static class UmlautAndSuffix extends SuffixForm {

        protected UmlautAndSuffix(final CharSequence suffix, final float weight) {
            super(suffix, weight);
        }

        public Optional<WeightedSequence> apply(final CharSequence baseForm) {
            // +umlaut +e
            // We only check one vowel to the left for an umlaut, except if it is an 'u', in which case
            // we will check whether it's an 'äu'

            String replacement = null;
            int position = baseForm.length() - 2;
            while ((position > -1) && (replacement == null)) {
                switch (baseForm.charAt(position)) {
                    case 'a':
                    case 'o':
                        replacement = null;
                        position = -1;
                        break;
                    case 'u':
                        if (position > 0 && baseForm.charAt(position - 1) == 'ä') {
                            position -= 1;
                            replacement = "a";
                        } else {
                            replacement = null;
                            position = -1;
                        }
                        break;
                    case 'ä':
                        replacement = "a";
                        break;
                    case 'ö':
                        replacement = "o";
                        break;
                    case 'ü':
                        replacement = "u";
                        break;
                    default:
                        replacement = null;
                        position--;
                }
            }

            if (replacement == null) {
                return Optional.empty();
            }

            final CharSequence word = sequence == null
                        ? new CompoundCharSequence(null, baseForm.subSequence(0, position), replacement,
                            baseForm.subSequence(position + 1, baseForm.length()))
                        : new CompoundCharSequence(null, baseForm.subSequence(0, position), replacement,
                            baseForm.subSequence(position + 1, baseForm.length()), sequence);




            return Optional.of(new WeightedSequence(word, weight));



        }

    }

    protected void collectSuggestions(final CharSequence word, final IndexReader indexReader,
                                      final int maxDecompoundExpansions, final boolean verifyCollation,
                                      final Queue<BreakSuggestion> collection) throws UncheckedIOException {


        final int termLength = Character.codePointCount(word, 0, word.length());
        if (termLength < minBreakLength) {
            return;
        }

        final CharSequence input = lowerCaseInput && (!(word instanceof LowerCaseCharSequence))
                ? new LowerCaseCharSequence(word) : word;


        final IndexSearcher searcher = new IndexSearcher(indexReader);

        // TODO live docs
        final float totalDocsNorm = 2f * (float) Math.log(1 + indexReader.numDocs());

//        final Term inputTerm = new Term(dictionaryField, new BytesRef(input));
//        try {
//            System.out.println("Input DF: " + indexReader.docFreq(inputTerm));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        // the original left term can be longer than rightOfs because the compounding might have removed characters
        // TODO: find min left size (based on linking morphemes and minBreakLength)
        for (int leftLength = termLength - minBreakLength; leftLength > 0; leftLength--) {

            int splitIndex = Character.offsetByCodePoints(input, 0, leftLength);

            final CharSequence right = input.subSequence(splitIndex, input.length());
            final Term rightTerm = new Term(dictionaryField, new BytesRef(right));

            final int rightDf;
            try {
                rightDf = indexReader.docFreq(rightTerm);
                //System.out.println(rightTerm + ": " + rightDf);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
            if (rightDf < minSuggestionFrequency) {
                continue;
            }

            final CharSequence left = input.subSequence(0, splitIndex);

            // strategy: -er, we assume that the -er could only be stripped off if the word had at least 5
            // or minBreakLength chars
            if (leftLength + 2 >= Math.max(5, minBreakLength)) {

                if (maybeOfferTerm(new CompoundCharSequence(null, left, "en"), right, rightTerm, rightDf, totalDocsNorm,
                        verifyCollation, maxDecompoundExpansions, minSuggestionFrequency, weightMinusE, indexReader, searcher, collection)
                        && (collection.size() > maxDecompoundExpansions)) {
                    collection.poll();
                }

            }


            if (leftLength + 1 >= Math.max(4, minBreakLength)) {

                // strategy: -e, we assume that the -e could only be stripped off if the word had at least 4
                // or minBreakLength chars
                if (maybeOfferTerm(new CompoundCharSequence(null, left, "e"), right, rightTerm, rightDf, totalDocsNorm,
                        verifyCollation, maxDecompoundExpansions, minSuggestionFrequency, weightMinusE, indexReader, searcher, collection)
                        && (collection.size() > maxDecompoundExpansions)) {
                    collection.poll();
                }
            }

            if (leftLength >= minBreakLength) {

                // strategy: no linking element
                if (maybeOfferTerm(left, right, rightTerm, rightDf, totalDocsNorm, verifyCollation, maxDecompoundExpansions, minSuggestionFrequency, weight0, indexReader, searcher, collection)
                        && (collection.size() > maxDecompoundExpansions)) {
                    collection.poll();
                }

                // if we can remove a 1 char linking morpheme (while adding no suffix)
                if (leftLength >= minBreakLength + 1) {

                    final char lastChar = left.charAt(splitIndex - 1);

                    final SuffixForm[] suffixForms;
                    // +s, +n, +e, -um +a, -on +a, -e +i
                    switch (lastChar) {
                        case 's': suffixForms = new SuffixForm[] {new SuffixForm(null, weightPlusS)}; break;
                        case 'n': suffixForms = new SuffixForm[] {new SuffixForm(null, weightPlusN)}; break;
                        case 'e': suffixForms = new SuffixForm[] {new SuffixForm(null, weightPlusE), new UmlautAndSuffix(null, weightMinusOnPlusEn)}; break;
                        case 'a': suffixForms = new SuffixForm[] {new SuffixForm("um", weightMinusUmPlusA), new SuffixForm("on", weightMinusOnPlusA)}; break;
                        case 'i': suffixForms = new SuffixForm[] {new SuffixForm("e", weightMinusEPlusI)}; break;
                        default: suffixForms = null;
                    }

                    if (suffixForms != null) {
                        // we don't have to care about surrogate chars here
                        // (our morphemes are not surrogate pairs or part of it)
                        final CharSequence shorterByOne = input.subSequence(0, splitIndex - 1);

                        Arrays.stream(suffixForms)
                                .map(suffixForm -> suffixForm.apply(shorterByOne))
                                .filter(optionalWeightedSequence -> optionalWeightedSequence.map(weightedSequence -> {
                                    final boolean added = maybeOfferTerm(weightedSequence.sequence, right,
                                            rightTerm, rightDf, totalDocsNorm, verifyCollation,
                                            maxDecompoundExpansions, minSuggestionFrequency,
                                            weightedSequence.weight, indexReader, searcher,
                                            collection);

                                    if (added && (collection.size() > maxDecompoundExpansions)) {
                                        collection.poll();
                                    }

                                    return added;
                                }).orElse(false)
                                ).limit(1).findFirst();



                    }

                    if (leftLength >= minBreakLength + 2) {
                        final char penultimateChar = left.charAt(splitIndex - 2);
                        if (penultimateChar == 'e') {

                            float strategyWeight;

                            // +es, +en, +er
                            switch (lastChar) {
                                case 's': strategyWeight = weightPlusEs; break;
                                case 'n': strategyWeight = weightPlusEn; break;
                                case 'r': strategyWeight = weightPlusEr; break;
                                default: strategyWeight = -1f;
                            }
                            if (strategyWeight > 0f) {
                                // we don't have to care about surrogate chars here
                                // (our morphemes are not surrogate pairs or part of it)
                                final CharSequence shorterByTwo = input.subSequence(0, splitIndex - 2);

                                if (maybeOfferTerm(shorterByTwo, right, rightTerm, rightDf, totalDocsNorm, verifyCollation, maxDecompoundExpansions, minSuggestionFrequency, strategyWeight, indexReader,
                                        searcher, collection) && (collection.size() > maxDecompoundExpansions)) {
                                    collection.poll();
                                }

                                switch (lastChar) {
                                    case 'n': {
                                        // -us +en, -um +en, -a +en, -on +en


                                        // these suffixes should be mutually exclusive
                                        if (maybeOfferTerm(new CompoundCharSequence(null, shorterByTwo, "us"),
                                                right, rightTerm, rightDf, totalDocsNorm, verifyCollation, maxDecompoundExpansions, minSuggestionFrequency, weightMinusUsPlusEn, indexReader,
                                                searcher, collection)) {

                                            if (collection.size() > maxDecompoundExpansions) {
                                                collection.poll();
                                            }

                                        } else if (maybeOfferTerm(new CompoundCharSequence(null, shorterByTwo, "um"),
                                                right, rightTerm, rightDf, totalDocsNorm, verifyCollation, maxDecompoundExpansions, minSuggestionFrequency, weightMinusUmPlusEn, indexReader,
                                                searcher, collection)) {
                                            if (collection.size() > maxDecompoundExpansions) {
                                                collection.poll();
                                            }
                                        } else if (maybeOfferTerm(new CompoundCharSequence(null, shorterByTwo, "a"),
                                                right, rightTerm, rightDf, totalDocsNorm, verifyCollation, maxDecompoundExpansions, minSuggestionFrequency, weightMinusAPlusEn, indexReader,
                                                searcher, collection)) {
                                            if (collection.size() > maxDecompoundExpansions) {
                                                collection.poll();
                                            }

                                        } else if (maybeOfferTerm(new CompoundCharSequence(null, shorterByTwo, "on"),
                                                right, rightTerm, rightDf, totalDocsNorm, verifyCollation, maxDecompoundExpansions, minSuggestionFrequency, weightMinusOnPlusEn, indexReader,
                                                searcher, collection) && (collection.size() > maxDecompoundExpansions)) {
                                            collection.poll();
                                        }

                                        // +nen, +ien
                                        if (leftLength >= minBreakLength + 3) {

                                            final SuffixForm form;
                                            switch (left.charAt(splitIndex - 3)) {
                                                case 'n': form = new SuffixForm(null, weightPlusNen); break;
                                                case 'i': form = new SuffixForm(null, weightPlusIen); break;
                                                default: form = null;
                                            }
                                            if (form != null) {
                                                form.apply(input.subSequence(0, splitIndex - 3))
                                                        .ifPresent(weightedSequence -> {
                                                    if (maybeOfferTerm(weightedSequence.sequence, right,
                                                            rightTerm, rightDf, totalDocsNorm, verifyCollation,
                                                            maxDecompoundExpansions, minSuggestionFrequency, weightedSequence.weight,
                                                            indexReader, searcher, collection)
                                                            && (collection.size() > maxDecompoundExpansions))  {
                                                        collection.poll();
                                                    }
                                                });
                                            }


                                        }

                                    }
                                    break;
                                    case 'r': {
                                        // +umlaut +er
                                        // We only check the next vowel to the left if it is an umlaut.

                                        for (int position = shorterByTwo.length() - 2; position > -1; position--) {
                                            final String replacement;
                                            switch (shorterByTwo.charAt(position)) {
                                                case 'a':
                                                case 'o':
                                                case 'u':
                                                    replacement = null; position = -1; break;
                                                case 'ä': replacement = "a"; break;
                                                case 'ö': replacement = "o"; break;
                                                case 'ü': replacement = "u"; break;
                                                default: replacement = null;
                                            }

                                            if (replacement != null) {
                                                final CharSequence baseForm = new CompoundCharSequence(null,
                                                        shorterByTwo.subSequence(0, position),
                                                        replacement,
                                                        shorterByTwo.subSequence(position + 1, shorterByTwo.length()));
                                                if (maybeOfferTerm(baseForm, right, rightTerm, rightDf,
                                                        totalDocsNorm, verifyCollation, maxDecompoundExpansions,
                                                        minSuggestionFrequency, weightMinusOnPlusEn, indexReader,
                                                        searcher, collection) && (collection.size() > maxDecompoundExpansions)) {
                                                    collection.poll();
                                                }

                                                position = -1;
                                            }

                                        }

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
                                   final int rightDf, final float totalDocsNorm,
                                   final boolean verifyCollation, final int maxDecompoundExpansions,
                                   final int minSuggestionFrequency, final float strategyWeight,
                                   final IndexReader indexReader, final IndexSearcher searcher,
                                   final Queue<BreakSuggestion> collection)
            throws UncheckedIOException {

        final Term leftTerm = new Term(dictionaryField, new BytesRef(left));
        final int leftDf;
        try {
            leftDf = indexReader.docFreq(leftTerm);


            if (leftDf >= minSuggestionFrequency) {

                //System.out.println(leftTerm + ": " + leftDf);
                final float score = weightDfObservation == 0f ? strategyWeight
                        : strategyWeight / ((float) Math.pow(totalDocsNorm - Math.log(leftDf + 1) - Math.log(rightDf + 1), weightDfObservation));

                if (verifyCollation) {

                    if (((collection.size() < maxDecompoundExpansions) || (score > collection.element().score))

                            && hasMinMatches(searcher, 1, leftTerm, rightTerm)) {

                        collection.offer(new BreakSuggestion(new CharSequence[]{left, right}, score));
                        return true;
                    }

                } else {
                    collection.offer(new BreakSuggestion(new CharSequence[]{left, right}, score));
                    return true;
                }

            }

        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        return false;

    }

    private boolean hasMinMatches(final IndexSearcher searcher, final int minCount, final Term... suggestion)
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



    static class BreakSuggestion implements Comparable<BreakSuggestion> {

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


}
