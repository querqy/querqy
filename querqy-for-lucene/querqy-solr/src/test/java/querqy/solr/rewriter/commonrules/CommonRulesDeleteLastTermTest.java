package querqy.solr.rewriter.commonrules;

import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.StandaloneSolrTestSupport.withCommonRulesRewriter;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.BeforeClass;
import org.junit.Test;

@SolrTestCaseJ4.SuppressSSL
public class CommonRulesDeleteLastTermTest extends SolrTestCaseJ4 {

    public static void index() {

        assertU(adoc("id", "1", "f1", "a"));
        assertU(adoc("id", "2", "f1", "a"));
        assertU(adoc("id", "3", "f1", "b"));
        assertU(adoc("id", "4", "f1", "b"));

        assertU(commit());
    }

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig.xml", "schema.xml");
        withCommonRulesRewriter(h.getCore(), "common_rules", "configs/commonrules/rules-delete-last-term.txt");
        index();

    }

    @Test
    public void testThatQueryDoesNotMatchIfContainsBoostDownQueryAndHasNoTerm() {
        String q = "e";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "debugQuery", "on",
                "defType", "querqy",
                PARAM_REWRITERS, "common_rules"
        );

        assertQ("",
                req,
                "//result[@name='response' and @numFound='0']"
        );
        req.close();

    }

    @Test
    public void testThatMatchAllQueryIsAppliedIfQueryContainsBoostUpQueryAndHasNoTerm() {
        String q = "d";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "debugQuery", "on",
                "defType", "querqy",
                PARAM_REWRITERS, "common_rules");

        assertQ("",
                req,
                "//result[@name='response' and @numFound='4']"
        );
        req.close();

    }

    @Test
    public void testThatMatchAllQueryIsAppliedIfQueryContainsFilterQueryAndHasNoTerm() {
        String q = "c";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "debugQuery", "on",
                "defType", "querqy",
                PARAM_REWRITERS, "common_rules");

        assertQ("",
                req,
                "//result[@name='response' and @numFound='4']"
        );
        req.close();

    }

    @Test
    public void testNoMatchAfterDeletingLastTerm() {
        String q = "b";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "debugQuery", "on",
                "defType", "querqy",
                PARAM_REWRITERS, "common_rules");

        assertQ("",
                req,
                "//result[@name='response' and @numFound='0']"
        );
        req.close();

    }

    @Test
    public void testMatchOnlyByGeneratedTerm() {
        String q = "a";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "debugQuery", "on",
                "defType", "querqy",
                PARAM_REWRITERS, "common_rules");

        assertQ("",
                req,
                "//result[@name='response' and @numFound='2']"
        );
        req.close();

    }


}

