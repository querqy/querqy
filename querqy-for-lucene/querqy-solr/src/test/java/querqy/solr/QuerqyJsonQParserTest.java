package querqy.solr;

import org.apache.lucene.tests.util.LuceneTestCase;
import org.apache.solr.SolrJettyTestBase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.json.JsonQueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.util.SolrJettyTestRule;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import querqy.model.convert.converter.MapConverterConfig;
import querqy.model.convert.builder.BooleanQueryBuilder;
import querqy.model.convert.builder.ExpandedQueryBuilder;
import querqy.rewrite.experimental.QueryRewritingHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
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

public class QuerqyJsonQParserTest extends SolrTestCaseJ4 {

    @ClassRule
    public static final SolrJettyTestRule solrRule = new SolrJettyTestRule();

    private static Path HOME;

    //@BeforeClass
    public static void beforeTests() throws Exception {

        HOME = Files.createTempDirectory(getSimpleClassName());
        final File collDir = new File(HOME.toFile(), "collection1");
        if (!collDir.mkdir()) {
            throw new IOException("Could not create collection dir");
        }
        Files.copy(getFile("solr/solr.xml").toPath(), HOME.resolve("solr.xml"));
        final File confDir = new File(collDir, "conf");
        if (!confDir.mkdir()) {
            throw new IOException("Could not create conf dir");
        }

        Files.copy(getFile("solr/collection1/conf/solrconfig-external-rewriting.xml").toPath(),
                HOME.resolve("collection1").resolve("conf").resolve("solrconfig.xml")
                );
        Files.copy(getFile("solr/collection1/conf/schema.xml").toPath(),
                HOME.resolve("collection1").resolve("conf").resolve("schema.xml")
        );

        Files.copy(getFile("solr/collection1/core.properties").toPath(),
                HOME.resolve("collection1").resolve("core.properties")
        );

        solrRule.startSolr(LuceneTestCase.createTempDir());
        solrRule.newCollection().withConfigSet(HOME.toString());


        //initCore("solrconfig.xml", "schema.xml", HOME.toString());
        //createAndStartJetty(HOME.toString());

        addDocs();
    }

    //@AfterClass
    public static void cleanUp() throws IOException {
        Files.walkFileTree(HOME, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                file.toFile().delete();
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                dir.toFile().delete();
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void addDocs() throws Exception {
        final HttpSolrClient solrClient = getHttpSolrClient(solrRule.getBaseUrl().toString());
        solrClient.add("collection1",
                Arrays.asList(new SolrInputDocument("id", "0", "f1", "tv", "f2", "television"),
                    new SolrInputDocument("id", "1", "f1", "tv"),
                    new SolrInputDocument("id", "2", "f1", "tv", "f2", "led"),
                    new SolrInputDocument("id", "11", "f1", "led tv", "f2", "television"),
                    new SolrInputDocument("id", "20", "f1", "television", "f2", "schwarz"),
                    new SolrInputDocument("id", "21", "f1", "blau", "f2", "television")

                )
        );
        solrClient.commit("collection1");

    }
    @Ignore
    @Test
    public void testThatNoExceptionIsThrownIfQueryParserIsProperlySetInRequestParameters() {
        final ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("defType", "querqy");

        Assertions.assertThatCode(() ->
                createRequestToTestMatching(QueryRewritingHandler.builder()
                        .build()
                        .rewriteQuery("tv")
                        .getQuery(), params)
                        .process(solrRule.getSolrClient()))
                .doesNotThrowAnyException();
    }
    @Ignore
    @Test
    public void testThatNoExceptionIsThrownIfQueryParserIsSetProperlyInSolrConfigParameters() {
        final ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("qt", "/rh-with-proper-def-type");

        Assertions.assertThatCode(() ->
                createRequestToTestMatching(QueryRewritingHandler.builder()
                        .build()
                        .rewriteQuery("tv")
                        .getQuery(), params)
                        .process(solrRule.getSolrClient()))
                .doesNotThrowAnyException();
    }
    @Ignore
    @Test
    public void testMatchingOfSimpleQueryIfDefTypeIsCorrectlyDefined() throws IOException, SolrServerException {
        ExpandedQueryBuilder expanded = expanded(bq("tv"));

        final ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("defType", "querqy");
        params.add("fl", "*,score");

        final QueryResponse response = createRequestToTestMatching(expanded, params)
                .process(solrRule.getSolrClient());

        Assertions.assertThat(response.getResults()).hasSize(4);
    }
    @Ignore
    @Test
    public void testQueryRewritingHandler() throws IOException, SolrServerException {
        final ExpandedQueryBuilder expanded = QueryRewritingHandler.builder()
                .addCommonRulesRewriter("tv => \n SYNONYM: television")
                .build()
                .rewriteQuery("tv")
                .getQuery();


        final QueryResponse response = createRequestToTestMatching(expanded)
                .process(solrRule.getSolrClient());

        Assertions.assertThat(response.getResults()).hasSize(6);
    }
    @Ignore
    @Test
    public void testScoringOfFieldWeightsWithDownBoost() throws IOException, SolrServerException {
        ExpandedQueryBuilder expanded = expanded(
                bq(dmq("tv")),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList(boost(bq("television"), 5.0f)),
                Collections.emptyList()
        );

        final ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("fl", "id,score");
        params.add("fq", "id:(2 OR 11)");
        params.add("debugQuery", "true");

        final QueryResponse response = createRequestToTestScoring(expanded, params)
                .process(solrRule.getSolrClient());

        final List<Map<String, Object>> results = response.getResults().stream().map(HashMap::new).collect(Collectors.toList());

        Assertions.assertThat(results).contains(
                doc("2", 45.0f)
        );
    }
    @Ignore
    @Test
    public void testScoringOfFieldWeightsWithRawQueryUpBoost() throws IOException, SolrServerException {
        ExpandedQueryBuilder expanded = expanded(
                bq(dmq("tv", "television")),
                boost(raw("{!func}if(query({!lucene df=f1 v=blau}),100,0)"), 1.0f)
        );

        final ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("fl", "score");
        params.add("fq", "id:21");

        final QueryResponse response = createRequestToTestScoring(expanded, params)
                .process(solrRule.getSolrClient());

        final List<Map<String, Object>> results = response.getResults().stream().map(HashMap::new).collect(Collectors.toList());

        Assertions.assertThat((Float) results.get(0).get("score")).isEqualTo(110.0f);
    }
    @Ignore
    @Test
    public void testScoringOfFieldWeightsWithSimpleBoost() throws IOException, SolrServerException {
        ExpandedQueryBuilder expanded = expanded(
                bq(dmq("tv", "television")),
                boost(bq("blau"), 100.0f)
        );

        final ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("fl", "score");
        params.add("fq", "id:21");

        final QueryResponse response = createRequestToTestScoring(expanded, params)
                .process(solrRule.getSolrClient());

        final List<Map<String, Object>> results = response.getResults().stream().map(HashMap::new).collect(Collectors.toList());

        Assertions.assertThat((Float) results.get(0).get("score")).isGreaterThan(10.0f);
    }
    @Ignore
    @Test
    public void testScoringOfFieldWeights() throws IOException, SolrServerException {
        ExpandedQueryBuilder expanded = expanded(bq(dmq("tv", "television")));

        final ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("fl", "id,score");
        params.add("fq", "id:(0 OR 21)");

        final QueryResponse response = createRequestToTestScoring(expanded, params)
                .process(solrRule.getSolrClient());

        final List<Map<String, Object>> results = response.getResults().stream().map(HashMap::new).collect(Collectors.toList());

        Assertions.assertThat(results).containsExactlyInAnyOrder(
                doc("0", 40.0f), doc("21", 10.0f)
        );
    }
    @Ignore
    @Test
    public void testMatchAllQuery() throws IOException, SolrServerException {
        ExpandedQueryBuilder expanded = expanded(matchall());

        final ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("fl", "*,score");

        final QueryResponse response = createRequestToTestMatching(expanded, params)
                .process(solrRule.getSolrClient());

        Assertions.assertThat(response.getResults()).hasSize(6);
    }
    @Ignore
    @Test
    public void testMatchingOfSimpleQuery() throws IOException, SolrServerException {
        ExpandedQueryBuilder expanded = expanded(bq("tv"));

        final ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("fl", "*,score");

        final QueryResponse response = createRequestToTestMatching(expanded, params)
                .process(solrRule.getSolrClient());

        Assertions.assertThat(response.getResults()).hasSize(4);
    }
    @Ignore
    @Test
    public void testMatchingOfSimpleQueryWithQuerqyFilter() throws IOException, SolrServerException {
        ExpandedQueryBuilder expanded = expanded(bq("tv"), bq("television"));

        final ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("fl", "*,score");

        final QueryResponse response = createRequestToTestMatching(expanded, params)
                .process(solrRule.getSolrClient());

        Assertions.assertThat(response.getResults()).hasSize(2);
    }
    @Ignore
    @Test
    public void testMatchingOfSimpleQueryWithSolrFilter() throws IOException, SolrServerException {
        ExpandedQueryBuilder expanded = expanded(bq("tv"));

        final ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("fl", "*,score");
        params.add("fq", "f1:television OR f2:television");

        final QueryResponse response = createRequestToTestMatching(expanded, params)
                .process(solrRule.getSolrClient());

        Assertions.assertThat(response.getResults()).hasSize(2);
    }

    @Ignore
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

        final QueryResponse response = createRequestToTestMatching(expandedQuery, params)
                .process(solrRule.getSolrClient());

        Assertions.assertThat(response.getResults()).hasSize(5);
    }

    private static JsonQueryRequest createRequestToTestMatching(final ExpandedQueryBuilder expandedQuery,
                                                                final ModifiableSolrParams params) {
        final Map expandedQueryMap = expandedQuery.toMap(MapConverterConfig.builder().parseBooleanToString(true).build());

        params.add("mm", "100%");
        params.add("tie", "0.0");
        params.add("uq.similarityScore", "off");
        params.add("qf", "f1 f2");

        return new JsonQueryRequest(params).setQuery(
                Collections.singletonMap("querqy",
                        Collections.singletonMap("query", expandedQueryMap)));
    }

    private static JsonQueryRequest createRequestToTestMatching(final ExpandedQueryBuilder expandedQuery) {
        return createRequestToTestMatching(expandedQuery, new ModifiableSolrParams());
    }

    private static JsonQueryRequest createRequestToTestScoring(final ExpandedQueryBuilder expandedQuery,
                                                               final ModifiableSolrParams params) {
        final Map expandedQueryMap = expandedQuery.toMap(MapConverterConfig.builder().parseBooleanToString(true).build());

        params.add("mm", "100%");
        params.add("tie", "0.0");
        params.add("uq.similarityScore", "off");
        params.add("qboost.similarityScore", "off");
        params.add("qf", "f1^40 f2^10");

        return new JsonQueryRequest(params).setQuery(
                Collections.singletonMap("querqy",
                        Collections.singletonMap("query", expandedQueryMap)));
    }

    private static Map<String, Object> doc(String id, float score) {
        Map<String, Object> solrDocument = new HashMap<>();
        solrDocument.put("id", id);
        solrDocument.put("score", score);
        return solrDocument;
    }
}
