package querqy.lucene.contrib.rewrite.wordbreak;

import org.apache.lucene.index.IndexReader;
import querqy.LowerCaseCharSequence;
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
import querqy.model.RewrittenQuery;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.trie.TrieMap;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

public class WordBreakCompoundRewriter extends AbstractNodeVisitor<Node> implements QueryRewriter {

    private final LuceneWordBreaker wordBreaker;
    private final LuceneCompounder compounder;
    private final IndexReader indexReader;
    private final boolean lowerCaseInput;

    // We are not using this as a map but as a kind of a set to look up CharSequences quickly
    private final TrieMap<Boolean> reverseCompoundTriggerWords;

    private ArrayDeque<Term> previousTerms = null;
    private ArrayDeque<Term> termsToDelete = null;

    //
    private List<Node> nodesToAdd = null;

    private final boolean alwaysAddReverseCompounds;

    private final int maxDecompoundExpansions;
    private final boolean verifyDecompoundCollation;

    private final TrieMap<Boolean> protectedWords;

    /**
     * @param wordBreaker The word breaker to use
     * @param compounder The compounder to use
     * @param indexReader The index reader
     * @param lowerCaseInput Iff true, lowercase input before matching it against the dictionary field.
     * @param alwaysAddReverseCompounds Iff true, reverse shingles will be added to the query
     * @param reverseCompoundTriggerWords Query tokens found as keys in this map will trigger the creation of a reverse compound of the surrounding tokens.
     * @param maxDecompoundExpansions The maximum number of decompounds to add to the query
     * @param verifyDecompoundCollation Iff true, verify that all parts of the compound cooccur in dictionaryField after decompounding
     * @param protectedWords The "false-positive" set of terms that should never be split or be result of a combination
     */
    public WordBreakCompoundRewriter(final LuceneWordBreaker wordBreaker, final LuceneCompounder compounder,
                                     final IndexReader indexReader,
                                     final boolean lowerCaseInput, final boolean alwaysAddReverseCompounds,
                                     final TrieMap<Boolean> reverseCompoundTriggerWords,
                                     final int maxDecompoundExpansions, final boolean verifyDecompoundCollation,
                                     final TrieMap<Boolean> protectedWords) {

        if (reverseCompoundTriggerWords == null) {
            throw new IllegalArgumentException("reverseCompoundTriggerWords must not be null");
        }

        this.wordBreaker = wordBreaker;
        this.compounder = compounder;

        this.alwaysAddReverseCompounds = alwaysAddReverseCompounds;
        this.reverseCompoundTriggerWords = reverseCompoundTriggerWords;
        this.maxDecompoundExpansions = maxDecompoundExpansions;
        this.verifyDecompoundCollation = verifyDecompoundCollation;
        this.indexReader = indexReader;
        this.lowerCaseInput = lowerCaseInput;
        this.protectedWords = protectedWords;
    }

    @Override
    public RewrittenQuery rewrite(final ExpandedQuery query) {
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
        return new RewrittenQuery(query);
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

            if (isReverseCompoundTriggerWord(term.getValue())) {
                termsToDelete.add(term);
            } else {
                if (!isProtectedWord(term.getValue())) {
                    decompound(term);
                }
                compound(term);
            }

            previousTerms.add(term);
        }

        return term;
    }

    protected void decompound(final Term term) {

        // determine the nodesToAdd based on the term
        try {

            for (final CharSequence[] decompounded : wordBreaker.breakWord(term, indexReader, maxDecompoundExpansions,
                    verifyDecompoundCollation)) {

                if (decompounded != null && decompounded.length > 0) {

                    final BooleanQuery bq = new BooleanQuery(term.getParent(), Clause.Occur.SHOULD, true);

                    for (final CharSequence word : decompounded) {
                        final DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(bq, Clause.Occur.MUST, true);
                        bq.addClause(dmq);
                        dmq.addClause(new Term(dmq, term.getField(), word, true));
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
            final Iterator<Term> previousTermsIterator = new TermsFromFieldIterator(previousTerms.descendingIterator(),
                    term.getField());

            Term previousTerm = null;
            while (previousTermsIterator.hasNext() && previousTerm == null) {
                final Term maybePreviousTerm = previousTermsIterator.next();
                if (isReverseCompoundTriggerWord(maybePreviousTerm.getValue())) {
                    reverseCompound = true;
                } else {
                    previousTerm = maybePreviousTerm;
                }
            }

            if (previousTerm != null) {

                final Term[] compoundTerms = new Term[] {previousTerm, term};

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

    private void addCompounds(final Term[] terms, final boolean reverse) throws IOException {

        for (final LuceneCompounder.CompoundTerm compoundTerm : compounder.combine(terms, indexReader, reverse)) {
            if (!isProtectedWord(compoundTerm.value)) {
                for (final Term sibling: compoundTerm.originalTerms) {
                    nodesToAdd.add(new Term(sibling.getParent(), sibling.getField(), compoundTerm.value, true));
                }
            }
        }

    }

    private boolean isReverseCompoundTriggerWord(final CharSequence chars) {
        return reverseCompoundTriggerWords.get(lowerCaseInput ? new LowerCaseCharSequence(chars) : chars)
                .getStateForCompleteSequence().isFinal();
    }

    private boolean isProtectedWord(final CharSequence chars) {
        return protectedWords.get(lowerCaseInput ? new LowerCaseCharSequence(chars) : chars)
                .getStateForCompleteSequence().isFinal();
    }

    @Override
    public Node visit(final BooleanQuery bq) {
        previousTerms.clear();
        return super.visit(bq);
    }

    public static class MaxSortable<T> implements Comparable<MaxSortable<T>> {
        public final T obj;
        public final int count;

        public MaxSortable(final T obj, final int count) {
            this.obj = obj;
            this.count = count;
        }

        @Override
        public int compareTo(final MaxSortable<T> o) {
            // reverse order
            return Integer.compare(o.count, this.count);
        }
    }

    // Iterator wrapper that only iterates as long as it can emit terms from a given field
    private static class TermsFromFieldIterator implements Iterator<Term> {

        private final Iterator<Term> delegate;
        private final String field;

        private Term slot = null;

        public TermsFromFieldIterator(final Iterator<Term> delegate, final String field) {
            this.delegate = delegate;
            this.field = field;
        }

        @Override
        public boolean hasNext() {
            return tryFillSlotIfEmpty() && Objects.equals(slot.getField(), field);
        }

        @Override
        public Term next() {
            tryFillSlotIfEmpty();
            if (slot == null || !Objects.equals(slot.getField(), field)) {
                throw new NoSuchElementException("No more terms");
            } else {
                Term term = slot;
                slot = null;
                return term;
            }
        }

        private boolean tryFillSlotIfEmpty() {
            if (slot == null && delegate.hasNext()) {
                slot = delegate.next();
                return true;
            } else {
                return false;
            }
        }
    }

}
