package querqy.solr.explain;

import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.StandaloneSolrTestSupport.withCommonRulesRewriter;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.BeforeClass;
import org.junit.Test;
import querqy.rewrite.lookup.preprocessing.LookupPreprocessorType;
import querqy.solr.rewriter.commonrules.CommonRulesConfigRequestBuilder;

@SolrTestCaseJ4.SuppressSSL
public class ExplainRewriteChainTest  extends SolrTestCaseJ4 {

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig.xml", "schema.xml");
    }

    @Test
    public void testExplainChain() {
        final String rewriterName = "conf_common_rules";
        final CommonRulesConfigRequestBuilder builder = new CommonRulesConfigRequestBuilder()
                .rules("a =>\n SYNONYM: b\nSYNONYM(2.5): f1:c\nFILTER: x\nUP(100): pears")
                .lookupPreprocessorType(LookupPreprocessorType.LOWERCASE);
        withCommonRulesRewriter(h.getCore(), rewriterName, builder);



        try (final SolrQueryRequest req = req(
                "qt", "/querqy/rewriter/_explain/chain",
                PARAM_REWRITERS, rewriterName,
                "q", "a"
        )) {

            assertQ("Error explaining chain",
                    req,
                    "//lst[@name='explain']/str[@name='query_string'][text()='a']",

                    "//lst[@name='MATCHING_QUERY'][1]//arr[@name='clauses']//lst[1]//bool[@name='generated'][text()='true']",
                    "//lst[@name='MATCHING_QUERY'][1]//arr[@name='clauses']//lst[1]//str[@name='field'][text()='f1']",
                    "//lst[@name='MATCHING_QUERY'][1]//arr[@name='clauses']//lst[1]//float[@name='boost'][text()='2.5']",

                    "//lst[@name='BOOST_QUERIES']//bool[@name='generated'][text()='true']",
                    "//lst[@name='BOOST_QUERIES']//str[@name='value'][text()='pears']",
                    "//lst[@name='BOOST_QUERIES']//float[@name='factor'][text()='100.0']",

                    "//arr[@name='FILTER_QUERIES']//str[@name='value'][text()='x']",
                    "//arr[@name='FILTER_QUERIES']//bool[@name='generated'][text()='true']"
            );

        }

    }
}
