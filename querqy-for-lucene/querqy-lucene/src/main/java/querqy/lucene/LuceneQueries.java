package querqy.lucene;

import org.apache.lucene.search.Query;

import java.util.List;
import java.util.Objects;

public class LuceneQueries {

    public final Query mainQuery;
    public final List<Query> filterQueries;
    public final Query userQuery;
    public final List<Query> querqyBoostQueries;
    public final boolean areQueriesInterdependent;


    public LuceneQueries(final Query mainQuery, final List<Query> filterQueries, final List<Query> querqyBoostQueries,
                         final Query userQuery, final boolean areQueriesInterdependent) {
        this.mainQuery = Objects.requireNonNull(mainQuery);
        this.filterQueries = filterQueries;
        this.querqyBoostQueries = querqyBoostQueries;
        this.userQuery =  Objects.requireNonNull(userQuery);
        this.areQueriesInterdependent = areQueriesInterdependent;
    }

    public LuceneQueries(final Query mainQuery, final List<Query> filterQueries, final Query userQuery,
                         final boolean areQueriesInterdependent) {
        this(mainQuery, filterQueries, null, userQuery, areQueriesInterdependent);
    }
}
