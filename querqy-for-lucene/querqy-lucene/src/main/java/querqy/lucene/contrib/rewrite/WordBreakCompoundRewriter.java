package querqy.lucene.contrib.rewrite;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.spell.CombineSuggestion;
import org.apache.lucene.search.spell.SuggestMode;
import org.apache.lucene.search.spell.SuggestWord;
import org.apache.lucene.search.spell.WordBreakSpellChecker;
import querqy.model.AbstractNodeVisitor;
import querqy.model.BooleanParent;
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

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class WordBreakCompoundRewriter extends AbstractNodeVisitor<Node> implements QueryRewriter {

    private static final int COMPOUND_WINDOW = 3;
    private static final int MAX_EXPANSION = 5;

    private final WordBreakSpellChecker wordBreakSpellChecker;
    private final IndexReader indexReader;
    private final String dictionaryField;

    private final Set<String> compoundReversalTerms = new HashSet<>(Arrays.asList("aus", "für"));

    private ArrayDeque<Term> previousTerms = null;

    //
    private List<BooleanQuery> decompounds = null;
    private List<Term> compoundsToAdd = null;

    private boolean reverseCompound = false;

    public WordBreakCompoundRewriter(final WordBreakSpellChecker wordBreakSpellChecker,
                                     final IndexReader indexReader,
                                     final String dictionaryField) {
        this.wordBreakSpellChecker = wordBreakSpellChecker;
        this.indexReader = indexReader;
        this.dictionaryField = dictionaryField;
    }

    @Override
    public ExpandedQuery rewrite(final ExpandedQuery query) {
        final QuerqyQuery<?> userQuery = query.getUserQuery();
        if (userQuery instanceof Query){
            previousTerms = new ArrayDeque<>(COMPOUND_WINDOW);
            decompounds = new LinkedList<>();
            compoundsToAdd = new LinkedList<>();
            visit((Query) userQuery);

            // append decompounds to parent query
            decompounds.forEach(booleanQuery -> {
                final BooleanParent parent = booleanQuery.getParent();
                // TODO: extend BooleanParent? interface so that we don't need this cast?
                if (parent instanceof DisjunctionMaxQuery) {
                    ((DisjunctionMaxQuery) parent).addClause(booleanQuery);
                } else if (parent instanceof BooleanQuery) {
                    ((BooleanQuery) parent).addClause(booleanQuery);
                } else {
                    throw new IllegalStateException("Unknown parent type " + parent.getClass().getName());
                }

            });

            // compounds are just added
            for (Term term : compoundsToAdd) {
                term.getParent().addClause(term);
            }
        }
        return query;
    }

    @Override
    public Node visit(final DisjunctionMaxQuery dmq) {
        final List<DisjunctionMaxClause> clauses = dmq.getClauses();
        if (clauses != null && !clauses.isEmpty()) {
            DisjunctionMaxClause nonGeneratedClause = null;
            for (DisjunctionMaxClause clause: clauses) {
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

        // for now, don't handle generated terms
        if (!term.isGenerated()) {

            // determine the decompounds based on the term
            try {

                for (final SuggestWord[] decompounded : suggestWordbreaks(term)) {

                    final BooleanQuery bq = new BooleanQuery(term.getParent(), Clause.Occur.SHOULD, true);

                    for (final SuggestWord word : decompounded) {
                        final DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(bq, Clause.Occur.MUST, true);
                        bq.addClause(dmq);
                        dmq.addClause(new Term(dmq, term.getField(), word.string, true));
                    }
                    decompounds.add(bq);
                }

            } catch (final IOException e) {
                // IO is broken, this looks serious -> throw as RTE
                throw new RuntimeException("Error decompounding " + term, e);
            }

        }

      /*      // the "compound reversal" terms ("für", "aus") just flag and are not processed
            if (compoundReversalTerms.contains(term.getValue().toString())) {
                reverseCompound = true;
            } else {
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
                compoundTerms.add(term);

             /  try {
                    CombineSuggestion[] combinations = suggestCombination(compoundTerms.iterator());
                    for (CombineSuggestion suggestion: combinations) {
                        compoundsToAdd.add(new Term(term.getParent(), term.getField(), suggestion.suggestion.string, true));
                    }
                    if (reverseCompound) {
                        combinations = suggestCombination(compoundTerms.descendingIterator());
                        for (CombineSuggestion suggestion: combinations) {
                            compoundsToAdd.add(new Term(term.getParent(), term.getField(), suggestion.suggestion.string, true));
                        }
                    }
                } catch (IOException e) {
                    // todo: logging
                }
                previousTerms.add(term);
                if (previousTerms.size() > COMPOUND_WINDOW) {
                    previousTerms.removeFirst();

            }
        }}*/
        return term;
    }

    @Override
    public Node visit(final BooleanQuery bq) {
        previousTerms = new ArrayDeque<>();
        reverseCompound = false;
        return super.visit(bq);
    }

    protected SuggestWord[][] suggestWordbreaks(Term term) throws IOException {
        return wordBreakSpellChecker.suggestWordBreaks(lookupTerm(term), MAX_EXPANSION, indexReader,
                SuggestMode.SUGGEST_ALWAYS, WordBreakSpellChecker.BreakSuggestionSortMethod.NUM_CHANGES_THEN_MAX_FREQUENCY);
    }

    protected CombineSuggestion[] suggestCombination(Iterator<Term> terms) throws IOException {
        List<org.apache.lucene.index.Term> luceneTerms = new ArrayList<>(COMPOUND_WINDOW);
        terms.forEachRemaining(term -> luceneTerms.add(lookupTerm(term)));
        return wordBreakSpellChecker.suggestWordCombinations(luceneTerms.toArray(new org.apache.lucene.index.Term[0]),
                10, indexReader, SuggestMode.SUGGEST_ALWAYS);
    }

    private org.apache.lucene.index.Term lookupTerm(Term querqyTerm) {
        return new org.apache.lucene.index.Term(dictionaryField, querqyTerm.getValue().toString());
    }

    private static <T> boolean eq(final T value1, final T value2) {
        return value1 == null && value2 == null || value1 != null && value1.equals(value2);
    }



}
