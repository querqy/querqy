package querqy.lucene.contrib.rewrite;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.spell.CombineSuggestion;
import org.apache.lucene.search.spell.SuggestMode;
import org.apache.lucene.search.spell.SuggestWord;
import org.apache.lucene.search.spell.WordBreakSpellChecker;
import querqy.model.AbstractNodeVisitor;
import querqy.model.BooleanClause;
import querqy.model.BooleanQuery;
import querqy.model.Clause;
import querqy.model.DisjunctionMaxClause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.Node;
import querqy.model.QuerqyQuery;
import querqy.model.Query;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.trie.TrieMap;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class WordBreakCompoundRewriter extends AbstractNodeVisitor<Node> implements QueryRewriter {

    private static final int COMPOUND_WINDOW = 3;
    private static final int MAX_EXPANSION = 5;

    private final WordBreakSpellChecker wordBreakSpellChecker;
    private final IndexReader indexReader;
    private final String dictionaryField;

    // We are not using this as a map but as a kind of a set to look up CharSequences quickly
    private final TrieMap<Boolean> reverseCompoundTriggerWords;

    private ArrayDeque<Term> previousTerms = null;

    //
    private List<Node> nodesToAdd = null;

    private boolean reverseCompound = false;
    private final boolean alwaysAddReverseCompounds;

    public WordBreakCompoundRewriter(final WordBreakSpellChecker wordBreakSpellChecker,
                                     final IndexReader indexReader,
                                     final String dictionaryField,
                                     final boolean alwaysAddReverseCompounds,
                                     final TrieMap<Boolean> reverseCompoundTriggerWords) {

        if (reverseCompoundTriggerWords == null) {
            throw new IllegalArgumentException("reverseCompoundTriggerWords must not be null");
        }

        this.alwaysAddReverseCompounds = alwaysAddReverseCompounds;
        this.reverseCompoundTriggerWords = reverseCompoundTriggerWords;
        this.wordBreakSpellChecker = wordBreakSpellChecker;
        this.indexReader = indexReader;
        this.dictionaryField = dictionaryField;
        reverseCompound = alwaysAddReverseCompounds;

    }

    @Override
    public ExpandedQuery rewrite(final ExpandedQuery query) {
        final QuerqyQuery<?> userQuery = query.getUserQuery();
        if (userQuery instanceof Query){
            previousTerms = new ArrayDeque<>(COMPOUND_WINDOW);
            nodesToAdd = new LinkedList<>();
            visit((Query) userQuery);

            // append nodesToAdd to parent query
            nodesToAdd.forEach(node -> {
                final Node parent = node.getParent();
                // TODO: extend BooleanParent? interface so that we don't need this cast?
                if (parent instanceof DisjunctionMaxQuery) {
                    ((DisjunctionMaxQuery) parent).addClause((DisjunctionMaxClause) node);
                } else if (parent instanceof BooleanQuery) {
                    ((BooleanQuery) parent).addClause((BooleanClause) node);
                } else {
                    throw new IllegalStateException("Unknown parent type " + parent.getClass().getName());
                }

            });

        }
        return query;
    }

    @Override
    public Node visit(final DisjunctionMaxQuery dmq) {
        final List<DisjunctionMaxClause> clauses = dmq.getClauses();
        if (clauses != null && !clauses.isEmpty()) {
            DisjunctionMaxClause nonGeneratedClause = null;
            for (final DisjunctionMaxClause clause: clauses) {
                if (!clause.isGenerated()) {
                    // second non-generated clause - cannot handle this
                    if (nonGeneratedClause != null) {
                        throw new IllegalArgumentException("cannot handle more then one non-generated DMQ clause");
                    }
                    nonGeneratedClause = clause;
                }
            }
            if (nonGeneratedClause != null) {
                nonGeneratedClause.accept(this);
            }
        }
        return null;
        
    }

    @Override
    public Node visit(final Term term) {

        // don't handle generated terms
        if (!term.isGenerated()) {

            if (isReverseCompoundTriggerWord(term)) {
                this.reverseCompound = true;
                return term;
            }

            decompound(term);
            compound(term);

            previousTerms.add(term);
            if (previousTerms.size() > COMPOUND_WINDOW) {
                previousTerms.removeFirst();
            }


        } else {
            previousTerms.clear();
        }

        this.reverseCompound = alwaysAddReverseCompounds;

        return term;
    }

    private boolean isReverseCompoundTriggerWord(final Term term) {
        return reverseCompoundTriggerWords.get(term).getStateForCompleteSequence().isFinal();
    }

    protected void decompound(final Term term) {
        // determine the nodesToAdd based on the term
        try {

            for (final SuggestWord[] decompounded : suggestWordbreaks(term)) {

                if (decompounded != null && decompounded.length > 0) {

                    final BooleanQuery bq = new BooleanQuery(term.getParent(), Clause.Occur.SHOULD, true);

                    for (final SuggestWord word : decompounded) {
                        final DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(bq, Clause.Occur.MUST, true);
                        bq.addClause(dmq);
                        dmq.addClause(new Term(dmq, term.getField(), word.string, true));
                    }
                    nodesToAdd.add(bq);

                }

            }

        } catch (final IOException e) {
            // IO is broken, this looks serious -> throw as RTE
            throw new RuntimeException("Error decompounding " + term, e);
        }
    }

    protected void compound(final Term term) {

        if (!previousTerms.isEmpty()) {

            // calculate the compounds based on term and lookback window
            final Iterator<Term> previousTermsIterator = previousTerms.descendingIterator();

            final ArrayDeque<Term> compoundTerms = new ArrayDeque<>();

            while (previousTermsIterator.hasNext()) {
                final Term previousTerm = previousTermsIterator.next();
                if (eq(previousTerm.getField(), term.getField())) {
                    compoundTerms.addFirst(previousTerm);
                } else {
                    break;
                }
            }

            if (!compoundTerms.isEmpty()) {

                compoundTerms.add(term);

                try {
                    addCompounds(compoundTerms, false);
                    if (reverseCompound) {
                        addCompounds(compoundTerms, true);
                    }
                } catch (final IOException e) {
                    throw new RuntimeException("Error while compounding " + term, e);
                }


            }

        }
    }

    private void addCompounds(final ArrayDeque<Term> terms, final boolean reverse) throws IOException {

        final CombineSuggestion[] combinations = suggestCombination(reverse
                ? terms.descendingIterator() : terms.iterator());

        if (combinations != null && combinations.length > 0) {

            final Term[] termArray;
            if (reverse) {
                termArray = new Term[terms.size()];
                int i = terms.size() - 1;
                final Iterator<Term> termIterator = terms.descendingIterator();
                while (termIterator.hasNext()) {
                    termArray[i--] = termIterator.next();
                }
            } else {
                termArray = terms.toArray(new Term[0]);
            }

            for (final CombineSuggestion suggestion : combinations) {
                // add compound to each sibling that is part of the compound to maintain mm logic
                Arrays.stream(suggestion.originalTermIndexes)
                        .mapToObj(idx -> termArray[idx])
                        .forEach(sibling -> nodesToAdd.add(
                                new Term(sibling.getParent(), sibling.getField(), suggestion.suggestion.string, true)));

            }

        }

    }

    @Override
    public Node visit(final BooleanQuery bq) {
        previousTerms.clear();
        reverseCompound = false;
        return super.visit(bq);
    }

    protected SuggestWord[][] suggestWordbreaks(final Term term) throws IOException {
        return wordBreakSpellChecker.suggestWordBreaks(toLuceneTerm(term), MAX_EXPANSION, indexReader,
                SuggestMode.SUGGEST_ALWAYS,
                WordBreakSpellChecker.BreakSuggestionSortMethod.NUM_CHANGES_THEN_MAX_FREQUENCY);
    }

    protected CombineSuggestion[] suggestCombination(final Iterator<Term> terms) throws IOException {

        final List<org.apache.lucene.index.Term> luceneTerms = new ArrayList<>(COMPOUND_WINDOW);

        terms.forEachRemaining(term -> luceneTerms.add(toLuceneTerm(term)));

        return wordBreakSpellChecker.suggestWordCombinations(
                luceneTerms.toArray(new org.apache.lucene.index.Term[0]), 10, indexReader, SuggestMode.SUGGEST_ALWAYS);
    }

    private org.apache.lucene.index.Term toLuceneTerm(final Term querqyTerm) {
        return new org.apache.lucene.index.Term(dictionaryField, querqyTerm.getValue().toString());
    }

    private static <T> boolean eq(final T value1, final T value2) {
        return value1 == null && value2 == null || value1 != null && value1.equals(value2);
    }



}
