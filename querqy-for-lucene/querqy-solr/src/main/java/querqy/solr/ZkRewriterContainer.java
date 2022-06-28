package querqy.solr;

import static java.nio.charset.StandardCharsets.UTF_8;
import static querqy.solr.utils.JsonUtil.*;

import org.apache.solr.cloud.ZkController;
import org.apache.solr.cloud.ZkSolrResourceLoader;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;
import querqy.lucene.GZIPAwareResourceLoader;
import querqy.rewrite.RewriterFactory;
import querqy.solr.utils.JsonUtil;
import querqy.solr.utils.NamedListWrapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

public class ZkRewriterContainer extends RewriterContainer<ZkSolrResourceLoader> {

    public static final int DEFAULT_MAX_FILE_SIZE = 1000000;
    public static final String CONF_MAX_FILE_SIZE = "zkMaxFileSize";
    public static final String CONF_CONFIG_NAME = "zkConfigName";
    public static final String CONF_CONFIG_DATA_DIR = "zkDataDirectory";

    protected static final String IO_PATH = "querqy/rewriters";
    protected static final String IO_DATA = ".data";
    protected static final String DEFAULT_REWRITER_DATA_DIR = ".data";

    private String inventoryPath;
    private String dataPath;
    private String dataDirectory;
    private SolrZkClient zkClient = null;
    private HashMap<String, RewriterWatcher> rewriterWatchers;
    private int maxFileSize = DEFAULT_MAX_FILE_SIZE; // TODO: Is 1 MB decimal or binary in ZK?

    protected ZkRewriterContainer(final SolrCore core, final ZkSolrResourceLoader resourceLoader) {
        super(core, resourceLoader);
        rewriterWatchers = new HashMap<>();
    }

    @Override
    protected void init(@SuppressWarnings({"rawtypes"}) final NamedList argsList) {

        final NamedListWrapper args = NamedListWrapper.create(argsList, "Error in ZkRewriterContainer config");

        maxFileSize = args.getOrDefaultInteger(CONF_MAX_FILE_SIZE, DEFAULT_MAX_FILE_SIZE);

        final String configuredDir = args.getStringOrDefault(CONF_CONFIG_DATA_DIR, DEFAULT_REWRITER_DATA_DIR);
        if (configuredDir.contains("..") || configuredDir.indexOf('/') > -1) {
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Invalid value for config property "
                    + CONF_CONFIG_DATA_DIR + ": " + configuredDir);
        }

        dataDirectory = configuredDir;

        final ZkController zkController = resourceLoader.getZkController();
        zkClient = zkController.getZkClient();
        final String collection = core.getCoreDescriptor().getCollectionName();

        final String zkConfigName;
        try {
            zkConfigName = args.getStringOrDefault(CONF_CONFIG_NAME,
                    zkController.getZkStateReader().readConfigName(collection));
        } catch (final Exception e) {
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Failed to load config name for collection:" +
                    collection  + " due to: ", e);
        }

        inventoryPath = "/configs/" + zkConfigName + "/" + IO_PATH;
        dataPath = inventoryPath + "/" + dataDirectory;

        try {
            // TODO: add watcher?
            zkClient.makePath(dataPath, false, true);
        } catch (final Exception e) {
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
                    "Could not assure rewriter config path in ZK");
        }

        onDirectoryChanged();

    }

    @Override
    protected synchronized void doClose() {
        rewriterWatchers.values().forEach(RewriterWatcher::disable);
        rewriterWatchers = null;
        zkClient = null;
    }

    @Override
    protected void doSaveRewriter(final String rewriterId, final Map<String, Object> instanceDescription)
            throws IOException {

        if (rewriterId.startsWith(".")) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Rewriter ID must not start with '.'");
        }

        if (rewriterId.equals(dataDirectory)) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Rewriter ID must not equal configured " +
                    "property " +  CONF_CONFIG_DATA_DIR);
        }

        final List<String> uuids = new ArrayList<>();

        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream(maxFileSize)) {

            try (final GZIPOutputStream gzOut = new GZIPOutputStream(bos)) {
                JsonUtil.writeJson(instanceDescription, gzOut);
            }


            final byte[] bytes = bos.toByteArray();
            int offset = 0;

            while (offset < bytes.length) {
                final String uuid = UUID.randomUUID().toString();
                final String path = rewriterDataPath(rewriterId, dataDirectory, uuid);
                final int len = Math.min(maxFileSize, bytes.length - offset);
                
                try {
                    zkClient.makePath(path, Arrays.copyOfRange(bytes, offset, (offset + len)), true);
                    offset += len;
                    uuids.add(uuid);
                } catch (InterruptedException | KeeperException e) {
                    throw new IOException("Error saving rewriter data for " + rewriterId, e);        
                }
            }
        }

        final String rewriterStorageInfoNode = rewriterStorageInfoNode(rewriterId);
        final Stat stat;
        try {
            stat = zkClient.exists(rewriterStorageInfoNode, null, true);
        } catch (final InterruptedException | KeeperException e) {
            throw new IOException("Error saving rewriter " + rewriterId, e);
        }

        try {

            final byte[] infoData = new RewriterStorageInfo(uuids, dataDirectory).toJsonString().getBytes(UTF_8);

            if (stat == null) {

                try {
                    zkClient.makePath(rewriterStorageInfoNode, infoData, CreateMode.PERSISTENT, null, true, true);
                } catch (KeeperException.NodeExistsException e) {
                    for (final String uuid : uuids) {
                        // undo saving parts
                        final String rewriterDataPath = rewriterDataPath(rewriterId, dataDirectory, uuid);
                        try {
                            zkClient.delete(rewriterDataPath, -1, true);
                        } catch (final Exception exception) {
                            throw new IOException("Rewriter " + rewriterId +
                                    " already exists. In addition, could not undo saving to " + rewriterDataPath,
                                    exception);

                        }
                    }
                    throw new IOException("Rewriter " + rewriterId + " already exists");
                }

            } else {

                final RewriterStorageInfo oldStorageInfo = RewriterStorageInfo.fromString(
                        new String(zkClient.getData(rewriterStorageInfoNode, null, stat, true)));

                zkClient.setData(rewriterStorageInfoNode, infoData, stat.getVersion(), true);

                for (final String oldUuid : oldStorageInfo.uuids) {
                    final String oldPath = rewriterDataPath(rewriterId, oldStorageInfo.dataDir, oldUuid);
                    try {
                        zkClient.delete(oldPath, -1, true);
                    } catch (final Exception e) {
                        LOG.error("Could not delete old rewriter data: " + oldPath, e);
                    }
                }
            }

        } catch (final InterruptedException | KeeperException e) {
            throw new IOException("Error saving rewriter " + rewriterId, e);
        }

    }

    protected RewriterStorageInfo readRewriterStorageInfo(final String rewriterId, final RewriterWatcher watcher) throws
            IOException {

        try {
            return RewriterStorageInfo.fromString(new String(zkClient.getData(rewriterStorageInfoNode(rewriterId), watcher,
                    null, true)));
        } catch (final KeeperException e) {
            if (KeeperException.Code.NONODE == e.code()) {
                throw new SolrException(SolrException.ErrorCode.NOT_FOUND, "Rewriter " + rewriterId + " not found.");
            } else {
                throw new IOException("Error getting rewriter " + rewriterId, e);
            }
        } catch (final InterruptedException e) {
            throw new IOException("Error getting rewriter " + rewriterId, e);
        }


    }

    @Override
    protected void deleteRewriter(final String rewriterId) throws IOException {

        if (rewriterId.startsWith(".")) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Rewriter ID must not start with '.'");
        }

        if (rewriterId.equals(dataDirectory)) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Rewriter ID must not equal configured " +
                    "property " +  CONF_CONFIG_DATA_DIR);
        }

        final RewriterStorageInfo storageInfo = readRewriterStorageInfo(rewriterId, newRewriterWatcher(rewriterId));

        try {
            zkClient.delete(rewriterStorageInfoNode(rewriterId), -1, true);
        } catch (final KeeperException e) {
            if (KeeperException.Code.NONODE == e.code()) {
                throw new SolrException(SolrException.ErrorCode.NOT_FOUND, "Rewriter " + rewriterId + " not found.");
            } else {
                throw new IOException("Error deleting rewriter " + rewriterId, e);
            }
        } catch (final InterruptedException e) {
            throw new IOException("Error deleting rewriter " + rewriterId, e);
        }

        try {
            for (final String oldUuid : storageInfo.uuids) {
                zkClient.delete(rewriterDataPath(rewriterId, storageInfo.dataDir, oldUuid), -1, true);
            }
        } catch (final InterruptedException | KeeperException e) {
            LOG.error("The rewriter " + rewriterId + " was deleted but not all data could be removed from ZK", e);
        }
    }

    protected synchronized void onDirectoryChanged() {

        final List<String> children;
        try {
            children = zkClient.getChildren(inventoryPath, event -> {
                // register a Watcher on the directory 
                // if we're not closed yet
                if (zkClient != null) {
                    onDirectoryChanged();
                    notifyRewritersChangeListener();
                }
            }, true).stream() // get all children except for the .data subdirectory
                    .filter(child -> !(child.startsWith(".")
                            || (child.equals(dataDirectory)
                            || (child.startsWith("__")))))
                    .collect(Collectors.toList());

        } catch (final Exception e) {
            LOG.error("Error handling onDirectoryChanged", e);
            return;
        }

        final Set<String> known = new HashSet<>(rewriters.keySet());

        for (final String rewriterId : children) {
            if (!known.remove(rewriterId)) {
                // unknown => new rewriter. This loads it and creates an updated 'rewriters' map
                try {
                    onRewriterChanged(rewriterId);
                } catch (final Exception e) {
                    LOG.error("Error loading rewriter " + rewriterId, e);
                }
            }
        }

        // rewriters in 'known' no longer exist in Zk - do not keep them in the 'rewriters' map any longer

        // We do not manipulate the 'rewriters' map but replace it with an updated map to avoid locking/synchronization

        final Map<String, RewriterFactory> newRewriters = new HashMap<>(rewriters);
        for (final String rewriterId : known) {
            LOG.info("Unloading rewriter: {}", rewriterId);
            newRewriters.remove(rewriterId);
            final RewriterWatcher oldWatcher = rewriterWatchers.remove(rewriterId);
            if (oldWatcher != null) {
                oldWatcher.disable();
            }
        }
        rewriters = newRewriters;

    }

    public synchronized void onRewriterChanged(final String rewriterId) throws Exception {

        loadRewriter(rewriterId, readRewriterDefinition(rewriterId, newRewriterWatcher(rewriterId)));

    }

    @Override
    public synchronized Map<String, Object> readRewriterDefinition(final String rewriterId)
            throws IOException {
        return readRewriterDefinition(rewriterId, null);
    }

    protected synchronized Map<String, Object> readRewriterDefinition(final String rewriterId,
                                                                      final RewriterWatcher watcher)
            throws IOException {

        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream(maxFileSize)) {

            final RewriterStorageInfo storageInfo = readRewriterStorageInfo(rewriterId, watcher);

            for (final String uuid : storageInfo.uuids) {
                try {
                    final byte[] data = zkClient.getData(rewriterDataPath(rewriterId, storageInfo.dataDir, uuid), null,
                            null, true);
                    bos.write(data);
                } catch (final KeeperException e) {
                    if (KeeperException.Code.NONODE == e.code()) {
                        throw new SolrException(SolrException.ErrorCode.NOT_FOUND, "Rewriter data not found: " +
                                storageInfo.dataDir + "/" + uuid);
                    } else {
                        throw new IOException(e);
                    }
                } catch (final InterruptedException e) {
                    throw new IOException(e);
                }
            }

            return readJson(GZIPAwareResourceLoader.detectGZIPAndWrap(new ByteArrayInputStream(bos.toByteArray())),
                    Map.class);

        }
    }

    protected String rewriterStorageInfoNode(final String rewriterId) {
        return inventoryPath + "/" + rewriterId;
    }

    protected String rewriterDataPath(final String rewriterId, final String dataDir, final String uuid) {
        return inventoryPath + "/" + dataDir + "/" + rewriterId + "-" + uuid;
    }

    protected synchronized RewriterWatcher newRewriterWatcher(final String rewriterId) {
        final RewriterWatcher newWatcher = new RewriterWatcher(rewriterId);
        final RewriterWatcher oldWatcher = rewriterWatchers.put(rewriterId, newWatcher);
        if (oldWatcher != null) {
            oldWatcher.disable();
        }
        return newWatcher;
    }

    protected class RewriterWatcher implements Watcher {

        final String rewriterId;
        boolean enabled = true;

        RewriterWatcher(final String rewriterId) {
            this.rewriterId = rewriterId;
        }

        @Override
        public void process(final WatchedEvent event) {
            if (enabled) {
                try {
                    onRewriterChanged(rewriterId);
                } catch (final Exception e) {
                    LOG.error("Error processing WatchedEvent for rewriter " + rewriterId, e);
                    return;
                }
                LOG.info("Rewriter changed: {}", rewriterId);
                notifyRewritersChangeListener();
            }
        }

        public void disable() {
            enabled = false;
        }
    }

    public static final class RewriterStorageInfo {

        private static final String PROP_VERSION = "_version";
        private static final String PROP_DATA_DIR = "data_dir";
        private static final String PROP_UUIDS = "uuids";
        public static final int CURRENT_VERSION = 2;

        public final List<String> uuids;
        public final String dataDir;

        public RewriterStorageInfo(final List<String> uuids, final String dataDir) {
            this.uuids = uuids;
            this.dataDir = dataDir;
        }

        static RewriterStorageInfo fromString(final String str) {

            if (str == null) {
                throw new IllegalArgumentException("Cannot read RewriterStorageInfo from null");
            }

            final String data = str.trim();
            if (data.isEmpty()) {
                throw new IllegalArgumentException("Cannot read RewriterStorageInfo from empty data");
            }

            if (data.charAt(0) == '{') {
                final Map<String, Object> dataMap = readMapFromJson(data);
                final Integer version = (Integer) dataMap.get(PROP_VERSION);
                if (version == null) {
                    throw new IllegalStateException("Missing version info in RewriterStorageInfo");
                }
                if (version != CURRENT_VERSION) {
                    throw new IllegalStateException("Cannot handle RewriterStorageInfo version: " + version);
                }

                final String dataPath = (String) dataMap.getOrDefault(PROP_DATA_DIR, DEFAULT_REWRITER_DATA_DIR);
                final List<String> uuids = (List<String>) dataMap.get(PROP_UUIDS);
                if ((uuids == null) || uuids.isEmpty()) {
                    throw new IllegalStateException("Missing node ids in RewriterStorageInfo");
                }
                return new RewriterStorageInfo(uuids, dataPath);

            } else {
                return new RewriterStorageInfo(Arrays.asList(data.split(",")), DEFAULT_REWRITER_DATA_DIR);
            }
        }

        public String toJsonString() {
            final Map<String, Object> data = new LinkedHashMap<>();
            data.put(PROP_VERSION, CURRENT_VERSION);
            data.put(PROP_DATA_DIR, dataDir);
            data.put(PROP_UUIDS, uuids);
            return JsonUtil.toJson(data);
        }

    }


}
