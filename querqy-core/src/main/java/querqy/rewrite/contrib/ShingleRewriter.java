package querqy.rewrite.contrib;

import querqy.CompoundCharSequence;
import querqy.model.*;
import querqy.rewrite.QueryRewriter;

import java.util.LinkedList;
import java.util.List;

/**
 * <p>A query rewriter that joins two adjacent query terms into a new term and adds this new term
 * to the query as a synonym to the two original terms. A query A B C thus becomes:</p>
 * <pre>
 (A OR AB) (B OR AB OR BC) (C OR BC)
 </pre>
 * <p>The resulting structure has the same number of clauses like the original query.<P>
 * 
 * @author muellenborn
 * @author René Kriegler, @renekrie
 */
public class ShingleRewriter extends AbstractNodeVisitor<Node> implements QueryRewriter {

    Term previousTerm = null;
    List<Term> termsToAdd = null;
    boolean acceptGeneratedTerms = false;

    public ShingleRewriter(){
        this(false);
    }

    public ShingleRewriter(boolean acceptGeneratedTerms) {
        this.acceptGeneratedTerms = acceptGeneratedTerms;
    }

    @Override
    public ExpandedQuery rewrite(ExpandedQuery query) {
        Query userQuery = query.getUserQuery();
        if (userQuery != null){
            previousTerm = null;
            termsToAdd = new LinkedList<>();
            visit(userQuery);
            for (Term term : termsToAdd) {
                term.getParent().addClause(term);
            }
        }
        return query;
    }

    @Override
    public Node visit(DisjunctionMaxQuery dmq) {
        List<DisjunctionMaxClause> clauses = dmq.getClauses();
        if (clauses != null && !clauses.isEmpty()){
            if (clauses.size() > 1) {
                throw new IllegalArgumentException("cannot handle more then one DMQ clause");
            }
            super.visit(dmq);
        }
        return null;
    }

    @Override
    public Node visit(Term term) {
        if (previousTerm != null
                && eq(previousTerm.getField(), term.getField())
                && (term.isGenerated() == acceptGeneratedTerms || !term.isGenerated())
                && (previousTerm.isGenerated() == acceptGeneratedTerms || !previousTerm.isGenerated())) {
            CharSequence seq = new CompoundCharSequence(null, previousTerm, term);
            termsToAdd.add(buildShingle(previousTerm, seq));
            termsToAdd.add(buildShingle(term, seq));
        }
        previousTerm = term;
        return term;
    }

    private static <T> boolean eq(T value1, T value2) {
        return value1 == null && value2 == null || value1 != null && value1.equals(value2);
    }

    private Term buildShingle(Term term, CharSequence seq) {

        return new Term(term.getParent(), term.getField(), seq, true);
    }

    @Override
    public Node visit(BooleanQuery bq) {
        previousTerm = null;
        return super.visit(bq);
    }
}
