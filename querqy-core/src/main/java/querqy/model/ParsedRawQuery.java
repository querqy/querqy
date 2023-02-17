package querqy.model;

public abstract class ParsedRawQuery<T> extends RawQuery {

    protected final T query;

    public ParsedRawQuery(final BooleanParent parent, final Occur occur, final boolean isGenerated, final T query) {
        super(parent, occur, isGenerated);
        this.query = query;
    }

    public T getQuery() {
        return query;
    }
}
