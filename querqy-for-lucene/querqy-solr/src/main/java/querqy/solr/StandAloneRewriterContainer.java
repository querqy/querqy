package querqy.solr;

import static querqy.solr.utils.JsonUtil.readJson;
import static querqy.solr.utils.JsonUtil.writeJson;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.rest.ManagedResourceStorage;
import querqy.rewrite.RewriterFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class StandAloneRewriterContainer extends RewriterContainer<SolrResourceLoader> {

    protected static final String IO_PATH = "querqy/rewriters";

    public StandAloneRewriterContainer(final SolrCore core, final SolrResourceLoader resourceLoader) {
        super(core, resourceLoader);
    }

    @Override
    protected void init(@SuppressWarnings({"rawtypes"}) NamedList args) {

        final File configDir = new File(resourceLoader.getConfigDir());
        final File querqyDir = new File(configDir, IO_PATH);
        if (querqyDir.exists()) {
            if (!(querqyDir.isDirectory() || querqyDir.canWrite())) {
                throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
                        "Not a writable directory: " + querqyDir.getAbsolutePath());
            }

            Arrays.stream(Objects.requireNonNull(querqyDir.listFiles(File::isFile))).forEach(file -> {
                final String rewriterId = file.getName();
                try {
                    loadRewriter(rewriterId, readJson(new FileInputStream(file), Map.class));
                } catch (final FileNotFoundException e) {
                    throw new RuntimeException("Could not load rewriter: " + rewriterId, e);
                }


            });

        } else {
            if (!querqyDir.mkdirs()) {
                throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
                        "Could not create " + querqyDir.getAbsolutePath());
            }
        }

    }

    @Override
    public synchronized Map<String, Object> readRewriterDescription(final String rewriterId)
            throws IOException {

        final ManagedResourceStorage.StorageIO storageIO = ManagedResourceStorage.newStorageIO(core.getCoreDescriptor()
                .getCollectionName(), resourceLoader, new NamedList<>());

        return readJson(storageIO.openInputStream(rewriterPath(rewriterId)), Map.class);

    }

    @Override
    protected void doClose() {
    }

    @Override
    protected synchronized void doSaveRewriter(final String rewriterId, final Map<String, Object> instanceDescription)
            throws IOException {

        final ManagedResourceStorage.StorageIO storageIO = ManagedResourceStorage.newStorageIO(core
                .getCoreDescriptor().getCollectionName(), resourceLoader, new NamedList<>());

        try (final OutputStream os = storageIO.openOutputStream(rewriterPath(rewriterId))) {
            writeJson(instanceDescription, os);
        }

        loadRewriter(rewriterId, instanceDescription);
        notifyRewritersChangeListener();
    }

    @Override
    protected synchronized void deleteRewriter(final String rewriterId) throws IOException {

        final String rewriterPath = rewriterPath(rewriterId);

        final ManagedResourceStorage.StorageIO storageIO = ManagedResourceStorage.newStorageIO(core
                .getCoreDescriptor().getCollectionName(), resourceLoader, new NamedList<>());

        final Map<String, RewriterFactory> newRewriters = new HashMap<>(rewriters);
        if ((newRewriters.remove(rewriterId) == null) && !storageIO.exists(rewriterPath)) {
            throw new SolrException(SolrException.ErrorCode.NOT_FOUND, "No such rewriter: " + rewriterId);
        }

        rewriters = newRewriters;
        storageIO.delete(rewriterPath);
        notifyRewritersChangeListener();

    }

    protected String rewriterPath(final String rewriterId) {
        return IO_PATH + "/" + rewriterId;
    }

}
