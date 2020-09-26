package querqy.solr;

import static querqy.solr.utils.JsonUtil.*;

import org.apache.solr.cloud.ZkController;
import org.apache.solr.cloud.ZkSolrResourceLoader;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.rest.ManagedResourceStorage;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import querqy.lucene.GZIPAwareResourceLoader;
import querqy.rewrite.RewriterFactory;
import querqy.solr.utils.JsonUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

public class ZkRewriterContainer extends RewriterContainer<ZkSolrResourceLoader> {

    protected static final String IO_PATH = "querqy/rewriters";

    private String basePath;
    private SolrZkClient zkClient = null;
    private HashMap<String, RewriterWatcher> rewriterWatchers;

    protected ZkRewriterContainer(final SolrCore core, final ZkSolrResourceLoader resourceLoader) {
        super(core, resourceLoader);
        rewriterWatchers = new HashMap<>();
    }

    @Override
    protected void init() {

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

        basePath = "/configs/" + zkConfigName + "/" + IO_PATH;

        try {
            // TODO: add watcher?
            zkClient.makePath(basePath, false, true);

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

        try (final OutputStream os = new GZIPOutputStream(storageIO.openOutputStream(IO_PATH + "/" + rewriterId))) {
            JsonUtil.writeJson(instanceDescription, os);
        }

    }

    @Override
    protected void deleteRewriter(final String rewriterId) throws IOException {
        try {
            zkClient.delete(rewriterPath(rewriterId), -1, true);
        } catch (final InterruptedException | KeeperException e) {
            throw new IOException("Error deleting rewriter " + rewriterId, e);
        }
    }

    protected synchronized void onDirectoryChanged() {

        final List<String> children;
        try {
            children = zkClient.getChildren(basePath, new Watcher() {
                @Override
                public void process(final WatchedEvent event) {
                    onDirectoryChanged();
                    notifyRewritersChangeListener();
                }
            }, true);
        } catch (final Exception e) {
            // TODO: log
            return;
        }

        final Set<String> known = new HashSet<>(rewriters.keySet());

        for (final String rewriterId : children) {
            if (!known.remove(rewriterId)) {
                onRewriterChanged(rewriterId);
            }
        }

        final Map<String, RewriterFactory> newRewriters = new HashMap<>(rewriters);
        for (final String rewriterId : known) {
            LOG.info("Unloading rewriter: {}", rewriterId);
            newRewriters.remove(rewriterId); // TODO: stop watchers
            final RewriterWatcher oldWatcher = rewriterWatchers.remove(rewriterId);
            if (oldWatcher != null) {
                oldWatcher.disable();
            }
        }
        rewriters = newRewriters;

    }

    public synchronized void onRewriterChanged(final String rewriterId) {

        try {
            final byte[] data = zkClient.getData(rewriterPath(rewriterId), newRewriterWatcher(rewriterId), null, true);

            loadRewriter(rewriterId, readJson(GZIPAwareResourceLoader.detectGZIPAndWrap(new ByteArrayInputStream(data)),
                    Map.class));

        } catch (final Exception e) {
            // TODO: log
        }

    }

    protected String rewriterPath(final String rewriterId) {
        return basePath + "/" + rewriterId;
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
