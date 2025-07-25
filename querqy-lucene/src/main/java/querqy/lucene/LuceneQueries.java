package querqy.lucene;

import org.apache.lucene.search.Query;

import java.util.List;
import java.util.Objects;

public class LuceneQueries {

    public final Query mainQuery;
    public final List<Query> filterQueries;
    public final Query userQuery;
    public final List<Query> querqyBoostQueries;
    public final Query rankQuery;
    public final boolean areQueriesInterdependent;
    public final boolean isMainQueryBoosted;


    public LuceneQueries(final Query mainQuery, final List<Query> filterQueries, final List<Query> querqyBoostQueries,
                         final Query userQuery, final Query rankQuery, final boolean areQueriesInterdependent,
                         final boolean isMainQueryBoosted) {
        this.mainQuery = Objects.requireNonNull(mainQuery);
        this.filterQueries = filterQueries;
        this.querqyBoostQueries = querqyBoostQueries;
        this.userQuery =  Objects.requireNonNull(userQuery);
        this.rankQuery = rankQuery;
        this.areQueriesInterdependent = areQueriesInterdependent;
        this.isMainQueryBoosted = isMainQueryBoosted;
    }
}
