package querqy.solr;

import static querqy.model.convert.builder.BooleanQueryBuilder.bq;
import static querqy.solr.QuerqyDismaxParams.GFB;
import static querqy.solr.QuerqyDismaxParams.GQF;
import static querqy.solr.QuerqyDismaxParams.MULTI_MATCH_TIE;
import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.StandaloneSolrTestSupport.withCommonRulesRewriter;
import static querqy.solr.StandaloneSolrTestSupport.withRewriter;

import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.HighlightParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.WrappedQuery;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import querqy.infologging.MultiSinkInfoLogging;
import querqy.model.BoostQuery;
import querqy.model.ExpandedQuery;
import querqy.model.MatchAllQuery;
import querqy.model.rewriting.RewriterOutput;
import querqy.model.Term;
import querqy.model.convert.builder.BoostQueryBuilder;
import querqy.model.convert.builder.StringRawQueryBuilder;
import querqy.parser.WhiteSpaceQuerqyParser;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriteChain;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.commonrules.model.BoostInstruction;

import java.util.*;

@SolrTestCaseJ4.SuppressSSL
public class QuerqyDismaxQParserPluginTest extends SolrTestCaseJ4 {

    public void index() {

        assertU(adoc("id", "1", "f1", "a"));
        assertU(adoc("id", "2", "f1", "a"));
        assertU(adoc("id", "3", "f2", "a"));
        assertU(adoc("id", "4", "f1", "b"));
        assertU(adoc("id", "5", "f1", "spellcheck", "f2", "test"));
        assertU(adoc("id", "6", "f1", "spellcheck filtered", "f2", "test"));
        assertU(adoc("id", "7", "f1", "aaa"));
        assertU(adoc("id", "8", "f1", "aaa bbb ccc", "f2", "w87"));
        assertU(adoc("id", "9", "f1", "ignore o u s z1"));
        assertU(adoc("id", "10", "f1", "vv uu tt ss xx ff gg hh"));
        assertU(adoc("id", "11", "f1", "xx yy zz tt ll ff gg hh"));
        assertU(adoc("id", "12", "f1", "x1 x2 x3 y1 m1"));
        assertU(adoc("id", "13", "f1", "x1 y1 z1 k1 m1"));
        assertU(adoc("id", "14", "f1", "multboost"));
        assertU(adoc("id", "15", "f1", "multboost multup1"));
        assertU(adoc("id", "16", "f1", "multboost multup1 multup2"));
        assertU(adoc("id", "17", "f1", "multboost multdown"));
        assertU(adoc("id", "18", "f1", "multboost multup1 multdown"));

        assertU(commit());
    }

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig.xml", "schema.xml");
        withCommonRulesRewriter(h.getCore(), "common_rules",
                "configs/commonrules/rules-QuerqyDismaxQParserTest.txt");
        withRewriter(h.getCore(), "match_all_filter", MatchAllRewriter.class);
        withRewriter(h.getCore(), "boost_mult_rewriter", MultiplicativeBoostRewriter.class);
        withCommonRulesRewriter(h.getCore(), "common_rules_multiplicative",
                "configs/commonrules/rules-QuerqyDismaxQParserTest.txt", BoostInstruction.BoostMethod.MULTIPLICATIVE);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        clearIndex();
        index();
    }

    @Test
    public void testLocalParams() {
        SolrQueryRequest req = req("q", "{!querqy qf='f1 f2'}a b");

        assertQ("local params don't work",
             req,"//result[@name='response' and @numFound='4']");

        req.close();
    }

    @Test
    public void testThatFilterRulesFromCollationDontEndUpInMainQuery() {

        SolrQueryRequest req0 = req("q", "spellcheck test",
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "2",

                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules"
                );
        assertQ("Filter expected",
                req0,
                "//result[@name='response' and @numFound='1']",
                "//arr[@name='parsed_filter_queries']/str[text() = 'f1:filtered']"
                );

        req0.close();

        SolrQueryRequest req = req("q", "spellhceck test",
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "2",
                "spellcheck.collate", "true",
                "spellcheck.maxCollations", "1",
                "spellcheck.maxCollationTries", "10",
                "spellcheck", "true",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules"
                );

        assertQ("Combination with collations doesn't work",
                req,
                "//result[@name='response' and @numFound='0']",
                "//str[@name='collation'][text() = 'spellcheck test']",
                "not(//arr[@name='parsed_filter_queries']/str[text() = 'f1:filtered'])"
                );

           req.close();
    }

    @Test
    public void testThatAMMof2getsSetFor3optionalClauses() throws Exception {

        SolrQueryRequest req = req("q", "a b c",
            DisMaxParams.QF, "f1 f2",
            DisMaxParams.MM, "2"
        );

        QuerqyDismaxQParser parser = new QuerqyDismaxQParser("a b c", null, req.getParams(), req,
            new WhiteSpaceQuerqyParser(), new RewriteChain(), new MultiSinkInfoLogging(Collections.emptyMap()), null);

        Query query = parser.parse();

        req.close();

        BooleanQuery bq = assertBQ(query);

        assertEquals(2, bq.getMinimumNumberShouldMatch());
    }

    @Test
    public void testThatMMIsAppliedWhileQueryContainsMUSTBooleanOperators() throws Exception {

        String q = "a +b c +d e f";

        SolrQueryRequest req = req("q", q,
            DisMaxParams.QF, "f1 f2",
            DisMaxParams.MM, "3");

        QuerqyDismaxQParser parser = new QuerqyDismaxQParser(q, null, req.getParams(), req,
            new WhiteSpaceQuerqyParser(), new RewriteChain(), new MultiSinkInfoLogging(Collections.emptyMap()), null);

        Query query = parser.parse();

        req.close();

        BooleanQuery bq = assertBQ(query);
        assertEquals(3, bq.getMinimumNumberShouldMatch());
    }

    @Test
    public void testThatMMIsNotAppliedWhileQueryContainsMUSTNOTBooleanOperator() throws Exception {

        String q = "a +b c -d e f";

        SolrQueryRequest req = req("q", q,
            DisMaxParams.QF, "f1 f2",
            DisMaxParams.MM, "3",
            QueryParsing.OP, "OR"
            );
        QuerqyDismaxQParser parser = new QuerqyDismaxQParser(q, null, req.getParams(), req,
            new WhiteSpaceQuerqyParser(), new RewriteChain(), new MultiSinkInfoLogging(Collections.emptyMap()), null);
        Query query = parser.parse();

        req.close();

        BooleanQuery bq = assertBQ(query);
        assertEquals(0, bq.getMinimumNumberShouldMatch());
    }

    @Test
    public void testMMTurningAllOptionalClausesIntoMust() throws Exception {

        String q = "aaa bbb";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                "defType", "querqy",
                DisMaxParams.MM, "2",
                "uq.similarityScore", "dfc",
                PARAM_REWRITERS, "common_rules"
        );


        assertQ("MM all should clauses doesn't work",
                req,
                "//result[@numFound='1']");

        req.close();


    }

    @Test
    public void testThatPfIsAppliedOnlyToExistingField() {

        String q = "a b c d";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "3",
                QueryParsing.OP, "OR",
                DisMaxParams.PF, "f3^2 f40^0.5",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules"
            );

        // f4 doesn't exist
        assertQ("wrong ps",
            req,
            "//str[@name='parsedquery'][contains(.,'PhraseQuery(f3:\"a b c d\")^2.0')]",
            "//str[@name='parsedquery'][not(contains(.,'PhraseQuery(f40:\"a b c d\")^0.5'))]");

        req.close();

    }

   @Test
   public void testThatMatchAllDoesNotThrowException() {
      String q = "*:*";

      SolrQueryRequest req = req("q", q,
              DisMaxParams.QF, "f1 f2",
              DisMaxParams.MM, "3",
              QueryParsing.OP, "OR",
              DisMaxParams.PF, "f3^2 f4^0.5",
              "defType", "querqy",
              "debugQuery", "true",
              PARAM_REWRITERS, "common_rules"
            );

      assertQ("Matchall fails",
            req,
            "//str[@name='parsedquery'][contains(.,'*:*')]",
            "//result[@name='response' and @numFound='18']"

      );

      req.close();
   }

    @Test
    public void testThatMultiMatchTieIsApplied() {

        String q = "x1 y1 z1";

        try (SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "1",
                QueryParsing.OP, "OR",
                DisMaxParams.TIE,  "1.0",
                MULTI_MATCH_TIE, "1.0",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules")) {

            assertQ("Test without MultiMatchTie failed",
                    req,
                    "//result[@name='response' and @numFound='3']",
                    "//result/doc[1]/str[@name='id'][text()='12']",
                    "//result/doc[2]/str[@name='id'][text()='13']",
                    "//result/doc[3]/str[@name='id'][text()='9']"
                    );
        }
        try (SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "1",
                QueryParsing.OP, "OR",
                DisMaxParams.TIE,  "1.0",
                MULTI_MATCH_TIE, "0.0",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules")) {

            assertQ("Test with MultiMatchTie failed",
                    req,
                    "//result[@name='response' and @numFound='3']",
                    "//result/doc[1]/str[@name='id'][text()='13']",
                    "//result/doc[2]/str[@name='id'][text()='12']",
                    "//result/doc[3]/str[@name='id'][text()='9']"
            );
        }
    }


   @Test
   public void testThatPfIsAppliedOnlyToFieldsWithTermPositions() {

      String q = "a b c d";
      SolrQueryRequest req = req("q", q,
              DisMaxParams.QF, "f1 f2",
              DisMaxParams.MM, "3",
              QueryParsing.OP, "OR",
              "defType", "querqy",
              "debugQuery", "true",
              PARAM_REWRITERS, "common_rules",
              DisMaxParams.PF, "f1^1.2 str^2 f_no_tfp^0.5");

      // str is a string field
      // f_no_tfp / f_no_tp don't have term positions
      assertQ("wrong ps2",
            req,
            "//str[@name='parsedquery'][contains(.,'PhraseQuery(f1:\"a b c d\")^1.2')]",
            "//str[@name='parsedquery'][not(contains(.,'PhraseQuery(str:\"a b c d\")^2.0'))]",
            "//str[@name='parsedquery'][not(contains(.,'PhraseQuery(f_no_tfp:\"a b c d\")^0.5'))]",
            "//str[@name='parsedquery'][not(contains(.,'PhraseQuery(f_no_tp:\"a b c d\")^0.5'))]");

      req.close();

   }

    @Test
    public void testThatAnalysisIsRunForPf() {

        String q = "K L M";
        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "3",
                QueryParsing.OP, "OR",
                "defType", "querqy",
                "debugQuery", "true",
                DisMaxParams.PF, "f1_lc f2_lc",
                PARAM_REWRITERS, "common_rules");


        assertQ("Analysis not applied for pf",
                req,
                // Query terms should be lower-cased in analysis for pf fields
                "//str[@name='parsedquery'][contains(.,'DisjunctionMaxQuery((f1_lc:\"k l m\" | f2_lc:\"k l m\"))')] or " +
                        "//str[@name='parsedquery'][contains(.,'DisjunctionMaxQuery((f2_lc:\"k l m\" | f1_lc:\"k l m\"))')]",
                // but not for query fields
                "//str[@name='parsedquery'][contains(.,'(f1:K f1:L f1:M)')]");

        req.close();

    }

    @Test
    public void testThatAnalysisIsRunForPf2() {

        String q = "K L M";
        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "3",
                QueryParsing.OP, "OR",
                "defType", "querqy",
                "debugQuery", "true",
                DisMaxParams.PF2, "f1_lc f2_lc",
                PARAM_REWRITERS, "common_rules");


        assertQ("Analysis not applied for pf2",
                req,
                // Query terms should be lower-cased in analysis for pf2 fields
                "//str[@name='parsedquery'][contains(.,'DisjunctionMaxQuery(((f1_lc:\"k l\" f1_lc:\"l m\") | (f2_lc:\"k l\" f2_lc:\"l m\")))')] or " +
                "//str[@name='parsedquery'][contains(.,'DisjunctionMaxQuery(((f2_lc:\"k l\" f2_lc:\"l m\") | (f1_lc:\"k l\" f1_lc:\"l m\")))')]",
                // but not for query fields
                "//str[@name='parsedquery'][contains(.,'(f1:K f1:L f1:M)')]");

        req.close();

    }

    @Test
    public void testThatAnalysisIsRunForPf3() {

        String q = "K L M";
        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "3",
                QueryParsing.OP, "OR",
                "defType", "querqy",
                "debugQuery", "true",
                DisMaxParams.PF3, "f1_lc f2_lc",
                PARAM_REWRITERS, "common_rules");


        assertQ("Analysis not applied for pf",
                req,
                // Query terms should be lower-cased in analysis for pf fields
                "//str[@name='parsedquery'][contains(.,'DisjunctionMaxQuery((f1_lc:\"k l m\" | f2_lc:\"k l m\"))') or " +
                        "contains(.,'DisjunctionMaxQuery((f2_lc:\"k l m\" | f1_lc:\"k l m\"))')]",
                // but not for query fields
                "//str[@name='parsedquery'][contains(.,'(f1:K f1:L f1:M)')]");

        req.close();

    }

    @Test
    public void testThatPf2IsAppliedOnlyToExistingField() {

        String q = "a b c d";
        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "3",
                QueryParsing.OP, "OR",
                "defType", "querqy",
                "debugQuery", "true",
                DisMaxParams.PF2, "f1^2 f40^0.5",
                PARAM_REWRITERS, "common_rules");

        // f40 does not exists
        assertQ("wrong ps2",
            req,
            "//str[@name='parsedquery_toString'][contains(.,'(f1:\"a b\" f1:\"b c\" f1:\"c d\")^2.0')]",
            "//str[@name='parsedquery_toString'][not(contains(.,'(f40:\"a b\" f1:\"b c\" f1:\"c d\")^0.5'))]");

        req.close();

    }

    @Test
    public void testThatPf2IsAppliedOnlyToFieldsWithTermPositions() {

        String q = "a b c d";
        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "3",
                QueryParsing.OP, "OR",
                "defType", "querqy",
                "debugQuery", "true",
                DisMaxParams.PF2, "f1 str f_no_tfp",
                PARAM_REWRITERS, "common_rules");

        // str is a string field
        // f_no_tfp doesn't have term positions
        assertQ("wrong ps2",
            req,
            "//str[@name='parsedquery'][contains(.,'f1:\"a b\"')]",
            "//str[@name='parsedquery'][contains(.,'f1:\"b c\"')]",
            "//str[@name='parsedquery'][contains(.,'f1:\"c d\"')]",
            "//str[@name='parsedquery'][not(contains(.,'str:\"a b\"'))]",
            "//str[@name='parsedquery'][not(contains(.,'str:\"b c\"'))]",
            "//str[@name='parsedquery'][not(contains(.,'str:\"c d\"'))]",
            "//str[@name='parsedquery'][not(contains(.,'f_no_tfp:\"a b\"'))]",
            "//str[@name='parsedquery'][not(contains(.,'f_no_tfp:\"b c\"'))]",
            "//str[@name='parsedquery'][not(contains(.,'f_no_tfp:\"c d\"'))]");

        req.close();

    }

    @Test
    public void testThatPf3IsAppliedOnlyToFieldsWithTermPositions() {

        String q = "a b c d";
        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "3",
                QueryParsing.OP, "OR",
                "defType", "querqy",
                "debugQuery", "true",
                DisMaxParams.PF3, "f1^1.2 str^2 f_no_tfp^0.5 f4^4",
                PARAM_REWRITERS, "common_rules");

        // str is a string field
        // f_no_tfp / f_no_tp don't have term positions
        assertQ("wrong ps2",
            req,
            "//str[@name='parsedquery'][contains(.,'(f1:\"a b c\" f1:\"b c d\")^1.2')]",
            "//str[@name='parsedquery'][not(contains(.,'str:\"a b c\"^2.0'))]",
            "//str[@name='parsedquery'][not(contains(.,'str:\"b c d\"^2.0'))]",
            "//str[@name='parsedquery'][not(contains(.,'f_no_tfp:\"a b c\"^0.5'))]",
            "//str[@name='parsedquery'][not(contains(.,'f_no_tfp:\"b c d\"^0.5'))]",
            "//str[@name='parsedquery'][not(contains(.,'f_no_tp:\"a b c\"^0.5'))]",
            "//str[@name='parsedquery'][not(contains(.,'f_no_tp:\"b c d\"^0.5'))]");

        req.close();

    }

    @Test
    public void testThatPf3IsApplied() throws Exception {

        String q = "a b c d";
        SolrQueryRequest req = req("q", q,
            DisMaxParams.QF, "f1 f2",
            DisMaxParams.MM, "3",
            QueryParsing.OP, "OR",
            DisMaxParams.PF3, "f2^2.5 f3^1.5"
            );
        verifyQueryString(req, q,
            "(f2:\"a b c\" f2:\"b c d\")^2.5",
            "(f3:\"a b c\" f3:\"b c d\")^1.5"

        );

    }

    @Test
    public void testThatPFSkipsMustNotClauses() throws Exception {

        String q = "a b -c d e f";

        SolrQueryRequest req = req("q", q,
            DisMaxParams.QF, "f1 f2",
            DisMaxParams.MM, "3",
            QueryParsing.OP, "OR",
            DisMaxParams.PF, "f2^1.5 f3^1.5",
            DisMaxParams.PF2, "f1^2.1 f2^2.1",
            DisMaxParams.PF3, "f3^3.9 f1^3.9"
            );

        verifyQueryString(req, q,
            "(f2:\"a b d e f\")^1.5", "(f3:\"a b d e f\")^1.5",
            "(f1:\"a b\" f1:\"b d\" f1:\"d e\" f1:\"e f\")^2.1",
            "(f2:\"a b\" f2:\"b d\" f2:\"d e\" f2:\"e f\")^2.1",
            "(f3:\"a b d\" f3:\"b d e\" f3:\"d e f\")^3.9",
            "(f1:\"a b d\" f1:\"b d e\" f1:\"d e f\")^3.9"

        );

    }

    @Test
    public void testThatPFWorksWithSynonymRewriting() {

        SolrQueryRequest req = req("q", "a b",
                DisMaxParams.QF, "f1 f2^0.9",
                DisMaxParams.PF, "f1^0.5",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules");

        assertQ("ps with synonyms not working",
            req,
            "//str[@name='parsedquery'][contains(.,'PhraseQuery(f1:\"a b\")^0.5')]");

        req.close();

    }

    @Test
    public void testThatPF23FWorksWithSynonymRewriting() {

        SolrQueryRequest req = req("q", "a b c d",
                DisMaxParams.QF, "f1 f2^0.9",
                DisMaxParams.PF2, "f1~2^2.1",
                DisMaxParams.PF3, "f2~3^3.9",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules");

        assertQ("ps2/3 with synonyms not working",
            req,
            "//str[@name='parsedquery'][contains(.,'(f1:\"a b\"~2 f1:\"b c\"~2 f1:\"c d\"~2)^2.1')]",
            "//str[@name='parsedquery'][contains(.,'(f2:\"a b c\"~3 f2:\"b c d\"~3)^3.9')]");

        req.close();

    }

    @Test
    public void testThatGeneratedTermsArePenalised() {
        SolrQueryRequest req = req("q", "a b",
                DisMaxParams.QF, "f1^2",
                DisMaxParams.PF, "f1^0.5",
                GFB, "0.8",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules");

        assertQ(GFB + " not working",
            req,
            "//str[@name='parsedquery'][contains(.,'f1:a^2.0 | f1:x^1.6') or contains(.,'f1:x^1.6 | f1:a^2.0')]",
            "//str[@name='parsedquery'][contains(.,'PhraseQuery(f1:\"a b\")^0.5')]");

        req.close();
    }

    @Test
    public void testThatGeneratedQueryFieldBoostsAreApplied() {
        SolrQueryRequest req = req("q", "a",
                DisMaxParams.QF, "f1^2 f2^3",
                GFB, "0.8",
                GQF, "f2^10",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules");

        assertQ("Generated query field boosts not working",
            req,
            "//str[@name='parsedquery'][contains(.,'f1:a^2.0') and contains(.,'f2:a^3.0') and contains(.,'f2:x^10.0')]"
        );
        req.close();
    }

    @Test
    public void testThatGeneratedQueryFieldsAreApplied() {
        SolrQueryRequest req = req("q", "a",
                DisMaxParams.QF, "f1^2 f2^3",
                GFB, "0.8",
                GQF, "f2 f4",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules");

        assertQ("Generated query fields not working",
                req,
                "//str[@name='parsedquery'][contains(.,'f1:a^2.0') and contains(.,'f2:a^3.0') and" +
                        " contains(.,'f2:x^2.4') and contains(.,'f4:x^0.8')]"
        );
        req.close();
    }

    @Test
    public void testThatBoostUpInPurelyNegativeSingleTokenQueryIsApplied() {
        SolrQueryRequest req = req("q", "xx uu",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules");

        assertQ("UP on negative single query not working",
                req,
                "//result[@name='response' and @numFound='2']",
                "//result/doc[1]/str[@name='id'][text()='11']"
        );
        req.close();

    }

    @Test
    public void testThatBoostDownInPurelyNegativeMultiTokenQueryIsApplied() {
        SolrQueryRequest req = req("q", "gg ss",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules");

        assertQ("UP on negative multi-token query not working",
                req,
                "//result[@name='response' and @numFound='2']",
                "//result/doc[1]/str[@name='id'][text()='11']"
        );
        req.close();

    }

    @Test
    public void testThatBoostDownInPurelyNegativeSingleTokenQueryIsApplied() {
        SolrQueryRequest req = req("q", "ff uu",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules");

        assertQ("UP on negative single query not working",
                req,
                "//result[@name='response' and @numFound='2']",
                "//result/doc[1]/str[@name='id'][text()='11']"
        );
        req.close();

    }

    @Test
    public void testThatBoostUpMixedQueryIsApplied() {
        SolrQueryRequest req = req("q", "hh uu",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules");

        assertQ("UP on negative single query not working",
                req,
                "//result[@name='response' and @numFound='2']",
                "//result/doc[1]/str[@name='id'][text()='11']"
        );
        req.close();

    }

    @Test
    public void testThatUpRuleCanPickUpPlaceHolder() {

        SolrQueryRequest req = req("q", "aaa",
                DisMaxParams.QF, "f1 f2",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules");

        assertQ("Default ranking for picking up wildcard not working",
                req,
                "//result/doc[1]/str[@name='id'][text()='7']"
        );
        req.close();

        SolrQueryRequest req2 = req("q", "aaa w87",
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "100%",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules");

        assertQ("Ranking for picking up wildcard not working",
                req2,
                "//result/doc[1]/str[@name='id'][text()='8']",
                "//result[@name='response' and @numFound='2']"
        );
        req2.close();


    }

    @Test
    public void testThatHighlightingIsApplied() {
        SolrQueryRequest req = req("q", "a",
                DisMaxParams.QF, "f1",
                HighlightParams.HIGHLIGHT, "on",
                HighlightParams.FIELDS, "f1",
                HighlightParams.SIMPLE_PRE, "PRE",
                HighlightParams.SIMPLE_POST, "POST",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules");

        assertQ("Highlighting not working",
                req,
                "//lst[@name='highlighting']//arr[@name='f1']/str[text()='PREaPOST']"
        );
        req.close();

    }

    @Test
    public void testThatHighlightingIsNotAppliedToBoostQuery() {
        SolrQueryRequest req = req("q", "o",
                DisMaxParams.QF, "f1",
                HighlightParams.HIGHLIGHT, "true",
                HighlightParams.FIELDS, "f1",
                HighlightParams.SIMPLE_PRE, "PRE",
                HighlightParams.SIMPLE_POST, "POST",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules");

        assertQ("UP token is highlighted",
                req,
                "//lst[@name='highlighting']//arr[@name='f1']/str[not(contains(.,'PREuPOST'))]"
        );
        req.close();

    }

    @Test
    public void testThatHighlightingIsAppliedToSynonyms() {
        SolrQueryRequest req = req("q", "o",
                DisMaxParams.QF, "f1",
                HighlightParams.HIGHLIGHT, "true",
                HighlightParams.FIELDS, "f1",
                HighlightParams.SIMPLE_PRE, "PRE",
                HighlightParams.SIMPLE_POST, "POST",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules");

        assertQ("UP token is highlighted",
                req,
                "//lst[@name='highlighting']//arr[@name='f1']/str[contains(.,'PREsPOST')]"
        );
        req.close();

    }

    @Test
    public void testThatMatchAllWithFilterIsApplied() {
        SolrQueryRequest req = req("q", "this should be ignored",
                DisMaxParams.QF, "f1",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "match_all_filter");

        assertQ("Match all not working",
                req,
                "//arr[@name='parsed_filter_queries']/str[text()='f1:a']",
                "//lst[@name='debug']/str[@name='parsedquery'][text()='MatchAllDocsQuery(*:*)']",
                "//result/doc/str[@name='id'][text()='1']",
                "//result/doc/str[@name='id'][text()='2']",
                "//result[@name='response' and @numFound='2']"
        );
        req.close();

    }

    @Test
    public void testThatBfIsApplied() {

        SolrQueryRequest req = req("q", "aaa",
                DisMaxParams.QF, "f1",
                DisMaxParams.BF, "product(termfreq(f2,'w87'),-1)^2.3",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules");

        assertQ("bq not applied",
                req,
                "//str[@name='parsedquery'][contains(.,' FunctionQuery(product(termfreq(f2,w87),const(-1)))^2.3')]",
                "//doc[1]/str[@name='id'][text()='7']"

        );
        req.close();

    }

    @Test
    public void testThatBqIsApplied() {

        SolrQueryRequest req = req("q", "aaa",
                DisMaxParams.QF, "f1",
                DisMaxParams.BQ, "f2:w87",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules");

        assertQ("bq not applied",
                req,
                "//str[@name='parsedquery'][contains(.,'f2:w87')]",
                "//str[@name='parsedquery'][not(contains(.,'BoostedQuery'))]",
                "//doc[1]/str[@name='id'][text()='8']"

        );
        req.close();

    }

    @Test
    public void testThatBqIsAppliedToMatchAllDocsQuery() {

        SolrQueryRequest req = req("q", "*:*",
                DisMaxParams.QF, "f1",
                DisMaxParams.BQ, "f2:w87",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules");

        assertQ("bq not applied to MatchAll",
                req,
                "//result[@numFound='18']",
                "//str[@name='parsedquery'][contains(.,'f2:w87')]",
                "//str[@name='parsedquery'][not(contains(.,'BoostedQuery'))]",
                "//doc[1]/str[@name='id'][text()='8']"

        );
        req.close();

    }

    @Test
    public void testThatBqIsAppliedToMatchAllDocsQueryRegardlessOfPf() {

        SolrQueryRequest req = req("q", "*:*",
                DisMaxParams.QF, "f1",
                DisMaxParams.BQ, "f2:w87",
                DisMaxParams.PF, "f1",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules");

        assertQ("bq not applied to MatchAll",
                req,
                "//result[@numFound='18']",
                "//str[@name='parsedquery'][contains(.,'f2:w87')]",
                "//str[@name='parsedquery'][not(contains(.,'BoostedQuery'))]",
                "//doc[1]/str[@name='id'][text()='8']"

        );
        req.close();

    }

    @Test
    public void testThatBoostParamIsApplied() {

        SolrQueryRequest req = req("q", "aaa",
                DisMaxParams.QF, "f1",
                QuerqyDismaxParams.MULT_BOOST, "{!lucene}f2:w87^100",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules");

        assertQ("bq not applied",
                req,
                "//lst[@name='explain']/str[@name='8'][contains(.,'weight(FunctionScoreQuery(f1:aaa, " +
                        "scored by boost(query((f2:w87)^100.0,def=1.0))))')]",
                "//doc[1]/str[@name='id'][text()='8']"

        );
        req.close();

    }

    @Test
    public void testThatSeveralBoostParamsAreApplied() {

        SolrQueryRequest req = req("q", "aaa",
                DisMaxParams.QF, "f1",
                QuerqyDismaxParams.MULT_BOOST, "{!lucene}f2:w87^100",
                QuerqyDismaxParams.MULT_BOOST, "{!lucene}f2:w87^200",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules");

        assertQ("bq not applied",
                req,
                "//lst[@name='explain']/str[@name='8'][contains(.,'weight(FunctionScoreQuery(f1:aaa, " +
                        "scored by boost(product(query((f2:w87)^100.0,def=1.0),query((f2:w87)^200.0,def=1.0)))))')]",
                "//doc[1]/str[@name='id'][text()='8']"

        );
        req.close();
    }


    @Test
    public void testQuerqyGeneratedMultiplicativeBoostIsApplied() {

        SolrQueryRequest req = req("q", "aaa",
                DisMaxParams.QF, "f1",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "boost_mult_rewriter");

        String expectedBoostQuery = "product(if(query(f2:w87,def=0.0),const(5.0),const(1.0)),if(query(f1:vv,def=0.0),const(0.5),const(1.0)))";

        assertQ("multiplicative boost from raw query not applied",
                req,
                "//lst[@name='explain']/str[@name='8'][contains(.,'weight(FunctionScoreQuery(f1:aaa, scored by boost(" + expectedBoostQuery + ")))')]",
                "//lst[@name='explain']/str[@name='8'][contains(.,'5.0 = product')]",
                "//doc[1]/str[@name='id'][text()='8']"
        );

        req.close();

        req = req("q", "xx",
                DisMaxParams.QF, "f1 f2",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "boost_mult_rewriter");

        expectedBoostQuery = "product(if(query(f2:w87,def=0.0),const(5.0),const(1.0)),if(query(f1:vv f2:vv,def=0.0),const(0.5),const(1.0)))";

        assertQ("multiplicative boost from boolean query not applied",
                req,
                "//lst[@name='explain']/str[@name='10'][contains(.,'weight(FunctionScoreQuery((f1:xx | f2:xx), " +
                        "scored by boost(" + expectedBoostQuery + ")))')] or " +
                        "//lst[@name='explain']/str[@name='10'][contains(.,'weight(FunctionScoreQuery((f2:xx | f1:xx), " +
                        "scored by boost(" + expectedBoostQuery + ")))')]"                ,
                "//lst[@name='explain']/str[@name='10'][contains(.,'0.5 = product(')]",
                "//doc[1]/str[@name='id'][text()='11']"
        );

        req.close();

    }

    @Test
    public void testMultiplicativeRulesAreMultiplied() {

        SolrQueryRequest req = req("q", "multboost",
                DisMaxParams.QF, "f1",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules_multiplicative");

        assertQ("multiplicative boost from rule is not applied",
                req,
                "//doc[1]/str[@name='id'][text()='16']",
                "//lst[@name='explain']/str[@name='16'][contains(.,'50.0 = product')]", // UP(10) * UP(5)
                "//lst[@name='explain']/str[@name='15'][contains(.,'10.0 = product')]", // UP(10)
                "//lst[@name='explain']/str[@name='17'][contains(.,'0.2 = product')]",  // DOWN(5)
                "//lst[@name='explain']/str[@name='18'][contains(.,'2.0 = product')]"   // UP(10) * DOWN(5)
        );

        req.close();
    }

    @Test
    public void testMultiplicativeRulesAreCombinedWithBoostRequestParameter() {

        SolrQueryRequest req = req("q", "multboost",
                DisMaxParams.QF, "f1",
                QuerqyDismaxParams.MULT_BOOST, "if(exists(query({!v='f1:multup2'})),0.1,1)",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "common_rules_multiplicative");

        assertQ("multiplicative boost from rule is not applied",
                req,
                "//doc[1]/str[@name='id'][text()='15']",
                "//lst[@name='explain']/str[@name='15'][contains(.,'10.0 = product')]",
                "//lst[@name='explain']/str[@name='16'][contains(.,'5.0 = product')]" // * UP(10) * UP(5) / 10 (by query parameter)
        );

        req.close();
    }


    public void verifyQueryString(SolrQueryRequest req, String q, String... expectedSubstrings) throws Exception {

        QuerqyDismaxQParser parser = new QuerqyDismaxQParser(q, null, req.getParams(), req,
                new WhiteSpaceQuerqyParser(), new RewriteChain(), new MultiSinkInfoLogging(Collections.emptyMap()), null);
        Query query = parser.parse();
        req.close();
        assertTrue(query instanceof BooleanQuery);
        BooleanQuery bq = (BooleanQuery) query;
        String qStr = bq.toString();
        for (String exp : expectedSubstrings) {
            assertTrue("Missing: " + exp + " in " + bq, qStr.contains(exp));
        }

    }

    protected BooleanQuery assertBQ(Query query) {

        BooleanQuery bq;
        if (query instanceof WrappedQuery) {
            Query w = ((WrappedQuery) query).getWrappedQuery();
            assertTrue(w instanceof BooleanQuery);
            bq = (BooleanQuery) w;
        } else {
            assertTrue(query instanceof BooleanQuery);
            bq = (BooleanQuery) query;
        }

        return bq;
    }

    public static class MatchAllRewriter extends SolrRewriterFactoryAdapter {

        public MatchAllRewriter(final String rewriterId) {
            super(rewriterId);
        }

        @Override
        public void configure(final Map<String, Object> config) { }

        @Override
        public List<String> validateConfiguration(final Map<String, Object> config) {
            return Collections.emptyList();
        }

        @Override
        public RewriterFactory getRewriterFactory() {
            return new RewriterFactory(rewriterId) {
                @Override
                public QueryRewriter createRewriter(final ExpandedQuery input,
                                                    final SearchEngineRequestAdapter searchEngineRequestAdapter) {
                    return (query, requestAdapter) -> {
                        query.setUserQuery(new MatchAllQuery());
                        query.addFilterQuery(WhiteSpaceQuerqyParser.parseString("a"));
                        return new RewriterOutput(query);
                    };
                }

                @Override
                public Set<Term> getCacheableGenerableTerms() {
                    return Collections.emptySet();
                }
            };
        }
    }

    // rewriter that just adds some static up/down boost
    public static class MultiplicativeBoostRewriter extends SolrRewriterFactoryAdapter {

        private static final BoostQuery UP_BOOST = BoostQueryBuilder.boost(StringRawQueryBuilder.raw("f2:w87"), 5f).build();
        private static final BoostQuery DOWN_BOOST = BoostQueryBuilder.boost(bq("vv"), 0.5f).build();

        public MultiplicativeBoostRewriter(final String rewriterId) {
            super(rewriterId);
        }

        @Override
        public void configure(final Map<String, Object> config) { }

        @Override
        public List<String> validateConfiguration(final Map<String, Object> config) {
            return Collections.emptyList();
        }

        @Override
        public RewriterFactory getRewriterFactory() {
            return new RewriterFactory(rewriterId) {
                @Override
                public QueryRewriter createRewriter(final ExpandedQuery input,
                                                    final SearchEngineRequestAdapter searchEngineRequestAdapter) {
                    return (query, requestAdapter) -> {
                        query.addMultiplicativeBoostQuery(UP_BOOST);
                        query.addMultiplicativeBoostQuery(DOWN_BOOST);
                        return new RewriterOutput(query);
                    };
                }

                @Override
                public Set<Term> getCacheableGenerableTerms() {
                    return Collections.emptySet();
                }
            };
        }
    }
}
