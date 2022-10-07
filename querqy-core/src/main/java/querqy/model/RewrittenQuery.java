package querqy.model;

import java.util.Collection;

public class RewrittenQuery {

    private final ExpandedQuery expandedQuery;

    public RewrittenQuery(final ExpandedQuery expandedQuery) {
        this.expandedQuery = expandedQuery;
    }

    public ExpandedQuery getExpandedQuery() {
        return expandedQuery;
    }

    public QuerqyQuery<?> getUserQuery() {
        return expandedQuery.getUserQuery();
    }

    public Collection<QuerqyQuery<?>> getFilterQueries() {
        return expandedQuery.getFilterQueries();
    }

    public Collection<BoostQuery> getBoostUpQueries() {
        return expandedQuery.getBoostUpQueries();
    }

    public Collection<BoostQuery> getMultiplicativeBoostQueries() {
        return expandedQuery.getMultiplicativeBoostQueries();
    }

    public Collection<BoostQuery> getBoostDownQueries() {
        return expandedQuery.getBoostDownQueries();
    }
}
