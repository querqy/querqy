package querqy.solr;

import org.apache.solr.common.util.NamedList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static querqy.solr.QuerqyRewriterRequestHandler.RewriterStorageType.*;
import static querqy.solr.QuerqyRewriterRequestHandler.SolrMode.CLOUD;
import static querqy.solr.QuerqyRewriterRequestHandler.SolrMode.STANDALONE;

@RunWith(Parameterized.class)
public class RewriterStorageTypeTest {

    private static final NamedList<Object> OLD_IN_MEMORY_CONFIG = new NamedList<>();
    private static final NamedList<Object> IN_MEMORY_CONFIG = new NamedList<>();
    private static final NamedList<Object> CONF_DIR_CONFIG = new NamedList<>();
    private static final NamedList<Object> INDEX_CONFIG = new NamedList<>();
    private static final NamedList<Object> ZK_CONFIG = new NamedList<>();
    private static final NamedList<Object> EMPTY_CONFIG = new NamedList<>();
    private static final NamedList<Object> INVALID_CONFIG = new NamedList<>();

    static {
        OLD_IN_MEMORY_CONFIG.add("inMemory", true);
        IN_MEMORY_CONFIG.add("rewriterStorage", "inMemory");
        CONF_DIR_CONFIG.add("rewriterStorage", "confDir");
        INDEX_CONFIG.add("rewriterStorage", "index");
        ZK_CONFIG.add("rewriterStorage", "zk");
        INVALID_CONFIG.add("rewriterStorage", "invalid");
    }

    @Parameterized.Parameter()
    public NamedList<Object> config;

    @Parameterized.Parameter(1)
    public QuerqyRewriterRequestHandler.SolrMode solrMode;

    @Parameterized.Parameter(2)
    public QuerqyRewriterRequestHandler.RewriterStorageType expectedStorageType;

    @Parameterized.Parameter(3)
    public Class<? extends Throwable> expectedException;

    @Parameterized.Parameters(name = "{index}: Config {0}, SolrMode {1} -> Expected StorageType {2}, Expected Exception {3}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {EMPTY_CONFIG, STANDALONE, CONF_DIR, null},
                {EMPTY_CONFIG, CLOUD, ZK, null},
                {ZK_CONFIG, STANDALONE, null, IllegalArgumentException.class},
                {CONF_DIR_CONFIG, STANDALONE, CONF_DIR, null},
                {CONF_DIR_CONFIG, CLOUD, null, IllegalArgumentException.class},
                {INDEX_CONFIG, STANDALONE, INDEX, null},
                {INDEX_CONFIG, CLOUD, null, IllegalArgumentException.class},
                {IN_MEMORY_CONFIG, STANDALONE, IN_MEMORY, null},
                {IN_MEMORY_CONFIG, CLOUD, IN_MEMORY, null},
                {OLD_IN_MEMORY_CONFIG, STANDALONE, IN_MEMORY, null},
                {INVALID_CONFIG, STANDALONE, null, IllegalArgumentException.class}
        });
    }

    @Test
    public void rewriterStorageTypeConfiguration() {
        if (expectedException != null) {
            assertThrows(expectedException, () -> fromConfig(config, solrMode));
        } else {
            assertEquals(expectedStorageType, fromConfig(config, solrMode));
        }
    }
}
