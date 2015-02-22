/**
 * 
 */
package querqy.model;

/**
 * @author René Kriegler, @renekrie
 *
 */
public abstract class AbstractNodeVisitor<T> implements NodeVisitor<T> {
    
    @Override
    public T visit(Query query) {
        for (BooleanClause clause : query.getClauses()) {
            clause.accept(this);
        }
        return null;
    }

    @Override
    public T visit(DisjunctionMaxQuery disjunctionMaxQuery) {
        for (DisjunctionMaxClause clause : disjunctionMaxQuery.getClauses()) {
            clause.accept(this);
        }
        return null;
    }

    @Override
    public T visit(BooleanQuery booleanQuery) {
        for (BooleanClause clause : booleanQuery.getClauses()) {
            clause.accept(this);
        }
        return null;
    }

    @Override
    public T visit(Term term) {
        return null;
    }

    @Override
    public T visit(RawQuery rawQuery) {
        return null;
    }
   

}
