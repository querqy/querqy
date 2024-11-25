package querqy.solr;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.file.PathUtils;
import org.apache.solr.SolrTestCase;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.NodeConfig;
import org.apache.solr.core.SolrCore;
import org.apache.solr.update.UpdateShardHandlerConfig;
import org.assertj.core.api.AbstractAssert;
import org.junit.*;
import querqy.solr.rewriter.commonrules.CommonRulesConfigRequestBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import org.assertj.core.api.Assertions;
import querqy.solr.rewriter.commonrules.CommonRulesRewriterFactory;

import static querqy.solr.SolrCoreRewriterContainer.configurationDocumentId;
import static querqy.solr.StandaloneSolrTestSupport.deleteRewriter;
import static querqy.solr.StandaloneSolrTestSupport.withCommonRulesRewriter;

public class SolrCoreRewriterContainerTest extends SolrTestCase {

    private static final String QUERQY_CONFIG_CORE_NAME = "querqy";
    private static final String SEARCH_CORE_NAME_A = "search_a";
    private static final String SEARCH_CORE_NAME_B = "search_b";

    private static Path solrHome;
    private static EmbeddedSolrServer embeddedSolr;
    private static CoreContainer coreContainer;

    @BeforeClass
    public static void startServer() throws Exception {
        solrHome = prepareSolrHomeWithConfigsets("solr");
        startSolr(solrHome);
    }

    @Before
    public void createCores() throws Exception {
        createCore(embeddedSolr, QUERQY_CONFIG_CORE_NAME, "querqyrewriters", null, null);
        createCore(embeddedSolr, SEARCH_CORE_NAME_A, "collection1", "solrconfig-core-rewritercontainer.xml", null);
        createCore(embeddedSolr, SEARCH_CORE_NAME_B, "collection1", "solrconfig-core-rewritercontainer.xml", null);
    }

    @After
    public void deleteCores() throws Exception {
        deleteCore(embeddedSolr, QUERQY_CONFIG_CORE_NAME);
        deleteCore(embeddedSolr, SEARCH_CORE_NAME_A);
        deleteCore(embeddedSolr, SEARCH_CORE_NAME_B);
    }

    @AfterClass
    public static void stopServer() throws IOException {
        if (coreContainer != null) {
            coreContainer.shutdown();
        }
        if (embeddedSolr != null) {
            embeddedSolr.close();
        }
        PathUtils.deleteDirectory(solrHome);
    }

    @Test
    public void saveSingleRewriterConfiguration() throws Exception {
        final CommonRulesConfigRequestBuilder builder = new CommonRulesConfigRequestBuilder()
                .rules("a =>\n SYNONYM: aa");

        try (SolrCore searchCore = embeddedSolr.getCoreContainer().getCore(SEARCH_CORE_NAME_A)) {
            withCommonRulesRewriter(searchCore, "rewriterA", builder);
        }

        SolrDocumentList querqyConfigurationDocumentA = embeddedSolr.query(QUERQY_CONFIG_CORE_NAME, matchAll()).getResults();
        Assertions.assertThat(querqyConfigurationDocumentA).hasSize(1);
        SolrDocument docA = querqyConfigurationDocumentA.stream().findFirst().get();
        assertThat(docA)
                .hasId(SolrCoreRewriterContainer.configurationDocumentId(SEARCH_CORE_NAME_A, "rewriterA"))
                .hasCoreName(SEARCH_CORE_NAME_A)
                .hasRewriterId("rewriterA")
                .hasData(builder.buildJson());

        NamedList<?> response = embeddedSolr.query(SEARCH_CORE_NAME_A, queryRewriterSubHandler("rewriterA")).getResponse();
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

        try (SolrCore searchCore = embeddedSolr.getCoreContainer().getCore(SEARCH_CORE_NAME_A)) {
            withCommonRulesRewriter(searchCore, "rewriterA", builderV1);
        }

        SolrDocumentList querqyConfigurationDocumentV1 = embeddedSolr.query(QUERQY_CONFIG_CORE_NAME, matchAll()).getResults();
        Assertions.assertThat(querqyConfigurationDocumentV1).hasSize(1);
        SolrDocument docV1 = querqyConfigurationDocumentV1.stream().findFirst().get();
        assertThat(docV1)
                .hasId(SolrCoreRewriterContainer.configurationDocumentId(SEARCH_CORE_NAME_A, "rewriterA"))
                .hasCoreName(SEARCH_CORE_NAME_A)
                .hasRewriterId("rewriterA")
                .hasData(builderV1.buildJson());

        NamedList<?> responseV1 = embeddedSolr.query(SEARCH_CORE_NAME_A, queryRewriterSubHandler("rewriterA")).getResponse();
        assertThat(responseV1)
                .hasRewriterId("rewriterA")
                .hasClass(CommonRulesRewriterFactory.class.getName())
                .hasRules("a =>\n SYNONYM: aa");

        try (SolrCore searchCore = embeddedSolr.getCoreContainer().getCore(SEARCH_CORE_NAME_A)) {
            withCommonRulesRewriter(searchCore, "rewriterA", builderV2);
        }

        SolrDocumentList querqyConfigurationDocumentV2 = embeddedSolr.query(QUERQY_CONFIG_CORE_NAME, matchAll()).getResults();
        Assertions.assertThat(querqyConfigurationDocumentV2).hasSize(1);
        SolrDocument docV2 = querqyConfigurationDocumentV2.stream().findFirst().get();
        assertThat(docV2)
                .hasId(SolrCoreRewriterContainer.configurationDocumentId(SEARCH_CORE_NAME_A, "rewriterA"))
                .hasCoreName(SEARCH_CORE_NAME_A)
                .hasRewriterId("rewriterA")
                .hasData(builderV2.buildJson());

        NamedList<?> responseV2 = embeddedSolr.query(SEARCH_CORE_NAME_A, queryRewriterSubHandler("rewriterA")).getResponse();
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
        try (SolrCore searchCore = embeddedSolr.getCoreContainer().getCore(SEARCH_CORE_NAME_A)) {
            withCommonRulesRewriter(searchCore, "rewriterA", builderA);
        }
        try (SolrCore searchCore = embeddedSolr.getCoreContainer().getCore(SEARCH_CORE_NAME_B)) {
            withCommonRulesRewriter(searchCore, "rewriterB", builderB);
        }

        SolrDocumentList querqyConfigurationDocuments = embeddedSolr.query(QUERQY_CONFIG_CORE_NAME, matchAll()).getResults();
        Assertions.assertThat(querqyConfigurationDocuments).hasSize(2);

        SolrDocumentList querqyConfigurationDocumentA = embeddedSolr.query(QUERQY_CONFIG_CORE_NAME, matchRewriterConfigById("rewriterA", SEARCH_CORE_NAME_A)).getResults();
        Assertions.assertThat(querqyConfigurationDocumentA).hasSize(1);
        SolrDocument docA = querqyConfigurationDocumentA.stream().findFirst().get();
        assertThat(docA)
                .hasId(SolrCoreRewriterContainer.configurationDocumentId(SEARCH_CORE_NAME_A, "rewriterA"))
                .hasCoreName(SEARCH_CORE_NAME_A)
                .hasRewriterId("rewriterA")
                .hasData(builderA.buildJson());

        NamedList<?> responseA = embeddedSolr.query(SEARCH_CORE_NAME_A, queryRewriterSubHandler("rewriterA")).getResponse();
        assertThat(responseA)
                .hasRewriterId("rewriterA")
                .hasClass(CommonRulesRewriterFactory.class.getName())
                .hasRules("a =>\n SYNONYM: aa");

        SolrDocumentList querqyConfigurationDocumentB = embeddedSolr.query(QUERQY_CONFIG_CORE_NAME, matchRewriterConfigById("rewriterB", SEARCH_CORE_NAME_B)).getResults();
        Assertions.assertThat(querqyConfigurationDocumentB).hasSize(1);
        SolrDocument docB = querqyConfigurationDocumentB.stream().findFirst().get();
        assertThat(docB)
                .hasId(SolrCoreRewriterContainer.configurationDocumentId(SEARCH_CORE_NAME_B, "rewriterB"))
                .hasCoreName(SEARCH_CORE_NAME_B)
                .hasRewriterId("rewriterB")
                .hasData(builderB.buildJson());

        NamedList<?> responseB = embeddedSolr.query(SEARCH_CORE_NAME_B, queryRewriterSubHandler("rewriterB")).getResponse();
        assertThat(responseB)
                .hasRewriterId("rewriterB")
                .hasClass(CommonRulesRewriterFactory.class.getName())
                .hasRules("b =>\n SYNONYM: bb");
    }

    @Test
    public void deleteSingleRewriterConfiguration() throws Exception {
        final CommonRulesConfigRequestBuilder builder = new CommonRulesConfigRequestBuilder()
                .rules("a =>\n SYNONYM: aa");

        try (SolrCore searchCore = embeddedSolr.getCoreContainer().getCore(SEARCH_CORE_NAME_A)) {
            withCommonRulesRewriter(searchCore, "rewriterA", builder);
        }

        SolrDocumentList querqyConfigurationDocument = embeddedSolr.query(QUERQY_CONFIG_CORE_NAME, matchRewriterConfigById("rewriterA", SEARCH_CORE_NAME_A)).getResults();
        Assertions.assertThat(querqyConfigurationDocument).hasSize(1);

        try (SolrCore searchCore = embeddedSolr.getCoreContainer().getCore(SEARCH_CORE_NAME_A)) {
            deleteRewriter(searchCore, "rewriterA");
        }

        SolrDocumentList allConfigurationDocuments = embeddedSolr.query(QUERQY_CONFIG_CORE_NAME, new SolrQuery("*:*")).getResults();
        Assertions.assertThat(allConfigurationDocuments).isEmpty();

        SolrQuery queryRewriterA = queryRewriterSubHandler("rewriterA");
        SolrException exception = assertThrows(SolrException.class, () -> embeddedSolr.query(SEARCH_CORE_NAME_A, queryRewriterA));
        Assertions.assertThat(exception.getMessage()).contains("No such rewriter: rewriterA");
    }

    @Test
    public void deleteMultipleRewriterConfigurations() throws Exception {
        final CommonRulesConfigRequestBuilder builderA = new CommonRulesConfigRequestBuilder()
                .rules("a =>\n SYNONYM: aa");
        final CommonRulesConfigRequestBuilder builderB = new CommonRulesConfigRequestBuilder()
                .rules("b =>\n SYNONYM: bb");

        // Add a rewriter config for each core
        try (SolrCore searchCore = embeddedSolr.getCoreContainer().getCore(SEARCH_CORE_NAME_A)) {
            withCommonRulesRewriter(searchCore, "rewriterA", builderA);
        }
        try (SolrCore searchCore = embeddedSolr.getCoreContainer().getCore(SEARCH_CORE_NAME_B)) {
            withCommonRulesRewriter(searchCore, "rewriterB", builderB);
        }

        SolrDocumentList querqyConfigurationDocumentA = embeddedSolr.query(QUERQY_CONFIG_CORE_NAME, matchRewriterConfigById("rewriterA", SEARCH_CORE_NAME_A)).getResults();
        Assertions.assertThat(querqyConfigurationDocumentA).hasSize(1);

        SolrDocumentList querqyConfigurationDocumentB = embeddedSolr.query(QUERQY_CONFIG_CORE_NAME, matchRewriterConfigById("rewriterB", SEARCH_CORE_NAME_B)).getResults();
        Assertions.assertThat(querqyConfigurationDocumentB).hasSize(1);

        try (SolrCore searchCore = embeddedSolr.getCoreContainer().getCore(SEARCH_CORE_NAME_A)) {
            deleteRewriter(searchCore, "rewriterA");
        }

        SolrQuery queryRewriterA = queryRewriterSubHandler("rewriterA");
        SolrException exceptionA = assertThrows(SolrException.class, () -> embeddedSolr.query(SEARCH_CORE_NAME_A, queryRewriterA));
        Assertions.assertThat(exceptionA.getMessage()).contains("No such rewriter: rewriterA");

        SolrDocumentList allConfigurationDocuments = embeddedSolr.query(QUERQY_CONFIG_CORE_NAME, matchAll()).getResults();
        Assertions.assertThat(allConfigurationDocuments).hasSize(1);

        try (SolrCore searchCore = embeddedSolr.getCoreContainer().getCore(SEARCH_CORE_NAME_B)) {
            deleteRewriter(searchCore, "rewriterB");
        }

        SolrQuery queryRewriterB = queryRewriterSubHandler("rewriterB");
        SolrException exceptionB = assertThrows(SolrException.class, () -> embeddedSolr.query(SEARCH_CORE_NAME_B, queryRewriterB));
        Assertions.assertThat(exceptionB.getMessage()).contains("No such rewriter: rewriterB");

        allConfigurationDocuments = embeddedSolr.query(QUERQY_CONFIG_CORE_NAME, matchAll()).getResults();
        Assertions.assertThat(allConfigurationDocuments).isEmpty();
    }

    @Test
    public void getUnknownRewriterConfiguration() {
        SolrQuery queryRewriter = queryRewriterSubHandler("rewriterX");
        SolrException exception = assertThrows(SolrException.class, () -> embeddedSolr.query(SEARCH_CORE_NAME_A, queryRewriter));
        Assertions.assertThat(exception.getMessage()).contains("No such rewriter: rewriterX");
    }

    @Test
    public void deleteUnknownRewriterConfiguration() {
        try (SolrCore searchCore = embeddedSolr.getCoreContainer().getCore(SEARCH_CORE_NAME_A)) {
            SolrException solrException = assertThrows(SolrException.class, () -> deleteRewriter(searchCore, "rewriterX"));
            assertEquals("No such rewriter: rewriterX", solrException.getMessage());
        }
    }

    private static SolrQuery matchAll() {
        return new SolrQuery("*:*");
    }

    private static SolrQuery matchRewriterConfigById(String rewriterId, String coreName) {
        return new SolrQuery(String.format("id:%s", configurationDocumentId(coreName, rewriterId)));
    }

    private static SolrQuery queryRewriterSubHandler(String rewriterId) {
        return new SolrQuery("qt", "/querqy/rewriter/" + rewriterId);
    }

    private static void startSolr(Path solrHome) {
        NodeConfig nodeConfig = newNodeConfigBuilder(solrHome).build();
        coreContainer = new CoreContainer(nodeConfig);
        embeddedSolr = new EmbeddedSolrServer(coreContainer, null);
        coreContainer.load();
    }

    private static Path prepareSolrHomeWithConfigsets(String configsetsResourceFolder) throws Exception {
        Path solrHome = SolrTestCase.createTempDir("solrHome");
        Path configsetsDirectory = solrHome.resolve("configsets");
        Files.createDirectory(configsetsDirectory);
        Path configsetResourcePath = resourcePath(configsetsResourceFolder);
        FileUtils.copyDirectory(configsetResourcePath.toFile(), configsetsDirectory.toFile());
        return solrHome;
    }

    private static Path resourcePath(String resource) throws Exception {
        return new File(Objects.requireNonNull(SolrCoreRewriterContainerTest.class.getClassLoader().getResource(resource)).toURI()).getAbsoluteFile().toPath();
    }

    private static void createCore(SolrClient solrClient, String name, String configset, String configFile, String schemaFile) throws SolrServerException, IOException {
        CoreAdminRequest.Create req = new CoreAdminRequest.Create();
        req.setCoreName(name);
        req.setInstanceDir(name);
        req.setConfigSet(configset);
        if (configFile != null) {
            req.setConfigName(configFile);
        }
        if (schemaFile != null) {
            req.setSchemaName(schemaFile);
        }
        req.process(solrClient);
    }

    private static void deleteCore(SolrClient solrClient, String name) throws SolrServerException, IOException {
        CoreAdminResponse response = CoreAdminRequest.unloadCore(name, true, true, solrClient);
        System.out.println("Core " + name + " deleted with status " + response.getStatus());
    }

    private static NodeConfig.NodeConfigBuilder newNodeConfigBuilder(Path solrHome) {
        UpdateShardHandlerConfig shardHandlerConfig =
                new UpdateShardHandlerConfig(
                        HttpClientUtil.DEFAULT_MAXCONNECTIONS,
                        HttpClientUtil.DEFAULT_MAXCONNECTIONSPERHOST,
                        30000,
                        30000,
                        UpdateShardHandlerConfig.DEFAULT_METRICNAMESTRATEGY,
                        UpdateShardHandlerConfig.DEFAULT_MAXRECOVERYTHREADS);

        return new NodeConfig.NodeConfigBuilder("testNode", solrHome)
                .setUpdateShardHandlerConfig(shardHandlerConfig)
                .setCoreRootDirectory(SolrTestCase.createTempDir("cores").toString());
    }

    private static ReturnedRewriterConfigurationAssert assertThat(NamedList<?> actual) {
        return new ReturnedRewriterConfigurationAssert(actual);
    }

    private static StoredRewriterConfigurationAssert assertThat(SolrDocument actual) {
        return new StoredRewriterConfigurationAssert(actual);
    }

    private static class ReturnedRewriterConfigurationAssert extends AbstractAssert<ReturnedRewriterConfigurationAssert, NamedList<?>> {

        public ReturnedRewriterConfigurationAssert(NamedList<?> actual) {
            super(actual, SolrCoreRewriterContainerTest.ReturnedRewriterConfigurationAssert.class);
        }

        public ReturnedRewriterConfigurationAssert hasRewriterId(String expectedRewriterId) {
            Map<?, ?> rewriterMap = (Map<?, ?>) actual.get("rewriter");
            String actualRewriterId = (String) rewriterMap.get("id");
            if (!expectedRewriterId.equals(actualRewriterId)) {
                failWithMessage("Expected rewriter id to be <%s> but was <%s>", expectedRewriterId, actualRewriterId);
            }
            return this;
        }

        public ReturnedRewriterConfigurationAssert hasClass(String expectedClass) {
            Map<?, ?> rewriterMap = (Map<?, ?>) actual.get("rewriter");
            Map<?, ?> definition = (Map<?, ?>) rewriterMap.get("definition");
            String actualClass = (String) definition.get("class");
            if (!expectedClass.equals(actualClass)) {
                failWithMessage("Expected rewriter class to be <%s> but was <%s>", expectedClass, actualClass);
            }
            return this;
        }

        public ReturnedRewriterConfigurationAssert hasRules(String expectedRules) {
            Map<?, ?> rewriterMap = (Map<?, ?>) actual.get("rewriter");
            Map<?, ?> definition = (Map<?, ?>) rewriterMap.get("definition");
            Map<?, ?> config = (Map<?, ?>) definition.get("config");
            String actualRules = (String) config.get("rules");
            if (!expectedRules.equals(actualRules)) {
                failWithMessage("Expected rewriter rules to be <%s> but was <%s>", expectedRules, actualRules);
            }
            return this;
        }

    }

    private static class StoredRewriterConfigurationAssert extends AbstractAssert<StoredRewriterConfigurationAssert, SolrDocument> {

        public StoredRewriterConfigurationAssert(SolrDocument actual) {
            super(actual, StoredRewriterConfigurationAssert.class);
        }

        public StoredRewriterConfigurationAssert hasId(String id) {
            Assertions.assertThat(actual.getFieldValue("id")).isEqualTo(id);
            return this;
        }

        public StoredRewriterConfigurationAssert hasCoreName(String coreName) {
            Assertions.assertThat(actual.getFieldValue("core")).isEqualTo(coreName);
            return this;
        }

        public StoredRewriterConfigurationAssert hasRewriterId(String rewriterId) {
            Assertions.assertThat(actual.getFieldValue("rewriterId")).isEqualTo(rewriterId);
            return this;
        }

        public StoredRewriterConfigurationAssert hasData(String jsonData) {
            Assertions.assertThat(new String((byte[]) actual.getFieldValue("data"), StandardCharsets.UTF_8)).isEqualTo(jsonData);
            return this;
        }
    }
}


