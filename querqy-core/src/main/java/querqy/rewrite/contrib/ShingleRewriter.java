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
 (A OR (AB AND -A)) (B OR (AB AND -B) OR (BC AND -B)) (C OR (BC AND -C))
 </pre>
 * <p>The resulting structure has the same number of clauses like the original query and assures that 
 * additive term scoring algorithms will not favour documents that have the shingle term and one or 
 * both component terms over documents that only have the shingle term.<P>
 * 
 * @author muellenborn
 * @author René Kriegler, @renekrie
 */
public class ShingleRewriter extends AbstractNodeVisitor<Node> implements QueryRewriter {

    Term previousTerm = null;
    List<BooleanQuery> bqToAdd = null;

    @Override
    public ExpandedQuery rewrite(ExpandedQuery query) {
        Query userQuery = query.getUserQuery();
        if (userQuery != null){
            previousTerm = null;
            bqToAdd = new LinkedList<>();
            visit(userQuery);
            for (BooleanQuery bq : bqToAdd) {
                ((DisjunctionMaxQuery) bq.getParent()).addClause(bq);
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
        if (previousTerm != null && eq(previousTerm.getField(), term.getField())) {
            CharSequence seq = new CompoundCharSequence(null, previousTerm, term);

            BooleanQuery bqPrev = buildShingleWithExcludedTerm(previousTerm, seq);
            bqToAdd.add(bqPrev);

            BooleanQuery bq = buildShingleWithExcludedTerm(term, seq);
            bqToAdd.add(bq);
        }
        previousTerm = term;
        return term;
    }

    private static <T> boolean eq(T value1, T value2) {
        return value1 == null && value2 == null || value1 != null && value1.equals(value2);
    }

    private BooleanQuery buildShingleWithExcludedTerm(Term term, CharSequence seq) {
        BooleanQuery bq = new BooleanQuery(term.getParent(), Clause.Occur.SHOULD, true);

        DisjunctionMaxQuery dmqShingle =  new DisjunctionMaxQuery(bq, Clause.Occur.MUST, true);
        bq.addClause(dmqShingle);

        Term shingleTerm = new Term(dmqShingle, term.getField(), seq, true);
        dmqShingle.addClause(shingleTerm);

        DisjunctionMaxQuery dmqNeg = new DisjunctionMaxQuery(bq, Clause.Occur.MUST_NOT, true);
        bq.addClause(dmqNeg);

        Term negTerm = new Term(dmqNeg, term.getField(), term, true);
        dmqNeg.addClause(negTerm);

        return bq;
    }

    @Override
    public Node visit(BooleanQuery bq) {
        previousTerm = null;
        return super.visit(bq);
    }
}
