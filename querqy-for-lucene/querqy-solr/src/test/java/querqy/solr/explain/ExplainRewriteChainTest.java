package querqy.solr.explain;


import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.StandaloneSolrTestSupport.withCommonRulesRewriter;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import querqy.solr.QuerqyQParserPlugin;
import querqy.solr.rewriter.commonrules.CommonRulesConfigRequestBuilder;

@SolrTestCaseJ4.SuppressSSL
public class ExplainRewriteChainTest  extends SolrTestCaseJ4 {

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig.xml", "schema.xml");


    }

    @Test
    public void testExplainChain() {
        // FIXME: complete test
        final String rewriterName = "conf_common_rules";
        final CommonRulesConfigRequestBuilder builder = new CommonRulesConfigRequestBuilder()
                .rules("a =>\n SYNONYM: b\nFILTER: x\nUP(100): pears").ignoreCase(true);
        withCommonRulesRewriter(h.getCore(), rewriterName, builder);



        try (final SolrQueryRequest req = req(
                "qt", "/querqy/rewriter/_explain/chain",
                PARAM_REWRITERS, rewriterName,
                "q", "a"
        )) {

            assertQ("Error explaining chain",
                    req,
                    "//lst[@name='explain']/str[@name='query_string'][text()='a']"
            );

        }

    }
}
