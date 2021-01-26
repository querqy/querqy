package querqy.solr;

import org.apache.solr.EmbeddedSolrServerTestBase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.json.JsonQueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;
import querqy.model.convert.builder.BooleanQueryBuilder;
import querqy.model.convert.builder.ExpandedQueryBuilder;
import querqy.model.convert.converter.MapConverterConfig;
import querqy.rewrite.experimental.QueryRewritingHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static querqy.model.convert.builder.BooleanQueryBuilder.bq;
import static querqy.model.convert.builder.BoostQueryBuilder.boost;
import static querqy.model.convert.builder.DisjunctionMaxQueryBuilder.dmq;
import static querqy.model.convert.builder.ExpandedQueryBuilder.expanded;
import static querqy.model.convert.builder.MatchAllQueryBuilder.matchall;
import static querqy.model.convert.builder.StringRawQueryBuilder.raw;
import static querqy.model.convert.builder.TermBuilder.term;
import static querqy.model.convert.model.Occur.MUST;

@SolrTestCaseJ4.SuppressSSL
public class QuerqyJsonQParserTest extends EmbeddedSolrServerTestBase {


    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig-external-rewriting.xml", "schema.xml");
        addDocs();
    }

    private static void addDocs() {
        assertU(adoc("id", "0", "f1", "tv", "f2", "television"));
        assertU(adoc("id", "1", "f1", "tv"));
        assertU(adoc("id", "2", "f1", "tv", "f2", "led"));
        assertU(adoc("id", "11", "f1", "led tv", "f2", "television"));
        assertU(adoc("id", "20", "f1", "television", "f2", "schwarz"));
        assertU(adoc("id", "21", "f1", "blau", "f2", "television"));

        assertU(commit());
    }

    @Test
    public void testQueryRewritingHandler() throws IOException, SolrServerException {
        final ExpandedQueryBuilder expanded = QueryRewritingHandler.builder()
                .addCommonRulesRewriter("tv => \n SYNONYM: television")
                .build()
                .rewriteQuery("tv")
                .getQuery();

        final JsonQueryRequest jsonQuery = new JsonQueryRequest()
                .setQuery(createRequestToTestMatching(expanded));

        final QueryResponse response = jsonQuery.process(getSolrClient(), "collection1");

        Assertions.assertThat(response.getResults()).hasSize(6);
    }

    @Test
    public void testScoringOfFieldWeightsWithDownBoost() throws IOException, SolrServerException {
        ExpandedQueryBuilder expanded = expanded(
                bq(dmq("tv")),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList(boost(bq("television"), 5.0f))
        );

        final ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("fl", "id,score");
        params.add("fq", "id:(2 OR 11)");
        params.add("debugQuery", "true");

        final JsonQueryRequest jsonQuery = new JsonQueryRequest(params)
                .setQuery(createRequestToTestScoring(expanded));

        final QueryResponse response = jsonQuery.process(getSolrClient(), "collection1");

        final List<Map<String, Object>> results = response.getResults().stream().map(HashMap::new).collect(Collectors.toList());

        Assertions.assertThat(results).contains(
                doc("2", 45.0f)
        );
    }

    @Test
    public void testScoringOfFieldWeightsWithRawQueryUpBoost() throws IOException, SolrServerException {
        ExpandedQueryBuilder expanded = expanded(
                bq(dmq("tv", "television")),
                boost(raw("{!func}if(query({!lucene df=f1 v=blau}),100,0)"), 1.0f)
        );

        final ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("fl", "score");
        params.add("fq", "id:21");

        final JsonQueryRequest jsonQuery = new JsonQueryRequest(params)
                .setQuery(createRequestToTestScoring(expanded));

        final QueryResponse response = jsonQuery.process(getSolrClient(), "collection1");

        final List<Map<String, Object>> results = response.getResults().stream().map(HashMap::new).collect(Collectors.toList());

        Assertions.assertThat((Float) results.get(0).get("score")).isEqualTo(110.0f);
    }

    @Test
    public void testScoringOfFieldWeightsWithSimpleBoost() throws IOException, SolrServerException {
        ExpandedQueryBuilder expanded = expanded(
                bq(dmq("tv", "television")),
                boost(bq("blau"), 100.0f)
        );

        final ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("fl", "score");
        params.add("fq", "id:21");

        final JsonQueryRequest jsonQuery = new JsonQueryRequest(params)
                .setQuery(createRequestToTestScoring(expanded));

        final QueryResponse response = jsonQuery.process(getSolrClient(), "collection1");

        final List<Map<String, Object>> results = response.getResults().stream().map(HashMap::new).collect(Collectors.toList());

        Assertions.assertThat((Float) results.get(0).get("score")).isGreaterThan(10.0f);
    }

    @Test
    public void testScoringOfFieldWeights() throws IOException, SolrServerException {
        ExpandedQueryBuilder expanded = expanded(bq(dmq("tv", "television")));

        final ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("fl", "id,score");
        params.add("fq", "id:(0 OR 21)");

        final JsonQueryRequest jsonQuery = new JsonQueryRequest(params)
                .setQuery(createRequestToTestScoring(expanded));

        final QueryResponse response = jsonQuery.process(getSolrClient(), "collection1");

        final List<Map<String, Object>> results = response.getResults().stream().map(HashMap::new).collect(Collectors.toList());

        Assertions.assertThat(results).containsExactlyInAnyOrder(
                doc("0", 40.0f), doc("21", 10.0f)
        );
    }

    @Test
    public void testMatchAllQuery() throws IOException, SolrServerException {
        ExpandedQueryBuilder expanded = expanded(matchall());

        final ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("fl", "*,score");

        final JsonQueryRequest jsonQuery = new JsonQueryRequest(params)
                .setQuery(createRequestToTestMatching(expanded));

        final QueryResponse response = jsonQuery.process(getSolrClient(), "collection1");

        Assertions.assertThat(response.getResults()).hasSize(6);
    }

    @Test
    public void testMatchingOfSimpleQuery() throws IOException, SolrServerException {
        ExpandedQueryBuilder expanded = expanded(bq("tv"));

        final ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("fl", "*,score");

        final JsonQueryRequest jsonQuery = new JsonQueryRequest(params)
                .setQuery(createRequestToTestMatching(expanded));

        final QueryResponse response = jsonQuery.process(getSolrClient(), "collection1");

        Assertions.assertThat(response.getResults()).hasSize(4);
    }

    @Test
    public void testMatchingOfSimpleQueryWithQuerqyFilter() throws IOException, SolrServerException {
        ExpandedQueryBuilder expanded = expanded(bq("tv"), bq("television"));

        final ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("fl", "*,score");

        final JsonQueryRequest jsonQuery = new JsonQueryRequest(params)
                .setQuery(createRequestToTestMatching(expanded));

        final QueryResponse response = jsonQuery.process(getSolrClient(), "collection1");

        Assertions.assertThat(response.getResults()).hasSize(2);
    }

    @Test
    public void testMatchingOfSimpleQueryWithSolrFilter() throws IOException, SolrServerException {
        ExpandedQueryBuilder expanded = expanded(bq("tv"));

        final ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("fl", "*,score");
        params.add("fq", "f1:television OR f2:television");

        final JsonQueryRequest jsonQuery = new JsonQueryRequest(params)
                .setQuery(createRequestToTestMatching(expanded));

        final QueryResponse response = jsonQuery.process(getSolrClient(), "collection1");

        Assertions.assertThat(response.getResults()).hasSize(2);
    }

    @Test
    public void testMatchingOfNestedQuery() throws IOException, SolrServerException {
        BooleanQueryBuilder query = bq(
                dmq(
                        term("tv"),
                        bq(
                                dmq("television").setOccur(MUST),
                                dmq("schwarz").setOccur(MUST)
                        )
                )
        );
        final ExpandedQueryBuilder expandedQuery = expanded(query);

        final ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("fl", "*,score");

        final JsonQueryRequest jsonQuery = new JsonQueryRequest(params)
                .setQuery(createRequestToTestMatching(expandedQuery));

        final QueryResponse response = jsonQuery.process(getSolrClient(), "collection1");

        Assertions.assertThat(response.getResults()).hasSize(5);
    }

    private static Map<String, Object> createRequestToTestMatching(ExpandedQueryBuilder expandedQuery) {
        Map expandedQueryMap = expandedQuery.toMap(MapConverterConfig.builder().parseBooleanToString(true).build());

        Map<String, Object> request = new HashMap<>();
        request.put("mm", "100%");
        request.put("tie", 0.0f);
        request.put("uq.similarityScore", "off");
        request.put("qf", "f1 f2");
        request.put("query", expandedQueryMap);

        return Collections.singletonMap("querqy", request);
    }

    private static Map<String, Object> createRequestToTestScoring(ExpandedQueryBuilder expandedQuery) {
        Map expandedQueryMap = expandedQuery.toMap(MapConverterConfig.builder().parseBooleanToString(true).build());

        Map<String, Object> request = new HashMap<>();
        request.put("mm", "100%");
        request.put("tie", 0.0f);
        request.put("uq.similarityScore", "off");
        request.put("qboost.similarityScore", "off");
        request.put("qf", "f1^40 f2^10");
        request.put("query", expandedQueryMap);

        return Collections.singletonMap("querqy", request);
    }

    private static Map<String, Object> doc(String id, float score) {
        Map<String, Object> solrDocument = new HashMap<>();
        solrDocument.put("id", id);
        solrDocument.put("score", score);
        return solrDocument;
    }
}
