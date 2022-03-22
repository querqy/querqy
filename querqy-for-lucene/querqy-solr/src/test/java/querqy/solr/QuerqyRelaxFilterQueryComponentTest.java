package querqy.solr;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.BeforeClass;
import org.junit.Test;
import querqy.solr.rewriter.commonrules.CommonRulesConfigRequestBuilder;

import java.io.IOException;

import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.StandaloneSolrTestSupport.withCommonRulesRewriter;

@SolrTestCaseJ4.SuppressSSL
public class QuerqyRelaxFilterQueryComponentTest extends SolrTestCaseJ4 {

    private static final String REWRITER_NAME = "common_rules";

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig-relax-filter.xml", "schema.xml");
        try {
            final CommonRulesConfigRequestBuilder builder = new CommonRulesConfigRequestBuilder()
                    .rules(QuerqyRelaxFilterQueryComponentTest.class.getClassLoader()
                            .getResourceAsStream("configs/commonrules/rules.txt"));
            builder.ignoreCase(true);
            withCommonRulesRewriter(h.getCore(), REWRITER_NAME, builder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        addDocs();
    }

    private static void addDocs() {
        assertU(adoc("id", "1", "f1", "a b c", "f2", "d"));
        assertU(adoc("id", "2", "f1", "a b c", "f2", "c"));
        assertU(adoc("id", "3", "f1", "a b c", "f2", "f"));
        assertU(adoc("id", "4", "f1", "gh i", "f2", "j"));
        assertU(adoc("id", "5", "f1", "gh i", "f2", "k"));
        assertU(adoc("id", "6", "f1", "l m", "f2", "n"));
        assertU(adoc("id", "7", "f1", "l m", "f2", "o"));
        assertU(adoc("id", "8", "f1", "abc no ise", "f2", "xyz"));
        assertU(adoc("id", "9", "f1", "abc", "f2", "not boosted"));
        assertU(adoc("id", "10", "f1", "a uvw", "f2", "not boosted"));
        assertU(adoc("id", "101", "f1", "qneg3 qneg4", "f2", "q a"));
        assertU(adoc("id", "102", "f1", "qneg3 qneg4", "f2", "q b"));
        assertU(adoc("id", "103", "f1", "qneg3 qneg4", "f2", "q c"));

        assertU(commit());
    }

    private String[] createParams(String q) {
        return new String[]{
                "q", q,
                DisMaxParams.QF, "f1",
                "fl", "id,score",
                DisMaxParams.MM, "100%",
                PARAM_REWRITERS, REWRITER_NAME,
                "debugQuery", "on",
                "defType", "querqy"
        };
    }

    @Test
    public void testRelaxFilterQueryFunction() {

        SolrQueryRequest req;

        // filter a -> * f2:c AND no match on id:10 (appended fq)-> numFound=1
        req = req(createParams("a b"));
        assertQ("", req,
                "//result[@name='response' and @numFound='1']");
        req.close();

        // no filter AND no match on id:10 (appended fq) -> numFound=3
        req = req(createParams("c"));
        assertQ("", req,
                "//result[@name='response' and @numFound='3']");
        req.close();

        // filter a -> * f2:c AND match on id:10 (appended fq) -> numFound=2
        req = req(createParams("a"));
        assertQ("", req,
                "//result[@name='response' and @numFound='2']");
        req.close();

    }

    @Test
    public void testRelaxFilterQueryToParsedFilterQueries() {

        String q = "qneg2";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, REWRITER_NAME
        );

        assertQ("Default RHS QuerqyParser fails",
                req,
                "//arr[@name='parsed_filter_queries']/str[contains(.,'f1:qneg') and contains(.,'-f1:k')]",
                "//arr[@name='parsed_filter_queries']/str[contains(.,'(') and contains(.,') id:10')]"
        );

        req.close();
    }

    @Test
    public void testRelaxFilterQueryCombinedWithNegativeFilter() {

        String q = "qneg3";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, REWRITER_NAME
        );

        assertQ("Combination with neg filter fails",
                req,
                "//result[@name='response' and @numFound='2']",
                "//arr[@name='parsed_filter_queries']/str[contains(.,'f1:qneg3') and contains(.,'-f2:c')]",
                "//arr[@name='parsed_filter_queries']/str[contains(.,'(') and contains(.,') id:10')]"
        );

        req.close();
    }

    @Test
    public void testRelaxFilterQueryCombinedWithOnlyNegativeFilter() {

        String q = "qneg4";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, REWRITER_NAME
        );

        assertQ("Combination with neg filter fails",
                req,
                "//result[@name='response' and @numFound='2']",
                "//arr[@name='parsed_filter_queries']/str[contains(.,'-f2:c')]",
                "//arr[@name='parsed_filter_queries']/str[not(contains(.,'(')) and not(contains(.,') id:10'))]"
        );

        req.close();
    }

}
