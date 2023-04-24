package querqy.solr.rewriter.embeddings;

import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.util.ContentStreamBase;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.request.SolrRequestInfo;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import querqy.embeddings.ServiceEmbeddingModel;
import querqy.lucene.embeddings.EmbeddingsRewriter;
import querqy.solr.RewriterConfigRequestBuilder;
import querqy.utils.JsonUtil;

import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static querqy.lucene.embeddings.EmbeddingsRewriterFactory.*;
import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.QuerqyRewriterRequestHandler.ActionParam.SAVE;
import static querqy.solr.rewriter.embeddings.DummyEmbeddingModel.EMBEDDINGS;

@SolrTestCaseJ4.SuppressSSL
public class EmbeddingsRewriterTest extends SolrTestCaseJ4 {
    private static final String EMB_REWRITER_DUMMY  = "embDummy";
    private static final String EMB_REWRITER_SERVICE = "embService";

    private final String embeddingsRewriterName;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    @ParametersFactory
    public static Iterable<Object[]> parameters() {
        // test both, an in-memory dummy model, and the external service model
        return Arrays.asList(new Object[][]{{EMB_REWRITER_DUMMY}, {EMB_REWRITER_SERVICE}});
    }

    public EmbeddingsRewriterTest(String embeddingsRewriterName) {
        this.embeddingsRewriterName = embeddingsRewriterName;
    }

    private static SolrInputDocument doc(final String id, final String f1, final String f2, final float[] vec) {
        final SolrInputDocument doc = new SolrInputDocument();
        doc.addField("id", id);
        doc.addField("f1", f1);
        doc.addField("f2", f2);
        final List<Float> val = new ArrayList<>(vec.length);
        for (final float v : vec) {
            val.add(v);
        }
        doc.addField("vector", val);
        return doc;
    }
    private static void addDocs() {
        // the first doc has the vector for w1, the second for w2 etc.
        // if we query with boost "w2", all docs will match but the second doc (id=2) should come back at the top
        // -> see testBoost
        assertU(adoc(doc("1", "a b c w1 w2 w3 w4", "d", EMBEDDINGS.get("w1"))));
        assertU(adoc(doc("2", "a b c w1 w2 w3 w4", "e", EMBEDDINGS.get("w2"))));
        assertU(adoc(doc("3", "a b c w1 w2 w3 w4", "f", EMBEDDINGS.get("w3"))));
        assertU(adoc(doc("4", "gh i w1 w2 w3 w4", "j",  EMBEDDINGS.get("w4"))));

        assertU(commit());
    }
    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig.xml", "rewriter/schema-embeddings.xml");
        addDocs();
    }

    @Before
    public void setupStubs() {
        setupEmbeddingsRewriter(h.getCore(), EMB_REWRITER_DUMMY, embeddingsConfigDummy());

        stubServiceApiResponse();
        setupEmbeddingsRewriter(h.getCore(), EMB_REWRITER_SERVICE, embeddingsConfigCustomService(wireMockRule.port()));
    }

    public static void setupEmbeddingsRewriter(final SolrCore core, final String rewriterId,
                                               RewriterConfigRequestBuilder rewriterConfigRequestBuilder) {
        SolrRequestHandler handler = core.getRequestHandler("/querqy/rewriter/" + rewriterId);

        final LocalSolrQueryRequest req = new LocalSolrQueryRequest(core, SAVE.params());
        req.setContentStreams(Collections.singletonList(new ContentStreamBase.StringStream(rewriterConfigRequestBuilder.buildJson())));
        req.getContext().put("httpMethod", "POST");

        final SolrQueryResponse rsp = new SolrQueryResponse();
        SolrRequestInfo.setRequestInfo(new SolrRequestInfo(req, rsp));
        try {
            core.execute(handler, req, rsp);
        } finally {
            SolrRequestInfo.clearRequestInfo();
            req.close();
        }
    }

    @Test
    public void testBoost() {

        String q = "w2";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                //DisMaxParams.MM, "100%",
                "defType", "querqy",
                "debugQuery", "on",
                "fl", "id,score",
                PARAM_REWRITERS, embeddingsRewriterName,
                PARAM_QUERQY_PREFIX + embeddingsRewriterName + PARAM_TOP_K,  "1",
                PARAM_QUERQY_PREFIX + embeddingsRewriterName + PARAM_BOOST,  "100",
                PARAM_QUERQY_PREFIX + embeddingsRewriterName + PARAM_MODE , EmbeddingsRewriter.EmbeddingQueryMode.BOOST.name(),
                PARAM_QUERQY_PREFIX + embeddingsRewriterName + PARAM_VECTOR_FIELD , "vector"

        );

        assertQ("Boosting not working",
                req,
                "//result[@name='response' and @numFound='4']",
                "//doc[1]/str[@name='id' and text()='2']"
        );


        req.close();
    }

    @Test
    public void testMainQuery() {

        String q = "w4";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                "defType", "querqy",
                "debugQuery", "on",
                PARAM_REWRITERS, embeddingsRewriterName,
                PARAM_QUERQY_PREFIX + embeddingsRewriterName + PARAM_TOP_K,  "2",
                PARAM_QUERQY_PREFIX + embeddingsRewriterName + PARAM_BOOST,  "100",
                PARAM_QUERQY_PREFIX + embeddingsRewriterName + PARAM_MODE , EmbeddingsRewriter.EmbeddingQueryMode.MAIN_QUERY.name(),
                PARAM_QUERQY_PREFIX + embeddingsRewriterName + PARAM_VECTOR_FIELD , "vector"

        );

        assertQ("Main query not working",
                req,
                "//result[@name='response' and @numFound='2']",
                "//doc[1]/str[@name='id' and text()='4']",
                "//doc[2]/str[@name='id' and text()='2']"
        );

        req.close();
    }

    private static EmbeddingsConfigRequestBuilder embeddingsConfigDummy() {
        return new EmbeddingsConfigRequestBuilder().model(DummyEmbeddingModel.class, null);
    }

    private static EmbeddingsConfigRequestBuilder embeddingsConfigCustomService(int port) {
        Map<String, Object> serviceRequestTemplate = Map.of(
                "text", "{{text}}",
                "output_format", "float_list",
                "separator", ",",
                "normalize", true);
        Map<String, Object> serviceEmbeddingModelConfig = Map.of(
                "url", "http://localhost:" + port + "/minilm/text/",
                "normalize", true,
                "request_template", serviceRequestTemplate,
                "response_path", "$.embedding");
        return new EmbeddingsConfigRequestBuilder().model(ServiceEmbeddingModel.class, serviceEmbeddingModelConfig);
    }

    private static void stubServiceApiResponse() {
        for (String query: new String[] { "w1", "w2", "w3", "w4" }) {
            float[] embedding = EMBEDDINGS.get(query);
            stubFor(post("/minilm/text/")
                    .withHeader("Content-Type", containing("json"))
                    .withRequestBody(equalToJson("{\"normalize\":true,\"output_format\":\"float_list\",\"separator\":\",\",\"text\":\"" + query + "\"}"))
                    .willReturn(ok()
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"embedding\":" + JsonUtil.toJson(embedding) + "}")));
        }
    }



}