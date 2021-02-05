package querqy.solr;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.response.SolrResponseBase;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.ShardRequest;
import org.apache.solr.handler.component.ShardResponse;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryRequestBase;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.apache.solr.handler.component.ResponseBuilder.STAGE_DONE;
import static org.apache.solr.handler.component.ResponseBuilder.STAGE_EXECUTE_QUERY;
import static org.assertj.core.util.Lists.emptyList;
import static querqy.solr.QuerqyQueryComponent.QUERQY_DECORATIONS;
import static querqy.solr.QuerqyQueryComponent.QUERQY_NAMED_DECORATIONS;
import static querqy.solr.ResponseSink.QUERQY_INFO_LOG;

@SolrTestCaseJ4.SuppressSSL
public class QuerqyQueryComponentTest extends SolrTestCaseJ4 {

    private QuerqyQueryComponent component;
    private SolrQueryRequest request;
    private ResponseBuilder rb;
    private NamedList<Object> namedList;
    private ShardRequest shardRequest;

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig.xml", "schema.xml");
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();

        component = new QuerqyQueryComponent();
        request = new SolrQueryRequestBase(h.getCore(), new ModifiableSolrParams()) {
        };
        rb = new ResponseBuilder(request, new SolrQueryResponse(), emptyList());
        namedList = new NamedList<>();
        shardRequest = new ShardRequest();

        ShardResponse srsp = new ShardResponse();
        SolrResponseBase shardQueryResponse = new SolrResponseBase();
        shardQueryResponse.setResponse(namedList);
        srsp.setSolrResponse(shardQueryResponse);
        shardRequest.responses.add(srsp);
    }

    @Test
    public void testIncorrectStage() {
        rb.stage = STAGE_DONE;
        namedList.add("foo", "bar");

        component.handleResponses(rb, shardRequest);

        assertNull(rb.rsp.getValues().get("foo"));
    }

    @Test
    public void testCorrectStageButIncorrectKey() {
        rb.stage = STAGE_EXECUTE_QUERY;
        namedList.add("foo", "bar");

        component.handleResponses(rb, shardRequest);

        assertNull(rb.rsp.getValues().get("foo"));
    }

    @Test
    public void testCorrectStage() {
        rb.stage = STAGE_EXECUTE_QUERY;
        namedList.add(QUERQY_INFO_LOG, "bar1");
        namedList.add(QUERQY_DECORATIONS, "bar2");
        namedList.add(QUERQY_NAMED_DECORATIONS, "bar3");

        component.handleResponses(rb, shardRequest);

        assertEquals("bar1", rb.rsp.getValues().get(QUERQY_INFO_LOG));
        assertEquals("bar2", rb.rsp.getValues().get(QUERQY_DECORATIONS));
        assertEquals("bar3", rb.rsp.getValues().get(QUERQY_NAMED_DECORATIONS));
    }

    @Test
    public void testCorrectStageAndOnlyFirstIsValid() {
        rb.stage = STAGE_EXECUTE_QUERY;
        namedList.add(QUERQY_INFO_LOG, "bar1");

        ShardResponse srsp2 = new ShardResponse();
        SolrResponseBase rspb2 = new SolrResponseBase();
        NamedList<Object> namedListSecondShard = new NamedList<>();
        namedList.add(QUERQY_INFO_LOG, "bar2");
        rspb2.setResponse(namedListSecondShard);
        srsp2.setSolrResponse(rspb2);
        shardRequest.responses.add(srsp2);

        component.handleResponses(rb, shardRequest);

        assertEquals(1, rb.rsp.getValues().size());
        assertEquals("bar1", rb.rsp.getValues().get(QUERQY_INFO_LOG));
    }
}