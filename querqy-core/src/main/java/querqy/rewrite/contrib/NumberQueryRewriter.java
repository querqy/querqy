package querqy.rewrite.contrib;

import querqy.CompoundCharSequence;
import querqy.model.*;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterOutput;
import querqy.rewrite.SearchEngineRequestAdapter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>A query rewriter that joins two or more adjacent numeric query terms into a new term
 * and adds this new term to the query as a synonym to the original terms.
 * A query 01 23 45 thus becomes:</p>
 * <pre>
 (01 OR 012345) (23 OR 012345) (45 OR 012345)
 </pre>
 *
 * @author Daniel Wrigley
 */
public class NumberQueryRewriter extends AbstractNodeVisitor<Node> implements QueryRewriter {

    Term previousTerm = null;
    List<Term> termsToAdd = null;
    List<Term> numberTermsToAdd = new ArrayList<>();
    int numberOfClauses = 0;
    int i = 1;

    final boolean acceptGeneratedTerms;

    public NumberQueryRewriter(){
        this(false);
    }

    public NumberQueryRewriter(final boolean acceptGeneratedTerms) {
        this.acceptGeneratedTerms = acceptGeneratedTerms;
    }

    @Override
    public RewriterOutput rewrite(final ExpandedQuery query, final SearchEngineRequestAdapter requestAdapter) {
        final QuerqyQuery<?> userQuery = query.getUserQuery();
        if (userQuery != null && userQuery instanceof Query){
            previousTerm = null;
            termsToAdd = new LinkedList<>();
            numberOfClauses = ((Query) userQuery).getClauses().size();
            i = 1;
            visit((Query) userQuery);
            for (Term term : termsToAdd) {
                term.getParent().addClause(term);
            }
        }
        return RewriterOutput.builder().expandedQuery(query).build();
    }

    @Override
    public Node visit(final DisjunctionMaxQuery dmq) {

        final List<DisjunctionMaxClause> clauses = dmq.getClauses();

        if (clauses != null) {

            switch (clauses.size()) {

                case 0: break;

                case 1: super.visit(dmq); break;

                default:

                    if (acceptGeneratedTerms) {

                        throw new IllegalArgumentException("cannot handle more than one DMQ clause");

                    } else {

                        DisjunctionMaxClause nonGeneratedClause = null;

                        for (DisjunctionMaxClause clause: clauses) {

                            if (!clause.isGenerated()) {
                                // second non-generated clause - cannot handle this
                                if (nonGeneratedClause != null) {
                                    throw new IllegalArgumentException("cannot handle more than one non-generated DMQ clause");
                                }
                                nonGeneratedClause = clause;
                            }
                        }

                        if (nonGeneratedClause != null) {
                            nonGeneratedClause.accept(this);
                        }
                    }

            }


        }

        return null;

    }

    @Override
    public Node visit(final Term term) {
        if (previousTerm != null
                && eq(previousTerm.getField(), term.getField())
                && (term.isGenerated() == acceptGeneratedTerms || !term.isGenerated())
                && (previousTerm.isGenerated() == acceptGeneratedTerms || !previousTerm.isGenerated())) {
            //if previousTerm and term are digits-only add them to a list
            if (previousTerm.toString().matches("\\d+") && term.toString().matches("\\d+") && previousTerm.toString().concat(term.toString()).length() > 2) {
                if (numberTermsToAdd.isEmpty()) {
                    numberTermsToAdd.add(previousTerm);
                }
                numberTermsToAdd.add(term);

                //When switching from digits-only to any other type of term concatenate all collected digit-only terms
            } else if (!numberTermsToAdd.isEmpty()) {
                processTerms(numberTermsToAdd);
            }
            //When we are looking at the last term (i==numberOfClauses) and we have something to process (!numberTermsToAdd.isEmpty())
            //we process the numeric terms. Otherwise we wouldn't do anything with numeric terms at the end of a query.
            if (i==numberOfClauses && !numberTermsToAdd.isEmpty()) {
                processTerms(numberTermsToAdd);
            }
        }
        previousTerm = term;
        i++;
        return term;
    }

    private void processTerms(List<Term> numberTermsToAdd) {
        final CharSequence seq = new CompoundCharSequence(null, numberTermsToAdd);
        for (Term numberTerm : numberTermsToAdd) {
            termsToAdd.add(new Term(numberTerm.getParent(), numberTerm.getField(), seq, true));
        }
        numberTermsToAdd.clear();
    }

    private static <T> boolean eq(final T value1, final T value2) {
        return value1 == null && value2 == null || value1 != null && value1.equals(value2);
    }

    @Override
    public Node visit(final BooleanQuery bq) {
        previousTerm = null;
        return super.visit(bq);
    }
}
