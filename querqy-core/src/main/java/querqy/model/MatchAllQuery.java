package querqy.model;

import lombok.EqualsAndHashCode;

/**
 * A top-level {@link Query} matching all documents
 */
@EqualsAndHashCode(callSuper = true)
public class MatchAllQuery extends Clause<BooleanParent> implements QuerqyQuery<BooleanParent> {

    public MatchAllQuery() {
        this(null, Occur.SHOULD, false);
    }

    public MatchAllQuery(final boolean isGenerated) {
        this(null, Occur.SHOULD, isGenerated);
    }

    public MatchAllQuery(final BooleanParent parent, final Occur occur, final boolean isGenerated) {

        super(parent, occur, isGenerated);

    }

    @Override
    public <T> T accept(final NodeVisitor<T> visitor) {
        return visitor.visit(this);
    }


    @Override
    public QuerqyQuery<BooleanParent> clone(final BooleanParent newParent) {
        return new MatchAllQuery(newParent, getOccur(), isGenerated());
    }

    @Override
    public QuerqyQuery<BooleanParent> clone(final BooleanParent newParent, boolean generated) {
        return new MatchAllQuery(newParent, getOccur(), generated);
    }
}
