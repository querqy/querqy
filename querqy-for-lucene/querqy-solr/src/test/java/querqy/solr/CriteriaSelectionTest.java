package querqy.solr;


import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import querqy.rewrite.commonrules.SimpleCommonRulesRewriterFactory;

@SolrTestCaseJ4.SuppressSSL
public class CriteriaSelectionTest extends SolrTestCaseJ4 {

    public void index() {

        assertU(adoc("id", "1", "f1", "syn1"));
        assertU(adoc("id", "2", "f1", "syn2"));
        assertU(adoc("id", "3", "f1", "syn3"));

        assertU(commit());
    }

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig-commonrules-criteria.xml", "schema.xml");
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        clearIndex();
        index();
    }

    @Test
    public void testDefaultSelectionStrategy() {
        SolrQueryRequest req = req("q", "input1 input2",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                "defType", "querqy",
                "debugQuery", "true"
        );

        assertQ("default SelectionStrategy doesn't work",
                req,
                "//result[@name='response' and @numFound='3']"
        );

        req.close();
    }

    @Test
    public void testDefaultSelectionStrategyIsUsedRegardlessOfCriteria() {
        SolrQueryRequest req = req("q", "input1 input2",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                // we set the criteria but don't enable the strategy
                "rules.criteria.filter", "group:1",
                "rules.criteria.sort", "priority asc",
                "rules.criteria.limit", "1",
                "defType", "querqy",
                "debugQuery", "true"
        );

        assertQ("default SelectionStrategy doesn't work",
                req,
                "//result[@name='response' and @numFound='3']"
        );

        req.close();
    }

    @Test
    public void testThatDefaultStrategyIsAppliedIfNoCriteriaParamIsSet() {
        SolrQueryRequest req = req("q", "input1 input2",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                SimpleCommonRulesRewriterFactory.PARAM_SELECTION_STRATEGY, "criteria",
                "defType", "querqy",
                "debugQuery", "true"
        );

        assertQ("default SelectionStrategy doesn't work",
                req,
                "//result[@name='response' and @numFound='3']"
        );

        req.close();
    }

    @Test
    public void testFilterByProperty() {
        SolrQueryRequest req = req("q", "input1 input2",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                SimpleCommonRulesRewriterFactory.PARAM_SELECTION_STRATEGY, "criteria",
                "rules.criteria.filter", "group:1",
                "defType", "querqy",
                "debugQuery", "true"
        );

        assertQ("Filter criterion doesn't work",
                req,
                "//result[@name='response' and @numFound='2']"
        );

        req.close();
    }

    @Test
    public void testSortingAndLimiting() {
        SolrQueryRequest req = req("q", "input1 input2",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                SimpleCommonRulesRewriterFactory.PARAM_SELECTION_STRATEGY, "criteria",
                "rules.criteria.filter", "group:1",
                "rules.criteria.sort", "priority asc",
                "rules.criteria.limit", "1",
                "defType", "querqy",
                "debugQuery", "true"
        );

        assertQ("Sorting/limit not working",
                req,
                "//result[@name='response' and @numFound='1']",
                "//result/doc/str[@name='id'][text()='2']"
        );

        req.close();
    }

}
