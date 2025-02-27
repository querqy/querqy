package querqy.solr;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.file.PathUtils;
import org.apache.solr.SolrTestCase;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStreamBase;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.embedded.JettyConfig;
import org.apache.solr.embedded.JettySolrRunner;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.junit.*;
import querqy.solr.rewriter.commonrules.CommonRulesConfigRequestBuilder;
import querqy.solr.rewriter.commonrules.CommonRulesRewriterFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.io.filefilter.FileFilterUtils.*;
import static org.hamcrest.Matchers.containsString;
import static querqy.solr.QuerqyRewriterRequestHandler.ActionParam.SAVE;
import static querqy.solr.IndexRewriterContainer.configurationDocumentId;

public class IndexRewriterContainerTest extends SolrTestCase {

    private static final String QUERQY_CONFIG_CORE_NAME = "querqy";
    private static final String SEARCH_CORE_NAME_A = "search_a";
    private static final String SEARCH_CORE_NAME_B = "search_b";

    private static JettySolrRunner leaderJetty, followerJetty;
    private static SolrClient leaderClient, followerClient;

    private static Path leaderSolrHome;
    private static Path followerSolrHome;

    @BeforeClass
    public static void startServer() throws Exception {
        System.setProperty("solr.disable.allowUrls", "true");
        leaderSolrHome = prepareSolrHomeWithConfigSets("leader");
        followerSolrHome = prepareSolrHomeWithConfigSets("follower");
        startSolrLeader(leaderSolrHome.toString(), Path.of(leaderSolrHome.toString(), "data").toString());
        startSolrFollower(followerSolrHome.toString(), Path.of(followerSolrHome.toString(), "data").toString());
    }

    @Before
    public void createCores() throws Exception {
        createLeaderCore(leaderClient, QUERQY_CONFIG_CORE_NAME, "querqy");
        createLeaderCore(leaderClient, SEARCH_CORE_NAME_A, "collection_with_indexstorage");
        createLeaderCore(leaderClient, SEARCH_CORE_NAME_B, "collection_with_indexstorage");

        createFollowerCore(followerClient, QUERQY_CONFIG_CORE_NAME, "querqy");
        createFollowerCore(followerClient, SEARCH_CORE_NAME_A, "collection_with_indexstorage");
        createFollowerCore(followerClient, SEARCH_CORE_NAME_B, "collection_with_indexstorage");
    }

    @After
    public void deleteCores() throws Exception {
        deleteCore(leaderClient, QUERQY_CONFIG_CORE_NAME);
        deleteCore(leaderClient, SEARCH_CORE_NAME_A);
        deleteCore(leaderClient, SEARCH_CORE_NAME_B);

        deleteCore(followerClient, QUERQY_CONFIG_CORE_NAME);
        deleteCore(followerClient, SEARCH_CORE_NAME_A);
        deleteCore(followerClient, SEARCH_CORE_NAME_B);
    }

    @AfterClass
    public static void stopServer() throws Exception {
        if (null != leaderJetty) {
            leaderJetty.stop();
            leaderJetty = null;
        }
        if (null != followerJetty) {
            followerJetty.stop();
            followerJetty = null;
        }
        if (null != leaderClient) {
            leaderClient.close();
            leaderClient = null;
        }
        if (null != followerClient) {
            followerClient.close();
            followerClient = null;
        }
        PathUtils.deleteDirectory(leaderSolrHome);
        PathUtils.deleteDirectory(followerSolrHome);
    }

    @Test
    public void saveSingleRewriterConfiguration() throws Exception {
        final CommonRulesConfigRequestBuilder builder = new CommonRulesConfigRequestBuilder()
                .rules("a =>\n SYNONYM: aa");

        saveCommonRulesRewriter(leaderClient, SEARCH_CORE_NAME_A, "rewriterA", builder);

        final SolrDocumentList querqyConfigurationDocumentA = leaderClient.query(QUERQY_CONFIG_CORE_NAME, matchAll()).getResults();
        Assertions.assertThat(querqyConfigurationDocumentA).hasSize(1);
        final SolrDocument docA = querqyConfigurationDocumentA.stream().findFirst().get();
        assertThat(docA)
                .hasId(configurationDocumentId(SEARCH_CORE_NAME_A, "rewriterA"))
                .hasCoreName(SEARCH_CORE_NAME_A)
                .hasRewriterId("rewriterA")
                .hasConfVersion(1)
                .hasData(builder.buildJson());

        final NamedList<?> response = leaderClient.query(SEARCH_CORE_NAME_A, queryRewriterSubHandler("rewriterA")).getResponse();
        assertThat(response)
                .hasRewriterId("rewriterA")
                .hasClass(CommonRulesRewriterFactory.class.getName())
                .hasRules("a =>\n SYNONYM: aa");
    }

    @Test
    public void updateSingleRewriterConfiguration() throws Exception {
        final CommonRulesConfigRequestBuilder builderV1 = new CommonRulesConfigRequestBuilder()
                .rules("a =>\n SYNONYM: aa");
        final CommonRulesConfigRequestBuilder builderV2 = new CommonRulesConfigRequestBuilder()
                .rules("z =>\n SYNONYM: zz");

        saveCommonRulesRewriter(leaderClient, SEARCH_CORE_NAME_A, "rewriterA", builderV1);

        final SolrDocumentList querqyConfigurationDocumentV1 = leaderClient.query(QUERQY_CONFIG_CORE_NAME, matchAll()).getResults();
        Assertions.assertThat(querqyConfigurationDocumentV1).hasSize(1);
        final SolrDocument docV1 = querqyConfigurationDocumentV1.stream().findFirst().get();
        assertThat(docV1)
                .hasId(configurationDocumentId(SEARCH_CORE_NAME_A, "rewriterA"))
                .hasCoreName(SEARCH_CORE_NAME_A)
                .hasRewriterId("rewriterA")
                .hasConfVersion(1)
                .hasData(builderV1.buildJson());

        final NamedList<?> responseV1 = leaderClient.query(SEARCH_CORE_NAME_A, queryRewriterSubHandler("rewriterA")).getResponse();
        assertThat(responseV1)
                .hasRewriterId("rewriterA")
                .hasClass(CommonRulesRewriterFactory.class.getName())
                .hasRules("a =>\n SYNONYM: aa");

        saveCommonRulesRewriter(leaderClient, SEARCH_CORE_NAME_A, "rewriterA", builderV2);

        final SolrDocumentList querqyConfigurationDocumentV2 = leaderClient.query(QUERQY_CONFIG_CORE_NAME, matchAll()).getResults();
        Assertions.assertThat(querqyConfigurationDocumentV2).hasSize(1);
        final SolrDocument docV2 = querqyConfigurationDocumentV2.stream().findFirst().get();
        assertThat(docV2)
                .hasId(configurationDocumentId(SEARCH_CORE_NAME_A, "rewriterA"))
                .hasCoreName(SEARCH_CORE_NAME_A)
                .hasRewriterId("rewriterA")
                .hasConfVersion(1)
                .hasData(builderV2.buildJson());

        final NamedList<?> responseV2 = leaderClient.query(SEARCH_CORE_NAME_A, queryRewriterSubHandler("rewriterA")).getResponse();
        assertThat(responseV2)
                .hasRewriterId("rewriterA")
                .hasClass(CommonRulesRewriterFactory.class.getName())
                .hasRules("z =>\n SYNONYM: zz");
    }

    @Test
    public void saveMultipleRewriterConfigurations() throws Exception {
        final CommonRulesConfigRequestBuilder builderA = new CommonRulesConfigRequestBuilder()
                .rules("a =>\n SYNONYM: aa");
        final CommonRulesConfigRequestBuilder builderB = new CommonRulesConfigRequestBuilder()
                .rules("b =>\n SYNONYM: bb");

        // Add a rewriter config for each core
        saveCommonRulesRewriter(leaderClient, SEARCH_CORE_NAME_A, "rewriterA", builderA);
        saveCommonRulesRewriter(leaderClient, SEARCH_CORE_NAME_B, "rewriterB", builderB);

        final SolrDocumentList querqyConfigurationDocuments = leaderClient.query(QUERQY_CONFIG_CORE_NAME, matchAll()).getResults();
        Assertions.assertThat(querqyConfigurationDocuments).hasSize(2);

        final SolrDocumentList querqyConfigurationDocumentA = leaderClient.query(QUERQY_CONFIG_CORE_NAME, matchRewriterConfigById("rewriterA", SEARCH_CORE_NAME_A)).getResults();
        Assertions.assertThat(querqyConfigurationDocumentA).hasSize(1);
        final SolrDocument docA = querqyConfigurationDocumentA.stream().findFirst().get();
        assertThat(docA)
                .hasId(configurationDocumentId(SEARCH_CORE_NAME_A, "rewriterA"))
                .hasCoreName(SEARCH_CORE_NAME_A)
                .hasRewriterId("rewriterA")
                .hasConfVersion(1)
                .hasData(builderA.buildJson());

        final NamedList<?> responseA = leaderClient.query(SEARCH_CORE_NAME_A, queryRewriterSubHandler("rewriterA")).getResponse();
        assertThat(responseA)
                .hasRewriterId("rewriterA")
                .hasClass(CommonRulesRewriterFactory.class.getName())
                .hasRules("a =>\n SYNONYM: aa");

        final SolrDocumentList querqyConfigurationDocumentB = leaderClient.query(QUERQY_CONFIG_CORE_NAME, matchRewriterConfigById("rewriterB", SEARCH_CORE_NAME_B)).getResults();
        Assertions.assertThat(querqyConfigurationDocumentB).hasSize(1);
        final SolrDocument docB = querqyConfigurationDocumentB.stream().findFirst().get();
        assertThat(docB)
                .hasId(configurationDocumentId(SEARCH_CORE_NAME_B, "rewriterB"))
                .hasCoreName(SEARCH_CORE_NAME_B)
                .hasRewriterId("rewriterB")
                .hasConfVersion(1)
                .hasData(builderB.buildJson());

        final NamedList<?> responseB = leaderClient.query(SEARCH_CORE_NAME_B, queryRewriterSubHandler("rewriterB")).getResponse();
        assertThat(responseB)
                .hasRewriterId("rewriterB")
                .hasClass(CommonRulesRewriterFactory.class.getName())
                .hasRules("b =>\n SYNONYM: bb");
    }

    @Test
    public void deleteSingleRewriterConfiguration() throws Exception {
        final CommonRulesConfigRequestBuilder builder = new CommonRulesConfigRequestBuilder()
                .rules("a =>\n SYNONYM: aa");

        saveCommonRulesRewriter(leaderClient, SEARCH_CORE_NAME_A, "rewriterA", builder);

        final SolrDocumentList querqyConfigurationDocument = leaderClient.query(QUERQY_CONFIG_CORE_NAME, matchRewriterConfigById("rewriterA", SEARCH_CORE_NAME_A)).getResults();
        Assertions.assertThat(querqyConfigurationDocument).hasSize(1);

        deleteRewriter(leaderClient, SEARCH_CORE_NAME_A, "rewriterA");

        final SolrDocumentList allConfigurationDocuments = leaderClient.query(QUERQY_CONFIG_CORE_NAME, new SolrQuery("*:*")).getResults();
        Assertions.assertThat(allConfigurationDocuments).isEmpty();

        final SolrQuery queryRewriterA = queryRewriterSubHandler("rewriterA");
        final SolrException exception = assertThrows(SolrException.class, () -> leaderClient.query(SEARCH_CORE_NAME_A, queryRewriterA));
        Assertions.assertThat(exception.getMessage()).contains("No such rewriter: rewriterA");
    }

    @Test
    public void deleteMultipleRewriterConfigurations() throws Exception {
        final CommonRulesConfigRequestBuilder builderA = new CommonRulesConfigRequestBuilder()
                .rules("a =>\n SYNONYM: aa");
        final CommonRulesConfigRequestBuilder builderB = new CommonRulesConfigRequestBuilder()
                .rules("b =>\n SYNONYM: bb");

        // Add a rewriter config for each core
        saveCommonRulesRewriter(leaderClient, SEARCH_CORE_NAME_A, "rewriterA", builderA);
        saveCommonRulesRewriter(leaderClient, SEARCH_CORE_NAME_B, "rewriterB", builderB);

        final SolrDocumentList querqyConfigurationDocumentA = leaderClient.query(QUERQY_CONFIG_CORE_NAME, matchRewriterConfigById("rewriterA", SEARCH_CORE_NAME_A)).getResults();
        Assertions.assertThat(querqyConfigurationDocumentA).hasSize(1);

        final SolrDocumentList querqyConfigurationDocumentB = leaderClient.query(QUERQY_CONFIG_CORE_NAME, matchRewriterConfigById("rewriterB", SEARCH_CORE_NAME_B)).getResults();
        Assertions.assertThat(querqyConfigurationDocumentB).hasSize(1);

        deleteRewriter(leaderClient, SEARCH_CORE_NAME_A, "rewriterA");

        final SolrQuery queryRewriterA = queryRewriterSubHandler("rewriterA");
        final SolrException exceptionA = assertThrows(SolrException.class, () -> leaderClient.query(SEARCH_CORE_NAME_A, queryRewriterA));
        Assertions.assertThat(exceptionA.getMessage()).contains("No such rewriter: rewriterA");

        SolrDocumentList allConfigurationDocuments = leaderClient.query(QUERQY_CONFIG_CORE_NAME, matchAll()).getResults();
        Assertions.assertThat(allConfigurationDocuments).hasSize(1);

        deleteRewriter(leaderClient, SEARCH_CORE_NAME_B, "rewriterB");

        final SolrQuery queryRewriterB = queryRewriterSubHandler("rewriterB");
        final SolrException exceptionB = assertThrows(SolrException.class, () -> leaderClient.query(SEARCH_CORE_NAME_B, queryRewriterB));
        Assertions.assertThat(exceptionB.getMessage()).contains("No such rewriter: rewriterB");

        allConfigurationDocuments = leaderClient.query(QUERQY_CONFIG_CORE_NAME, matchAll()).getResults();
        Assertions.assertThat(allConfigurationDocuments).isEmpty();
    }

    @Test
    public void getUnknownRewriterConfiguration() {
        final SolrQuery queryRewriter = queryRewriterSubHandler("rewriterX");
        final SolrException exception = assertThrows(SolrException.class, () -> leaderClient.query(SEARCH_CORE_NAME_A, queryRewriter));
        Assertions.assertThat(exception.getMessage()).contains("No such rewriter: rewriterX");
    }

    @Test
    public void deleteUnknownRewriterConfiguration() {
        SolrException solrException = assertThrows(SolrException.class, () -> deleteRewriter(leaderClient, SEARCH_CORE_NAME_A, "rewriterX"));
        assertThat(solrException.getMessage(), containsString("No such rewriter: rewriterX"));
    }

    @Test
    public void deleteRewriterConfigurationOnFollower() {
        SolrException solrException = assertThrows(SolrException.class, () -> deleteRewriter(followerClient, SEARCH_CORE_NAME_A, "rewriterX"));
        assertThat(solrException.getMessage(), containsString("Rewriter config must be updated via the leader"));
    }

    @Test
    public void indexRewriterReplication() throws Exception {
        final CommonRulesConfigRequestBuilder builder = new CommonRulesConfigRequestBuilder()
                .rules("laptop =>\n SYNONYM: notebook");

        final SolrInputDocument document = new SolrInputDocument();
        document.addField("id", "doc1");
        document.addField("f1", "notebook");
        leaderClient.add(SEARCH_CORE_NAME_A, document);
        leaderClient.commit(SEARCH_CORE_NAME_A);

        saveCommonRulesRewriter(leaderClient, SEARCH_CORE_NAME_A, "common_rules", builder);

        final long start = System.currentTimeMillis();
        final var timeout = Duration.ofSeconds(30);
        while (true) {
            final Duration timeWaitedForReplication = Duration.ofMillis(System.currentTimeMillis() - start);
            if (timeWaitedForReplication.compareTo(timeout) > 0) {
                fail("Replication of rewriter configuration did not happen after " + timeWaitedForReplication);
            }
            try {
                ModifiableSolrParams params = new ModifiableSolrParams();
                params.set(CommonParams.Q, "laptop");
                params.set("defType", "querqy");
                params.set(QuerqyQParserPlugin.PARAM_REWRITERS, "common_rules");
                params.set(DisMaxParams.QF, "f1");
                QueryResponse query = followerClient.query(SEARCH_CORE_NAME_A, params);
                if (query.getResults().getNumFound() > 0) {
                    break;
                }
            } catch (final SolrException e) {
                if (!e.getMessage().contains("No such rewriter")) {
                    throw e;
                }
            }
            // Replication is async, eventually we need to retry after another second
            TimeUnit.SECONDS.sleep(1);
        }
    }

    private static SolrQuery matchAll() {
        return new SolrQuery("*:*");
    }

    private static SolrQuery matchRewriterConfigById(final String rewriterId, final String coreName) {
        return new SolrQuery(String.format("id:%s", configurationDocumentId(coreName, rewriterId)));
    }

    private static SolrQuery queryRewriterSubHandler(final String rewriterId) {
        return new SolrQuery("qt", "/querqy/rewriter/" + rewriterId);
    }

    private static ReturnedRewriterConfigurationAssert assertThat(final NamedList<?> actual) {
        return new ReturnedRewriterConfigurationAssert(actual);
    }

    private static StoredRewriterConfigurationAssert assertThat(final SolrDocument actual) {
        return new StoredRewriterConfigurationAssert(actual);
    }

    private static class ReturnedRewriterConfigurationAssert extends AbstractAssert<ReturnedRewriterConfigurationAssert, NamedList<?>> {

        public ReturnedRewriterConfigurationAssert(final NamedList<?> actual) {
            super(actual, IndexRewriterContainerTest.ReturnedRewriterConfigurationAssert.class);
        }

        public ReturnedRewriterConfigurationAssert hasRewriterId(final String expectedRewriterId) {
            final Map<?, ?> rewriterMap = (Map<?, ?>) actual.get("rewriter");
            final String actualRewriterId = (String) rewriterMap.get("id");
            if (!expectedRewriterId.equals(actualRewriterId)) {
                failWithMessage("Expected rewriter id to be <%s> but was <%s>", expectedRewriterId, actualRewriterId);
            }
            return this;
        }

        public ReturnedRewriterConfigurationAssert hasClass(final String expectedClass) {
            final Map<?, ?> rewriterMap = (Map<?, ?>) actual.get("rewriter");
            final Map<?, ?> definition = (Map<?, ?>) rewriterMap.get("definition");
            final String actualClass = (String) definition.get("class");
            if (!expectedClass.equals(actualClass)) {
                failWithMessage("Expected rewriter class to be <%s> but was <%s>", expectedClass, actualClass);
            }
            return this;
        }

        public ReturnedRewriterConfigurationAssert hasRules(final String expectedRules) {
            final Map<?, ?> rewriterMap = (Map<?, ?>) actual.get("rewriter");
            final Map<?, ?> definition = (Map<?, ?>) rewriterMap.get("definition");
            final Map<?, ?> config = (Map<?, ?>) definition.get("config");
            final String actualRules = (String) config.get("rules");
            if (!expectedRules.equals(actualRules)) {
                failWithMessage("Expected rewriter rules to be <%s> but was <%s>", expectedRules, actualRules);
            }
            return this;
        }

    }

    private static class StoredRewriterConfigurationAssert extends AbstractAssert<StoredRewriterConfigurationAssert, SolrDocument> {

        public StoredRewriterConfigurationAssert(final SolrDocument actual) {
            super(actual, StoredRewriterConfigurationAssert.class);
        }

        public StoredRewriterConfigurationAssert hasId(final String id) {
            Assertions.assertThat(actual.getFieldValue("id")).isEqualTo(id);
            return this;
        }

        public StoredRewriterConfigurationAssert hasCoreName(final String coreName) {
            Assertions.assertThat(actual.getFieldValue("core")).isEqualTo(coreName);
            return this;
        }

        public StoredRewriterConfigurationAssert hasRewriterId(final String rewriterId) {
            Assertions.assertThat(actual.getFieldValue("rewriterId")).isEqualTo(rewriterId);
            return this;
        }

        public StoredRewriterConfigurationAssert hasData(final String jsonData) {
            Assertions.assertThat(new String((byte[]) actual.getFieldValue("data"), StandardCharsets.UTF_8)).isEqualTo(jsonData);
            return this;
        }

        public StoredRewriterConfigurationAssert hasConfVersion(final int confVersion) {
            Assertions.assertThat(actual.getFieldValue("confVersion")).isEqualTo(confVersion);
            return this;
        }
    }

    static void saveCommonRulesRewriter(final SolrClient client,
                                        final String collection,
                                        final String rewriterId,
                                        final CommonRulesConfigRequestBuilder builder
    ) throws SolrServerException, IOException {
        final var request = new ContentStreamUpdateRequest("/querqy/rewriter/" + rewriterId);
        request.addContentStream(new ContentStreamBase.StringStream(builder.buildJson()));
        request.setParams(new ModifiableSolrParams(SAVE.params()));
        request.process(client, collection);
    }

    static void deleteRewriter(final SolrClient client,
                                        final String collection,
                                        final String rewriterId
    ) throws SolrServerException, IOException {
        final var deleteRequest = RewriterConfigRequestBuilder.buildDeleteRequest(rewriterId);
        deleteRequest.process(client, collection);
    }

    public static Http2SolrClient createNewSolrClient(String baseUrl, String collectionOrCore) {
        return new Http2SolrClient.Builder(baseUrl)
                .withDefaultCollection(collectionOrCore)
                .withConnectionTimeout(15000, TimeUnit.MILLISECONDS)
                .withIdleTimeout(90000, TimeUnit.MILLISECONDS)
                .build();
    }

    private static void startSolrLeader(final String homeDir, final String dataDir) throws Exception {
        leaderJetty = createAndStartSolr(homeDir, dataDir);
        leaderClient = createNewSolrClient("http://localhost:" + leaderJetty.getLocalPort() + "/solr", null);
    }

    private static void startSolrFollower(final String homeDir, final String dataDir) throws Exception {
        followerJetty = createAndStartSolr(homeDir, dataDir);
        followerClient = createNewSolrClient("http://localhost:" + followerJetty.getLocalPort() + "/solr", null);
    }

    private static Path prepareSolrHomeWithConfigSets(String name) throws Exception {
        final Path solrHome = SolrTestCase.createTempDir("solrHome-" + name);
        final Path configsetResourcePath = resourcePath();
        FileUtils.copyDirectory(configsetResourcePath.toFile(), solrHome.toFile());
        FileUtils.copyDirectory(configsetResourcePath.toFile(), solrHome.toFile(), notFileFilter(
                and(directoryFileFilter(), nameFileFilter("collection1"))));
        return solrHome;
    }

    private static Path resourcePath() throws Exception {
        return new File(Objects.requireNonNull(IndexRewriterContainer.class.getClassLoader().getResource("solr")).toURI()).getAbsoluteFile().toPath();
    }

    private static final class CreateWithPropertiesRequest extends CoreAdminRequest.Create {
        private final Map<String, String> properties;
        CreateWithPropertiesRequest(final Map<String, String> properties) {
            this.properties = properties;
        }
        @Override
        public SolrParams getParams() {
            final var params = new ModifiableSolrParams(super.getParams());
            properties.forEach((String key, String value) -> params.set("property." + key, value));
            return params;
        }
    }

    private static void createLeaderCore(
            final SolrClient solrClient,
            final String name,
            final String configset
    ) throws SolrServerException, IOException {
        createCore(solrClient, name, configset, Map.of());
    }

    private static void createFollowerCore(
            final SolrClient solrClient,
            final String name,
            final String configset
    ) throws SolrServerException, IOException {
        final var properties = new HashMap<String, String>();
        properties.put("solr.replication.leader.url", "http://localhost:" + leaderJetty.getLocalPort() + "/solr/" + name);
        properties.put("solr.replication.follower.enabled", "true");
        createCore(solrClient, name, configset, properties);
    }


    private static void createCore(
            final SolrClient solrClient,
            final String name,
            final String configset,
            final Map<String, String> properties
    ) throws SolrServerException, IOException {
        final CoreAdminRequest.Create req = new CreateWithPropertiesRequest(properties);
        req.setCoreName(name);
        req.setInstanceDir(name);
        req.setConfigSet(configset);
        solrClient.request(req);
    }

    private static void deleteCore(final SolrClient solrClient, final String name) throws SolrServerException, IOException {
        CoreAdminRequest.unloadCore(name, true, true, solrClient);
    }

    private static JettySolrRunner createAndStartSolr(final String homeDir, final String dataDir) throws Exception {
        Properties nodeProperties = new Properties();
        nodeProperties.setProperty("solr.data.dir", dataDir);
        JettyConfig jettyConfig = JettyConfig.builder().setPort(0).build();
        JettySolrRunner jetty = new JettySolrRunner(homeDir, nodeProperties, jettyConfig);
        jetty.start();
        return jetty;
    }
}


