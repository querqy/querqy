/**
 *
 */
package querqy.model;

/**
 * The top-level query as entered by the user.
 *
 * @author René Kriegler, @renekrie
 */
public class Query extends BooleanQuery {

    public Query() {
        super(null, Occur.SHOULD, false);
    }

    public Query(final boolean generated) {
        super(null, Occur.SHOULD, generated);
    }

    @Override
    public Query clone(final BooleanParent newParent, final Occur occur, final boolean generated) {
        final Query q = new Query(generated);
        for (final BooleanClause clause : clauses) {
            q.addClause(clause.clone(q, generated));
        }
        return q;

    }

}
