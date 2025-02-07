package querqy.rewrite.contrib;

import querqy.CompoundCharSequence;
import querqy.model.*;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterOutput;
import querqy.rewrite.SearchEngineRequestAdapter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

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
public class NumberConcatenationRewriter extends AbstractNodeVisitor<Node> implements QueryRewriter {

    Term previousTerm = null;
    List<Term> termsToAdd = null;
    List<Term> numberTermsToAdd = new ArrayList<>();
    int numberOfClauses = 0;
    // We use clauseCount to keep track of the query clauses we have already looked at.
    // When we have reached numberOfClauses we can process the collected numerical terms accordingly.
    int clauseCount = 1;

    final boolean acceptGeneratedTerms;
    final int minimumLengthOfResultingQueryTerm;

    public NumberConcatenationRewriter(final boolean acceptGeneratedTerms, final int minimumLengthOfResultingQueryTerm) {
        this.acceptGeneratedTerms = acceptGeneratedTerms;
        this.minimumLengthOfResultingQueryTerm = minimumLengthOfResultingQueryTerm;
    }

    @Override
    public RewriterOutput rewrite(final ExpandedQuery query, final SearchEngineRequestAdapter requestAdapter) {
        final QuerqyQuery<?> userQuery = query.getUserQuery();
        if (userQuery instanceof Query){
            previousTerm = null;
            termsToAdd = new LinkedList<>();
            numberOfClauses = ((Query) userQuery).getClauses().size();
            clauseCount = 1;
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
                && Objects.equals(previousTerm.getField(), term.getField())
                && (term.isGenerated() == acceptGeneratedTerms || !term.isGenerated())
                && (previousTerm.isGenerated() == acceptGeneratedTerms || !previousTerm.isGenerated())) {
            //if previousTerm and term are digits-only add them to a list
            if (isDigit(previousTerm) && isDigit(term) && (previousTerm.length() + term.length()) >= minimumLengthOfResultingQueryTerm) {
                if (numberTermsToAdd.isEmpty()) {
                    numberTermsToAdd.add(previousTerm);
                }
                numberTermsToAdd.add(term);

                //When switching from digits-only to any other type of term concatenate all collected digit-only terms
            } else if (!numberTermsToAdd.isEmpty()) {
                processTerms(numberTermsToAdd);
            }
            //When we are looking at the last term (clauseCount==numberOfClauses) and we have something to process (!numberTermsToAdd.isEmpty())
            //we process the numeric terms. Otherwise we wouldn't do anything with numeric terms at the end of a query.
            if (clauseCount==numberOfClauses && !numberTermsToAdd.isEmpty()) {
                processTerms(numberTermsToAdd);
            }
        }
        previousTerm = term;
        clauseCount++;
        return term;
    }

    private void processTerms(final List<Term> numberTermsToAdd) {
        final CharSequence seq = new CompoundCharSequence(null, numberTermsToAdd);
        for (Term numberTerm : numberTermsToAdd) {
            termsToAdd.add(new Term(numberTerm.getParent(), numberTerm.getField(), seq, true));
        }
        numberTermsToAdd.clear();
    }

    final boolean isDigit(final Term term) {
        final int termLength = term.length();
        if (termLength < 1) return false; //for cases where there is an empty query term
        for (int pos = 0; pos < termLength; pos++) {
            if (!Character.isDigit(term.charAt(pos))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Node visit(final BooleanQuery bq) {
        previousTerm = null;
        return super.visit(bq);
    }
}
