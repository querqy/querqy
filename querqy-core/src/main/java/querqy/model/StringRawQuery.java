package querqy.model;

public class StringRawQuery extends RawQuery {
    private final String queryString;

    public StringRawQuery(final BooleanParent parent, final String queryString, final Occur occur, final boolean isGenerated) {
        super(parent, occur, isGenerated);
        this.queryString = queryString;
    }

    @Override
    public RawQuery clone(final BooleanParent newParent) {
        return clone(newParent, this.generated);
    }

    @Override
    public RawQuery clone(final BooleanParent newParent, final boolean generated) {
        return new StringRawQuery(newParent, queryString, occur, generated);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((queryString == null) ? 0 : queryString.hashCode());
        result = prime * result
                + ((occur == null) ? 0 : occur.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        StringRawQuery other = (StringRawQuery) obj;
        if (queryString == null) {
            if (other.queryString != null)
                return false;
        } else if (!queryString.equals(other.queryString))
            return false;

        return occur == other.occur;
    }

    @Override
    public String toString() {
        return "RawQuery [queryString=" + queryString + "]";
    }

    public String getQueryString() {
        return queryString;
    }

}
