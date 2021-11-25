package querqy.lucene.contrib.rewrite.wordbreak;

import org.apache.lucene.index.IndexReader;
import querqy.model.Term;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.stream.Collectors;

import static querqy.lucene.LuceneQueryUtil.toLuceneTerm;

public class MorphologicalCompounder implements LuceneCompounder {

    private static final int DEFAULT_MAX_COMPOUND_EXPANSIONS = 10;
    private final String dictionaryField;
    private final boolean lowerCaseInput;
    private final int minSuggestionFrequency;
    private final Morphology morphology;// move to constructor
    private final int maxCompoundExpansions;


    public MorphologicalCompounder(final Morphology morphology,
                                   final String dictionaryField,
                                   final boolean lowerCaseInput,
                                   final int minSuggestionFrequency,
                                   final int maxCompoundExpansions) {
        this.dictionaryField = dictionaryField;
        this.lowerCaseInput = lowerCaseInput;
        this.minSuggestionFrequency = minSuggestionFrequency;
        this.morphology = morphology;
        this.maxCompoundExpansions = maxCompoundExpansions;
    }

    public MorphologicalCompounder(final Morphology morphology,
                                   final String dictionaryField,
                                   final boolean lowercaseInput,
                                   final int minSuggestionFrequency) {
        this(morphology, dictionaryField, lowercaseInput, minSuggestionFrequency, DEFAULT_MAX_COMPOUND_EXPANSIONS);
    }


    @Override
    public List<CompoundTerm> combine(final Term[] terms, final IndexReader indexReader, final boolean reverse) {
        if (terms.length < 2) {
            return Collections.emptyList();
        }

        final Term left = lowerCaseInput ? terms[0].toLowerCaseTerm() : terms[0];
        final Term right = lowerCaseInput ? terms[1].toLowerCaseTerm() : terms[1];

        final int queueInitialCapacity = Math.min(maxCompoundExpansions, 10);
        final Collection<Compound> collector = Arrays.stream(morphology.suggestCompounds(left, right))
                .collect(Collectors.toCollection(() ->
                        new PriorityQueue<>(queueInitialCapacity))
                );

        return collector.stream()
                .sorted(Comparator.reverseOrder())
                .limit(maxCompoundExpansions)
                .map(compound -> new CompoundTerm(compound.compound, terms))
                .filter(compound -> {
                    final org.apache.lucene.index.Term compoundTerm = toLuceneTerm(dictionaryField, compound.value, false);
                    final int compoundDf;
                    try {
                        compoundDf = indexReader.docFreq(compoundTerm);
                    } catch (final IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    return (compoundDf >= minSuggestionFrequency);
                })
                .collect(Collectors.toList());
    }
}
