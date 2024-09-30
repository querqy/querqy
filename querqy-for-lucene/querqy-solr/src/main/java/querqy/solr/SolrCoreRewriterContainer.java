package querqy.solr;

import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CursorMarkParams;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.query.FilterQuery;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.response.DocsStreamer;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.search.*;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.CommitUpdateCommand;
import org.apache.solr.update.DeleteUpdateCommand;
import org.apache.solr.util.IOFunction;
import querqy.lucene.rewrite.infologging.Sink;
import querqy.solr.utils.JsonUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.solr.common.SolrException.ErrorCode.*;
import static querqy.solr.utils.CoreUtils.withCore;

/**
 * A RewriterContainer that persists the rewriter configuration in a Solr core.
 *
 * @author Matthias Krueger, Markus Schuch
 */
public class SolrCoreRewriterContainer extends RewriterContainer<SolrResourceLoader> {

    private static final String FIELD_DOC_ID = "id";
    private static final String FIELD_REWRITER_ID = "rewriterId";
    private static final String FIELD_CORE_NAME = "core";
    private static final String FIELD_DATA = "data";

    private boolean isFollower;

    private String rewriterConfigCoreName;

    public SolrCoreRewriterContainer(final SolrCore core, final SolrResourceLoader resourceLoader,
                                     final Map<String, Sink> infoLoggingSinks) {
        super(core, resourceLoader, infoLoggingSinks);
    }

    @Override
    protected void init(@SuppressWarnings({"rawtypes"}) NamedList args) {
        var initArgs = args.toSolrParams();
        this.rewriterConfigCoreName = initArgs.get("configCoreName", "querqy");
        this.isFollower = initArgs.getBool("isFollower", false);
        try {
            withConfigurationCore(core -> core.withSearcher(this::loadRewriters), Duration.ofMinutes(1));
        } catch (IOException e) {
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Could not load rewriter data", e);
        }
    }

    @Override
    protected void doClose() {
        // nothing to close
    }

    private synchronized List<String> loadRewriters(SolrIndexSearcher configurationSearcher) throws IOException {
        final List<String> storedRewriters = listStoredRewriterIds(configurationSearcher);
        for (final String rewriterId : storedRewriters) {
            loadRewriter(rewriterId, readRewriterDefinition(rewriterId, configurationSearcher));
        }
        return storedRewriters;
    }

    synchronized void reloadRewriterConfig(SolrIndexSearcher newConfigurationSearcher) throws IOException {
        final Set<String> previouslyLoadedRewriters = new HashSet<>(rewriters.keySet());
        final List<String> loadedRewriters = loadRewriters(newConfigurationSearcher);

        loadedRewriters.forEach(previouslyLoadedRewriters::remove);

        // rewriters left in 'previouslyLoadedRewriters' no longer exist in storage - do not keep them in the 'rewriters' map any longer
        // We do not manipulate the 'rewriters' map but replace it with an updated map to avoid locking/synchronization

        final Map<String, RewriterFactoryContext> newRewriters = new HashMap<>(rewriters);
        previouslyLoadedRewriters.forEach(rewriterId -> {
            LOG.info("Unloading rewriter: {}", rewriterId);
            newRewriters.remove(rewriterId);
        });
        rewriters = newRewriters;

        notifyRewritersChangeListener();
    }

    private List<String> listStoredRewriterIds(SolrIndexSearcher searcher) throws IOException {
        List<SolrDocument> rewriterConfigurationDocuments = allConfigurationDocuments(searcher, core.getName());
        return rewriterConfigurationDocuments.stream().map(this::getRewriterIdFromDocument).collect(Collectors.toList());
    }

    @Override
    public synchronized Map<String, Object> readRewriterDefinition(final String rewriterId)
            throws IOException {
        return withConfigurationCore(configurationCore -> configurationCore.withSearcher(searcher ->
                readRewriterDefinition(rewriterId, searcher)
        ));
    }

    private synchronized Map<String, Object> readRewriterDefinition(final String rewriterId, SolrIndexSearcher searcher)
            throws IOException {
        Optional<SolrDocument> rewriterConfigurationDocument = configurationDocumentForRewriter(searcher, core.getName(), rewriterId);
        return rewriterConfigurationDocument.map(this::readConfigurationFromDocument).orElseThrow(() -> new SolrException(NOT_FOUND, "No such rewriter: " + rewriterId));
    }

    @Override
    protected synchronized void doSaveRewriter(final String rewriterId, final Map<String, Object> instanceDescription)
            throws IOException {

        if (isFollower) {
            throw new SolrException(BAD_REQUEST, "Rewriter config must be updated via the leader");
        }

        withConfigurationCore(configurationCore -> {
            SolrInputDocument doc = new SolrInputDocument();
            doc.addField(FIELD_DOC_ID, configurationDocumentId(core.getName(), rewriterId));
            doc.addField(FIELD_REWRITER_ID, rewriterId);
            doc.addField(FIELD_CORE_NAME, core.getName());
            writeConfigurationToDocument(instanceDescription, doc);

            SolrParams requestParams = new MapSolrParams(Map.of(
                    "action", "update",
                    "name", this.rewriterConfigCoreName));

            try (LocalSolrQueryRequest solrUpdateRequest = new LocalSolrQueryRequest(configurationCore, requestParams)) {
                AddUpdateCommand addCmd = new AddUpdateCommand(solrUpdateRequest);
                addCmd.solrDoc = doc;
                configurationCore.getUpdateHandler().addDoc(addCmd);

                CommitUpdateCommand commitCmd = new CommitUpdateCommand(solrUpdateRequest, false);
                configurationCore.getUpdateHandler().commit(commitCmd);
            }
            return null;
        });

        loadRewriter(rewriterId, instanceDescription);
        notifyRewritersChangeListener();
    }

    @Override
    protected synchronized void deleteRewriter(final String rewriterId) throws IOException {

        if (isFollower) {
            throw new SolrException(BAD_REQUEST, "Rewriter config must be updated via the leader");
        }

        withConfigurationCore(configurationCore -> {
            SolrParams requestParams = new MapSolrParams(Map.of(
                    "action", "update",
                    "name", this.rewriterConfigCoreName));

            try (LocalSolrQueryRequest solrUpdateRequest = new LocalSolrQueryRequest(configurationCore, requestParams)) {
                DeleteUpdateCommand deleteCmd = new DeleteUpdateCommand(solrUpdateRequest);
                deleteCmd.id = configurationDocumentId(core.getName(), rewriterId);
                configurationCore.getUpdateHandler().delete(deleteCmd);

                CommitUpdateCommand commitCmd = new CommitUpdateCommand(solrUpdateRequest, false);
                configurationCore.getUpdateHandler().commit(commitCmd);
            }
            return null;
        });

        final Map<String, RewriterFactoryContext> newRewriters = new HashMap<>(rewriters);
        if ((newRewriters.remove(rewriterId) == null)) {
            throw new SolrException(NOT_FOUND, "No such rewriter: " + rewriterId);
        }
        rewriters = newRewriters;

        notifyRewritersChangeListener();
    }

    protected String getRewriterIdFromDocument(SolrDocument doc) {
        var fieldValue = doc.getFieldValue(FIELD_REWRITER_ID);
        if (fieldValue == null) {
            throw new SolrException(INVALID_STATE, String.format("Unexpected null value encountered in field '%s'", FIELD_REWRITER_ID));
        } else if (fieldValue instanceof IndexableField) {
            return ((IndexableField) fieldValue).stringValue();
        } else {
            throw new SolrException(INVALID_STATE, String.format("Unexpected field type '%s' encountered in field '%s'", fieldValue.getClass().getName(), FIELD_REWRITER_ID));
        }
    }

    protected Map<String, Object> readConfigurationFromDocument(SolrDocument doc) {
        var fieldValue = doc.getFieldValue(FIELD_DATA);
        if (fieldValue == null) {
            throw new SolrException(INVALID_STATE, String.format("Unexpected null value encountered in field '%s'", FIELD_DATA));
        } else if (fieldValue instanceof IndexableField) {
            var byteRef = ((IndexableField) fieldValue).binaryValue();
            return JsonUtil.readMapFromJson(byteRef.utf8ToString());
        } else {
            throw new SolrException(INVALID_STATE, String.format("Unexpected field type '%s' encountered in field '%s'", fieldValue.getClass().getName(), FIELD_DATA));
        }
    }

    protected void writeConfigurationToDocument(Map<String, Object> configuration, SolrInputDocument document) {
        var byteArrayOutputStream = new ByteArrayOutputStream();
        JsonUtil.writeJson(configuration, byteArrayOutputStream);
        document.addField(FIELD_DATA, byteArrayOutputStream.toByteArray());
    }

    protected Optional<SolrDocument> configurationDocumentForRewriter(SolrIndexSearcher searcher, String coreName, String rewriterId) throws IOException {
        try (LocalSolrQueryRequest req = new LocalSolrQueryRequest(searcher.getCore(), new ModifiableSolrParams())) {

            QueryResult result = new QueryResult();

            QueryCommand queryCommand = new QueryCommand();
            queryCommand.setQuery(new TermQuery(new Term(FIELD_DOC_ID, configurationDocumentId(coreName, rewriterId))));
            queryCommand.setLen(1);

            searcher.search(result, queryCommand);

            SolrDocumentList docs = toSolrDocumentList(result.getDocList(),
                    searcher,
                    searcher.getSchema(),
                    new SolrReturnFields(
                            new String[]{
                                    FIELD_DOC_ID,
                                    FIELD_REWRITER_ID,
                                    FIELD_CORE_NAME,
                                    FIELD_DATA
                            },
                            req
                    )
            );

            return docs.stream().findFirst();
        }
    }

    protected List<SolrDocument> allConfigurationDocuments(SolrIndexSearcher searcher, String coreName) throws IOException {
        ArrayList<SolrDocument> configurationDocuments = new ArrayList<>();
        try (LocalSolrQueryRequest req = new LocalSolrQueryRequest(searcher.getCore(), new ModifiableSolrParams())) {
            final ResponseBuilder rsp = new ResponseBuilder(req,
                    new SolrQueryResponse(),
                    Collections.emptyList());

            rsp.setSortSpec(new SortSpec(null, Collections.emptyList()));

            QueryResult result = new QueryResult();

            SortSpec sortSpec = SortSpecParsing.parseSortSpec(String.format("%s asc", FIELD_DOC_ID), searcher.getSchema());

            QueryCommand queryCommand = new QueryCommand();
            queryCommand.setFilterList(new FilterQuery(new TermQuery(new Term(FIELD_CORE_NAME, coreName))));
            queryCommand.setLen(100);
            queryCommand.setSort(sortSpec.getSort());

            CursorMark cursorMark = new CursorMark(searcher.getSchema(), sortSpec);
            cursorMark.parseSerializedTotem(CursorMarkParams.CURSOR_MARK_START);

            while (true) {

                queryCommand.setCursorMark(cursorMark);
                searcher.search(result, queryCommand);

                SolrDocumentList docs = toSolrDocumentList(result.getDocList(),
                        searcher,
                        searcher.getSchema(),
                        new SolrReturnFields(
                                new String[]{
                                        FIELD_REWRITER_ID,
                                },
                                req
                        )
                );

                configurationDocuments.addAll(docs);

                CursorMark nextCursorMark = result.getNextCursorMark();

                if (nextCursorMark == null || nextCursorMark.getSerializedTotem().equals(cursorMark.getSerializedTotem())) {
                    // cursor mark remained the same meaning the
                    // search does not produce more results
                    break;
                }

                cursorMark = nextCursorMark;
            }
            LOG.info("Fetched {} rewriter configuration documents", configurationDocuments.size());
            return configurationDocuments;
        }
    }

    private static SolrDocumentList toSolrDocumentList(
            DocList docList,
            IndexSearcher searcher,
            IndexSchema schema,
            ReturnFields fields) throws IOException {
        SolrDocumentList solrDocuments = new SolrDocumentList();
        Iterator<Integer> docIds = docList.iterator();
        while (docIds.hasNext()) {
            solrDocuments.add(DocsStreamer.convertLuceneDocToSolrDoc(
                    searcher.doc(docIds.next()),
                    schema,
                    fields));
        }
        return solrDocuments;
    }

    private <R> R withConfigurationCore(IOFunction<SolrCore, R> lambda) throws IOException {
        return withCore(lambda, rewriterConfigCoreName, core.getCoreContainer());
    }

    private <R> void withConfigurationCore(IOFunction<SolrCore, R> lambda, Duration waitTimeout) throws IOException {
        withCore(lambda, rewriterConfigCoreName, core.getCoreContainer(), waitTimeout);
    }

    static String configurationDocumentId(String coreName, String rewriterId) {
        return coreName + "#" + rewriterId;
    }

}
