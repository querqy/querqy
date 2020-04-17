package querqy.solr;


import static org.mockito.Mockito.when;

import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import querqy.infologging.InfoLogging;
import querqy.parser.QuerqyParser;
import querqy.rewrite.RewriteChain;

import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class DismaxSearchEngineRequestAdapterTest {

    @Mock
    QParser qParser;

    @Mock
    SolrQueryRequest request;

    @Mock
    QuerqyParser querqyParser;

    @Mock
    RewriteChain rewriteChain;

    @Mock
    InfoLogging infoLogging;

    @Test
    public void testGetDoubleParam() {

        final String name = "p1";

        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set(name, "0.5");

        when(request.getSchema()).thenReturn(null);

        final DismaxSearchEngineRequestAdapter adapter = new DismaxSearchEngineRequestAdapter(qParser, request,
                "some query", params, querqyParser, rewriteChain, infoLogging, null);

        Assert.assertEquals(0.5, adapter.getDoubleRequestParam(name).get(), 0.001);
        Assert.assertFalse(adapter.getDoubleRequestParam("p2").isPresent());

    }


    @Test
    public void testGetFloatParam() {

        final String name = "p1";

        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set(name, "-0.03");

        when(request.getSchema()).thenReturn(null);

        final DismaxSearchEngineRequestAdapter adapter = new DismaxSearchEngineRequestAdapter(qParser, request,
                "some query", params, querqyParser, rewriteChain, infoLogging, null);

        Assert.assertEquals(-0.03f, adapter.getFloatRequestParam(name).get(), 0.001f);
        Assert.assertFalse(adapter.getFloatRequestParam("p2").isPresent());

    }

    @Test
    public void testGetIntParam() {

        final String name = "p1";
        final int value = 42;

        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set(name, Integer.toString(value));

        when(request.getSchema()).thenReturn(null);

        final DismaxSearchEngineRequestAdapter adapter = new DismaxSearchEngineRequestAdapter(qParser, request,
                "some query", params, querqyParser, rewriteChain, infoLogging, null);

        Assert.assertEquals(value, adapter.getIntegerRequestParam(name).get().intValue());
        Assert.assertFalse(adapter.getIntegerRequestParam("p2").isPresent());

    }

    @Test
    public void testGetBooleanParam() {

        final String name1 = "p1";
        final String name2 = "p2";

        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set(name1, "true");
        params.set(name2, "false");

        when(request.getSchema()).thenReturn(null);

        final DismaxSearchEngineRequestAdapter adapter = new DismaxSearchEngineRequestAdapter(qParser, request,
                "some query", params, querqyParser, rewriteChain, infoLogging, null);

        Assert.assertTrue(adapter.getBooleanRequestParam(name1).get());
        Assert.assertFalse(adapter.getBooleanRequestParam(name2).get());
        Assert.assertFalse(adapter.getBooleanRequestParam("p3").isPresent());

    }

    @Test
    public void testGetParam() {

        final String name = "p1";
        final String value = "v1";

        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set(name, value);

        when(request.getSchema()).thenReturn(null);

        final DismaxSearchEngineRequestAdapter adapter = new DismaxSearchEngineRequestAdapter(qParser, request,
                "some query", params, querqyParser, rewriteChain, infoLogging, null);

        Assert.assertEquals(value, adapter.getRequestParam(name).get());
        Assert.assertFalse(adapter.getRequestParam("p2").isPresent());

    }


    @Test
    public void testGetParams() {

        final String name = "p1";
        final String value1 = "v1";
        final String value2 = "modelv2";

        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add(name, value1, value2);

        when(request.getSchema()).thenReturn(null);

        final DismaxSearchEngineRequestAdapter adapter = new DismaxSearchEngineRequestAdapter(qParser, request,
                "some query", params, querqyParser, rewriteChain, infoLogging, null);

        final String[] value = adapter.getRequestParams(name);
        Assert.assertNotNull(value);
        Assert.assertEquals(2, value.length);
        Assert.assertEquals(value1, value[0]);
        Assert.assertEquals(value2, value[1]);

        Assert.assertEquals(0, adapter.getRequestParams("p2").length);

    }

    @Test
    public void testThatFieldBoostingInQuerqyBoostQueriesIsOnByDefault() {
        ModifiableSolrParams params = new ModifiableSolrParams();
        when(request.getSchema()).thenReturn(null);
        final DismaxSearchEngineRequestAdapter adapter = new DismaxSearchEngineRequestAdapter(qParser, request,
                "some query", params, querqyParser, rewriteChain, infoLogging, null);
        Assert.assertTrue(adapter.useFieldBoostingInQuerqyBoostQueries());
    }

    @Test
    public void testSettingFieldBoostingInQuerqyBoostQueriesToOn() {
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add(QuerqyDismaxParams.QBOOST_FIELD_BOOST, QuerqyDismaxParams.QBOOST_FIELD_BOOST_ON);

        when(request.getSchema()).thenReturn(null);
        final DismaxSearchEngineRequestAdapter adapter = new DismaxSearchEngineRequestAdapter(qParser, request,
                "some query", params, querqyParser, rewriteChain, infoLogging, null);
        Assert.assertTrue(adapter.useFieldBoostingInQuerqyBoostQueries());
    }

    @Test
    public void testSettingFieldBoostingInQuerqyBoostQueriesToOff() {
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add(QuerqyDismaxParams.QBOOST_FIELD_BOOST, QuerqyDismaxParams.QBOOST_FIELD_BOOST_OFF);

        when(request.getSchema()).thenReturn(null);
        final DismaxSearchEngineRequestAdapter adapter = new DismaxSearchEngineRequestAdapter(qParser, request,
                "some query", params, querqyParser, rewriteChain, infoLogging, null);
        Assert.assertFalse(adapter.useFieldBoostingInQuerqyBoostQueries());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThatIllegalValueForFieldBoostingInQuerqyBoostThrowsException() {
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add(QuerqyDismaxParams.QBOOST_FIELD_BOOST, "maybe");

        when(request.getSchema()).thenReturn(null);
        final DismaxSearchEngineRequestAdapter adapter = new DismaxSearchEngineRequestAdapter(qParser, request,
                "some query", params, querqyParser, rewriteChain, infoLogging, null);
        adapter.useFieldBoostingInQuerqyBoostQueries();

    }

    @Test(expected = RuntimeException.class)
    public void testThatExceptionIsThrownIfThereAreNeitherQueryFieldsNorDefaultField() {
        ModifiableSolrParams params = new ModifiableSolrParams();

        when(request.getSchema()).thenReturn(null);
        final DismaxSearchEngineRequestAdapter adapter = new DismaxSearchEngineRequestAdapter(qParser, request,
                "some query", params, querqyParser, rewriteChain, infoLogging, null);
        adapter.parseQueryFields(DisMaxParams.QF, 1f, true);

    }

    @Test
    public void testThatParseQueryFieldsReturnsEmptyMapIfThereAreNoQueryFieldsAndDefaultFieldNotEnabled() {
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add(CommonParams.DF, "f1"); // setting default field but not enabling it
        final DismaxSearchEngineRequestAdapter adapter = new DismaxSearchEngineRequestAdapter(qParser, request,
                "some query", params, querqyParser, rewriteChain, infoLogging, null);
        Assert.assertTrue(adapter.parseQueryFields(DisMaxParams.QF, 1f, false).isEmpty());
    }

    @Test
    public void testThatParseQueryFieldsUsesDefaultFieldIfEnabled() {
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add(CommonParams.DF, "f1"); // setting default field but not enabling it
        final DismaxSearchEngineRequestAdapter adapter = new DismaxSearchEngineRequestAdapter(qParser, request,
                "some query", params, querqyParser, rewriteChain, infoLogging, null);
        final Map<String, Float> fields = adapter.parseQueryFields(DisMaxParams.QF, 2f, true);
        Assert.assertEquals(1, fields.size());
        Assert.assertEquals(2f, fields.get("f1").floatValue(), 0.001f);
    }



}
