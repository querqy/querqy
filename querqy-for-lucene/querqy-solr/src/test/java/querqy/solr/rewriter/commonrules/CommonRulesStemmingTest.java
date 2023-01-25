package querqy.solr.rewriter.commonrules;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.BeforeClass;
import org.junit.Test;
import querqy.rewrite.lookup.preprocessing.LookupPreprocessorType;

import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.StandaloneSolrTestSupport.withCommonRulesRewriter;

public class CommonRulesStemmingTest extends SolrTestCaseJ4 {

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig.xml", "schema.xml");
        withCommonRulesRewriter(h.getCore(), "common", "configs/commonrules/rules-stemming.txt", LookupPreprocessorType.GERMAN);
    }

    @Test
    public void testThat_synonymIsApplied_forApplyingGermanPreprocessingOnRuleLookups() {
        String q = "handys";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "debugQuery", "on",
                "defType", "querqy",
                PARAM_REWRITERS, "common"
        );

        assertQ("",
                req,
                "//lst[@name='debug']/str[@name='parsedquery' and contains(text(),'smartphone')]"
        );
        req.close();
    }

    @Test
    public void testThat_germanNormalizationIsApplied_forApplyingGermanPreprocessingOnRuleLookups() {
        String q = "spuelmaschine";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                "debugQuery", "on",
                "defType", "querqy",
                PARAM_REWRITERS, "common"
        );

        assertQ("",
                req,
                "//lst[@name='debug']/str[@name='parsedquery' and contains(text(),'geschirrreiniger')]"
        );
        req.close();
    }


}
