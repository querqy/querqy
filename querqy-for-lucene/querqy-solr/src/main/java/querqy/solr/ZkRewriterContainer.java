package querqy.solr;

import static querqy.solr.utils.JsonUtil.*;

import org.apache.solr.cloud.ZkController;
import org.apache.solr.cloud.ZkSolrResourceLoader;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.rest.ManagedResourceStorage;
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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

public class ZkRewriterContainer extends RewriterContainer<ZkSolrResourceLoader> {

    public static final int DEFAULT_MAX_FILE_SIZE = 1000000;
    public static final String CONF_MAX_FILE_SIZE = "zkMaxFileSize";

    protected static final String IO_PATH = "querqy/rewriters";
    protected static final String IO_DATA = ".data";

    private String inventoryPath;
    private String dataPath;
    private SolrZkClient zkClient = null;
    private HashMap<String, RewriterWatcher> rewriterWatchers;
    private int maxFileSize = DEFAULT_MAX_FILE_SIZE; // TODO: Is 1 MB decimal or binary in ZK?

    protected ZkRewriterContainer(final SolrCore core, final ZkSolrResourceLoader resourceLoader) {
        super(core, resourceLoader);
        rewriterWatchers = new HashMap<>();
    }

    @Override
    protected void init(@SuppressWarnings({"rawtypes"}) NamedList args) {

        maxFileSize = NamedListWrapper
                .create(args, "Error in ZkRewriterContainer config")
                .getOrDefaultInteger(CONF_MAX_FILE_SIZE, DEFAULT_MAX_FILE_SIZE);

        final ZkController zkController = resourceLoader.getZkController();
        zkClient = zkController.getZkClient();
        final String collection = core.getCoreDescriptor().getCollectionName();

        final String zkConfigName;
        try {
            zkConfigName = zkController.getZkStateReader().readConfigName(collection);
        } catch (final Exception e) {
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Failed to load config name for collection:" +
                    collection  + " due to: ", e);
        }

        inventoryPath = "/configs/" + zkConfigName + "/" + IO_PATH;
        dataPath = inventoryPath + "/" + IO_DATA;

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

        // Paths in Storage IO are relative to 'basePath'
        final ManagedResourceStorage.StorageIO storageIO = ManagedResourceStorage.newStorageIO(core
                .getCoreDescriptor().getCollectionName(), resourceLoader, new NamedList<>());

        final List<String> uuids = new ArrayList<>();

        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream(maxFileSize)) {

            try (final GZIPOutputStream gzOut = new GZIPOutputStream(bos)) {
                JsonUtil.writeJson(instanceDescription, gzOut);
            }


            final byte[] bytes = bos.toByteArray();
            int offset = 0;

            while (offset < bytes.length) {
                final String uuid = UUID.randomUUID().toString();
                try (final OutputStream os = storageIO.openOutputStream(IO_PATH + "/" + IO_DATA + "/" + rewriterId +
                        "-" + uuid)) {
                    final int len = Math.min(maxFileSize, bytes.length - offset);
                    os.write(bytes, offset, len);
                    offset += len;
                    uuids.add(uuid);
                }
            }
        }

        final Stat stat;
        try {
            stat = zkClient.exists(inventoryPath + "/" + rewriterId, null, true);
        } catch (final InterruptedException | KeeperException e) {
            throw new IOException("Error saving rewriter " + rewriterId, e);
        }

        try {

            if (stat == null) {

                try {
                    zkClient.makePath(inventoryPath + "/" + rewriterId, String.join(",", uuids).getBytes(),
                            CreateMode.PERSISTENT, null, true, true);
                } catch (KeeperException.NodeExistsException e) {
                    for (final String uuid : uuids) {
                        final String rewriterDataPath = rewriterDataPath(rewriterId, uuid);
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
                final String oldUuids = new String(zkClient.getData(inventoryPath + "/" + rewriterId, null, stat,
                        true));
                zkClient.setData(inventoryPath + "/" + rewriterId, String.join(",", uuids).getBytes(),
                        stat.getVersion(), true);
                for (final String oldUuid : oldUuids.split(",")) {
                    final String oldPath = rewriterDataPath(rewriterId, oldUuid);
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

    @Override
    protected void deleteRewriter(final String rewriterId) throws IOException {
        final String dataLoc;
        try {
            dataLoc = new String(zkClient.getData(rewriterPath(rewriterId), newRewriterWatcher(rewriterId),
                    null, true));
            zkClient.delete(rewriterPath(rewriterId), -1, true);

        } catch (final InterruptedException | KeeperException e) {
            throw new IOException("Error deleting rewriter " + rewriterId, e);
        }

        try {
            for (final String oldUuid : dataLoc.split(",")) {
                zkClient.delete(rewriterDataPath(rewriterId, oldUuid), -1, true);
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
                onDirectoryChanged();
                notifyRewritersChangeListener();
            }, true).stream() // get all children except for the .data subdirectory
                    .filter(child -> !IO_DATA.equals(child))
                    .collect(Collectors.toList());

        } catch (final Exception e) {
            // TODO: log
            return;
        }

        final Set<String> known = new HashSet<>(rewriters.keySet());

        for (final String rewriterId : children) {
            if (!known.remove(rewriterId)) {
                // unknown => new rewriter. This loads it and creates an updated 'rewriters' map
                onRewriterChanged(rewriterId);
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

    public synchronized void onRewriterChanged(final String rewriterId) {

        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream(maxFileSize)) {

            for (final String uuid : new String(zkClient.getData(rewriterPath(rewriterId),
                    newRewriterWatcher(rewriterId), null, true)).split(",")) {
                final byte[] data = zkClient.getData(rewriterDataPath(rewriterId, uuid), null, null, true);
                bos.write(data);
            }

            loadRewriter(rewriterId, readJson(GZIPAwareResourceLoader.detectGZIPAndWrap(
                    new ByteArrayInputStream(bos.toByteArray())), Map.class));

        } catch (final Exception e) {
            // TODO: log
        }

    }

    protected String rewriterPath(final String rewriterId) {
        return inventoryPath + "/" + rewriterId;
    }

    protected String rewriterDataPath(final String rewriterId, final String uuid) {
        return dataPath + "/" + rewriterId + "-" + uuid;
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
                onRewriterChanged(rewriterId);
                LOG.info("Rewriter changed: {}", rewriterId);
                notifyRewritersChangeListener();
            }
        }

        public void disable() {
            enabled = false;
        }
    }


}
