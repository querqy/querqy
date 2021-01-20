package querqy.lucene;

import querqy.model.ExpandedQuery;
import querqy.model.convert.builder.ExpandedQueryBuilder;
import java.util.Map;

public class JsonQueryParsingController extends QueryParsingController {

    private final Map request;

    public JsonQueryParsingController(final Map request, final LuceneSearchEngineRequestAdapter requestAdapter) {
        super(requestAdapter);

        this.request = request;
    }

    @Override
    public ExpandedQuery createExpandedQuery() {

        final ExpandedQueryBuilder expanded = new ExpandedQueryBuilder(request);
        return expanded.build();
    }
}
