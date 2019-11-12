package querqy.solr;


import static querqy.rewrite.commonrules.select.RuleSelectionParams.getFilterParamName;
import static querqy.rewrite.commonrules.select.RuleSelectionParams.getLimitParamName;
import static querqy.rewrite.commonrules.select.RuleSelectionParams.getSortParamName;
import static querqy.rewrite.commonrules.select.RuleSelectionParams.getStrategyParamName;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import querqy.rewrite.commonrules.select.SelectionStrategyFactory;

/**
 * Different from the {@link CriteriaSelectionTest}, this test uses the {@link ExpressionSelectionStrategyFactory}
 * as the default {@link SelectionStrategyFactory} instead of configuring it in
 * solrconfig-commonrules-criteria-defaults.xml.
 */
@SolrTestCaseJ4.SuppressSSL
public class CriteriaSelectionDefaultsTest extends SolrTestCaseJ4 {

    // matching CommonRulesRewriter in solrconfig-commonrules-criteria.xml:
    private static final String REWRITER_ID_1 = "rules1";
    private static final String REWRITER_ID_2 = "rules2";
    private static final String REWRITER_ID_3 = "rules3";

    public void index() {

        assertU(adoc("id", "1", "f1", "syn1"));
        assertU(adoc("id", "2", "f1", "syn2"));
        assertU(adoc("id", "3", "f1", "syn3"));
        assertU(adoc("id", "4", "f1", "syn4"));

        assertU(commit());
    }

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig-commonrules-criteria-defaults.xml", "schema.xml");
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        clearIndex();
        index();
    }

    @Test
    public void testDefaultBehaviourAppliesAllRules() {
        // definition order, no constraints expected
        SolrQueryRequest req = req("q", "input1 input2 input3 input4",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                "defType", "querqy",
                "debugQuery", "true"
        );

        assertQ("default SelectionStrategy doesn't work",
                req,
                "//result[@name='response' and @numFound='4']"
        );

        req.close();
    }

    @Test
    public void testThatExpressionStrategyIsTheDefaultSelectionStrategy() {
        SolrQueryRequest req = req("q", "input1 input2 input3 input4 input5 input6",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                // we set the criteria but don't enable the strategy
                getFilterParamName(REWRITER_ID_1), "$[?(@.priority == 2)]",
                getFilterParamName(REWRITER_ID_2), "$[?(@.group == 44)]",
                getFilterParamName(REWRITER_ID_3), "$[?(@._id == 'id6')]",

                getLimitParamName(REWRITER_ID_1), "1",
                getLimitParamName(REWRITER_ID_2), "1",
                getLimitParamName(REWRITER_ID_3), "1",
                "defType", "querqy",
                "debugQuery", "true"
        );

        assertQ("default SelectionStrategy=ExpressionStrategy doesn't work",
                req,
                "//result[@name='response' and @numFound='3']",
                "//result/doc/str[@name='id'][text()='1']",
                "//result/doc/str[@name='id'][text()='3']",
                "//result/doc/str[@name='id'][text()='4']"
        );

        req.close();
    }

    @Test
    public void testThatDefaultStrategyIsAppliedIfNoCriteriaParamIsSet() {
        SolrQueryRequest req = req("q", "input1 input2",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                getStrategyParamName(REWRITER_ID_1), "criteria",
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
                getStrategyParamName(REWRITER_ID_1), "criteria",
                getFilterParamName(REWRITER_ID_1), "group:1",
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
                getStrategyParamName(REWRITER_ID_1), "criteria",
                getFilterParamName(REWRITER_ID_1), "group:1",
                getSortParamName(REWRITER_ID_1), "priority asc",
                getLimitParamName(REWRITER_ID_1), "1",
                "defType", "querqy",
                "debugQuery", "true"
        );

        assertQ("PropertySorting/limit not working",
                req,
                "//result[@name='response' and @numFound='1']",
                "//result/doc/str[@name='id'][text()='2']"
        );

        req.close();
    }

    @Test
    public void testThatSelectionIsAppliedPerRewriter() {
        SolrQueryRequest req = req("q", "input4",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                getStrategyParamName(REWRITER_ID_1), "criteria",
                getStrategyParamName(REWRITER_ID_2), "criteria",
                getFilterParamName(REWRITER_ID_1), "group:4",
                getFilterParamName(REWRITER_ID_2), "group:44",
                "defType", "querqy",
                "debugQuery", "true"
        );

        assertQ("Rewriter selection not working",
                req,
                "//result[@name='response' and @numFound='2']",
                "//result/doc/str[@name='id'][text()='3']",
                "//result/doc/str[@name='id'][text()='4']"
        );

        req.close();


        req = req("q", "input4",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                getStrategyParamName(REWRITER_ID_1), "criteria",
                getStrategyParamName(REWRITER_ID_2), "criteria",
                getFilterParamName(REWRITER_ID_2), "group:4", // Flipping the groups between rewriters
                getFilterParamName(REWRITER_ID_1), "group:44",
                "defType", "querqy",
                "debugQuery", "true"
        );

        assertQ("Rewriter selection not working",
                req,
                "//result[@name='response' and @numFound='0']"
        );

        req.close();
    }

    @Test
    public void testJsonFilterEquality() {
        SolrQueryRequest req = req("q", "input5 input6",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                getFilterParamName(REWRITER_ID_3), "$[?(@.tenant)].tenant[?(@.enabled == true)]",
                "defType", "querqy",
                "debugQuery", "true"
        );

        assertQ("Json eq filter criterion doesn't work",
                req,
                "//result[@name='response' and @numFound='2']",
                "//result/doc/str[@name='id'][text()='1']",
                "//result/doc/str[@name='id'][text()='2']"
        );

        req.close();
    }

    @Test
    public void testJsonList() {
        SolrQueryRequest req = req("q", "input5 input6",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                getFilterParamName(REWRITER_ID_3), "$[?('a' in @.tt)]",
                "defType", "querqy",
                "debugQuery", "true"
        );

        assertQ("Json eq filter criterion doesn't work",
                req,
                "//result[@name='response' and @numFound='1']"
        );

        req.close();
    }

    @Test
    public void testJsonFilterEqualityAndGreaterThan() {
        SolrQueryRequest req = req("q", "input5 input6",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                getFilterParamName(REWRITER_ID_3), "$[?(@.tenant && @.priority > 5)].tenant[?(@.enabled == true)]",
                "defType", "querqy",
                "debugQuery", "true"
        );

        assertQ("Json eq and gt filter criterion doesn't work",
                req,
                "//result[@name='response' and @numFound='1']",
                "//result/doc/str[@name='id'][text()='1']"
        );

        req.close();
    }

    @Test(expected = Exception.class)
    public void testThatStrategyParamThrowsException() {
        SolrQueryRequest req = req("q", "input5 input6",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                getStrategyParamName(REWRITER_ID_3), "criteria",
                getFilterParamName(REWRITER_ID_3), "$[?(@.tenant && @.priority > 5)].tenant[?(@.enabled == true)]",
                "defType", "querqy",
                "debugQuery", "true"
        );

        assertQ("Json eq and gt filter criterion doesn't work",
                req,
                "//result[@name='response' and @numFound='1']",
                "//result/doc/str[@name='id'][text()='1']"
        );

        req.close();
    }

}
