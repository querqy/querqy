package querqy.rewrite.contrib;

import querqy.CompoundCharSequence;
import querqy.model.*;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.SearchEngineRequestAdapter;

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
    final boolean acceptGeneratedTerms;

    public ShingleRewriter(){
        this(false);
    }

    public ShingleRewriter(final boolean acceptGeneratedTerms) {
        this.acceptGeneratedTerms = acceptGeneratedTerms;
    }

    @Override
    public RewritingOutput rewrite(final ExpandedQuery query, final SearchEngineRequestAdapter requestAdapter) {
        final QuerqyQuery<?> userQuery = query.getUserQuery();
        if (userQuery != null && userQuery instanceof Query){
            previousTerm = null;
            termsToAdd = new LinkedList<>();
            visit((Query) userQuery);
            for (Term term : termsToAdd) {
                term.getParent().addClause(term);
            }
        }
        return new RewritingOutput(query);
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
            final CharSequence seq = new CompoundCharSequence(null, previousTerm, term);
            termsToAdd.add(buildShingle(previousTerm, seq));
            termsToAdd.add(buildShingle(term, seq));
        }
        previousTerm = term;
        return term;
    }

    private static <T> boolean eq(final T value1, final T value2) {
        return value1 == null && value2 == null || value1 != null && value1.equals(value2);
    }

    private Term buildShingle(final Term term, final CharSequence seq) {

        return new Term(term.getParent(), term.getField(), seq, true);
    }

    @Override
    public Node visit(final BooleanQuery bq) {
        previousTerm = null;
        return super.visit(bq);
    }
}
