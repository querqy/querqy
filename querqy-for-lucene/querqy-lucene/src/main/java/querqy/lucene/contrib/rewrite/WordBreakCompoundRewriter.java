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
import java.util.NoSuchElementException;

public class WordBreakCompoundRewriter extends AbstractNodeVisitor<Node> implements QueryRewriter {

    private static final int COMPOUND_WINDOW = 3;
    private static final int MAX_EXPANSION = 5;

    private final WordBreakSpellChecker wordBreakSpellChecker;
    private final IndexReader indexReader;
    private final String dictionaryField;

    // We are not using this as a map but as a kind of a set to look up CharSequences quickly
    private final TrieMap<Boolean> reverseCompoundTriggerWords;

    private ArrayDeque<Term> previousTerms = null;
    private ArrayDeque<Term> termsToDelete = null;

    //
    private List<Node> nodesToAdd = null;

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
    }

    @Override
    public ExpandedQuery rewrite(final ExpandedQuery query) {
        final QuerqyQuery<?> userQuery = query.getUserQuery();
        if (userQuery instanceof Query){
            previousTerms = new ArrayDeque<>();
            termsToDelete = new ArrayDeque<>();
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

            termsToDelete.forEach(this::removeIfNotOnlyChild);

        }
        return query;
    }

    public void removeIfNotOnlyChild(final Term term) {
        // remove the term from its parent. If the parent doesn't have any further child,
        // remove the parent from the grand-parent. If this also hasn't any further child,
        // do not remove anything
        // TODO: go until top level?
        final DisjunctionMaxQuery parentQuery = term.getParent();
        if (parentQuery.getClauses().size() > 1) {
            parentQuery.removeClause(term);
        } else {
            final BooleanQuery grandParent = parentQuery.getParent();
            if (grandParent != null && grandParent.getClauses().size() > 1) {
                grandParent.removeClause(parentQuery);
            }
        }

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

            if (!isReverseCompoundTriggerWord(term)) {
                decompound(term);
                compound(term);
            } else {
                termsToDelete.add(term);
            }

            previousTerms.add(term);
        }

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
            boolean reverseCompound = false;

            // calculate the compounds based on term and the previous term,
            // also possibly including its predecessor if the term before was a "compound reversal" trigger
            final Iterator<Term> previousTermsIterator = new TermsFromFieldIterator(previousTerms.descendingIterator(), term.getField());

            Term previousTerm = null;
            while (previousTermsIterator.hasNext() && previousTerm == null) {
                Term maybePreviousTerm = previousTermsIterator.next();
                if (isReverseCompoundTriggerWord(maybePreviousTerm)) {
                    reverseCompound = true;
                } else {
                    previousTerm = maybePreviousTerm;
                }
            }

            if (previousTerm != null) {
                ArrayDeque<Term> compoundTerms = new ArrayDeque<>(2);
                compoundTerms.add(previousTerm);
                compoundTerms.add(term);

                try {
                    addCompounds(compoundTerms, false);
                    if (reverseCompound || alwaysAddReverseCompounds) {
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

    // Iterator wrapper that only iterates as long as it can emit terms from a given field
    private static class TermsFromFieldIterator implements Iterator<Term> {

        private final Iterator<Term> delegate;
        private final String field;

        private Term slot = null;

        public TermsFromFieldIterator(Iterator<Term> delegate, String field) {
            this.delegate = delegate;
            this.field = field;
        }

        @Override
        public boolean hasNext() {
            tryFillSlotIfEmpty();
            return slot != null && eq(slot.getField(), field);
        }

        @Override
        public Term next() {
            tryFillSlotIfEmpty();
            if (slot == null || !eq(slot.getField(), field)) {
                throw new NoSuchElementException("No more terms");
            } else {
                Term term = slot;
                slot = null;
                return term;
            }
        }

        private void tryFillSlotIfEmpty() {
            if (slot == null && delegate.hasNext()) {
                slot = delegate.next();
            }
        }
    }

}
