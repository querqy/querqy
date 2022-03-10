package querqy.solr;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SyntaxError;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class QuerqyRelaxFilterQueryComponent extends QuerqyQueryComponent {

    String relaxFilterQueryString = null;

    @Override
    public void init(NamedList args) {
        relaxFilterQueryString = Optional.ofNullable(args.get("fq")).map(String::valueOf).orElse(null);
    }

    @Override
    public void prepare(final ResponseBuilder rb) throws IOException {
        super.prepare(rb);
        Query relaxQuery = getRelaxFilterQuery(rb);
        if (relaxQuery == null) {
            return;
        }
        QParser parser = rb.getQparser();
        if (parser instanceof QuerqyDismaxQParser) {
            List<Query> filterQueries = ((QuerqyDismaxQParser) parser).getFilterQueries();
            if ((filterQueries != null) && !filterQueries.isEmpty()) {
                // replace all filterqueries added by this parser
                int startQuerqyFqs = rb.getFilters().indexOf(filterQueries.get(0));
                List<Query> updatedFilters = new LinkedList<>(rb.getFilters().subList(0, startQuerqyFqs));
                for (int i = startQuerqyFqs; i < rb.getFilters().size(); i++) {
                    updatedFilters.add(combineFilterQueries(rb.getFilters().get(i), relaxQuery));
                }
                rb.setFilters(updatedFilters);
            }
        }
    }

    private Query getRelaxFilterQuery(final ResponseBuilder rb) {
        if (relaxFilterQueryString == null || relaxFilterQueryString.trim().isEmpty()) {
            return null;
        }
        ModifiableSolrParams solrParams = new ModifiableSolrParams();
        solrParams.add("q", relaxFilterQueryString);
        SolrQueryRequest req = new LocalSolrQueryRequest(rb.req.getCore(), solrParams);
        Query query;
        try {
            query = QParser.getParser(relaxFilterQueryString, req).getQuery();
        } catch (SyntaxError e) {
            throw new IllegalArgumentException(e);
        }
        return query;
    }

    private Query combineFilterQueries(Query query1, Query query2) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(query1, BooleanClause.Occur.SHOULD);
        builder.add(query2, BooleanClause.Occur.SHOULD);
        return builder.build();
    }

}
