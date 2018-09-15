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
    public T visit(final Query query) {
        for (final BooleanClause clause : query.getClauses()) {
            clause.accept(this);
        }
        return null;
    }

    @Override
    public T visit(final MatchAllQuery query) {
        return null;
    }

    @Override
    public T visit(final DisjunctionMaxQuery disjunctionMaxQuery) {
        for (final DisjunctionMaxClause clause : disjunctionMaxQuery.getClauses()) {
            clause.accept(this);
        }
        return null;
    }

    @Override
    public T visit(final BooleanQuery booleanQuery) {
        for (final BooleanClause clause : booleanQuery.getClauses()) {
            clause.accept(this);
        }
        return null;
    }

    @Override
    public T visit(final Term term) {
        return null;
    }

    @Override
    public T visit(final RawQuery rawQuery) {
        return null;
    }
   

}
