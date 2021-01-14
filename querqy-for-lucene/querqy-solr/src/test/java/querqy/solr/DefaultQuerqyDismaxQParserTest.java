package querqy.solr;

import static querqy.solr.QuerqyDismaxParams.GFB;
import static querqy.solr.QuerqyDismaxParams.GQF;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.HighlightParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.WrappedQuery;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import querqy.infologging.InfoLogging;
import querqy.model.ExpandedQuery;
import querqy.model.MatchAllQuery;
import querqy.model.Term;
import querqy.parser.WhiteSpaceQuerqyParser;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriteChain;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.infologging.InfoLoggingContext;

import java.util.Collections;
import java.util.Set;

@SolrTestCaseJ4.SuppressSSL
public class DefaultQuerqyDismaxQParserTest extends SolrTestCaseJ4 {

    public void index() {

        assertU(adoc("id", "1", "f1", "a"));
        assertU(adoc("id", "2", "f1", "a"));
        assertU(adoc("id", "3", "f2", "a"));
        assertU(adoc("id", "4", "f1", "b"));
        assertU(adoc("id", "5", "f1", "spellcheck", "f2", "test"));
        assertU(adoc("id", "6", "f1", "spellcheck filtered", "f2", "test"));
        assertU(adoc("id", "7", "f1", "aaa"));
        assertU(adoc("id", "8", "f1", "aaa bbb ccc", "f2", "w87"));
        assertU(adoc("id", "9", "f1", "ignore o u s"));
        assertU(adoc("id", "10", "f1", "vv uu tt ss xx ff gg hh"));
        assertU(adoc("id", "11", "f1", "xx yy zz tt ll ff gg hh"));

        assertU(commit());
    }

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig-DefaultQuerqyDismaxQParserTest.xml", "schema.xml");
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
                "debugQuery", "true"
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
                "debugQuery", "true"
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
            new WhiteSpaceQuerqyParser(), new RewriteChain(), new InfoLogging(Collections.emptyMap()), null);

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
            new WhiteSpaceQuerqyParser(), new RewriteChain(), new InfoLogging(Collections.emptyMap()), null);

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
            new WhiteSpaceQuerqyParser(), new RewriteChain(), new InfoLogging(Collections.emptyMap()), null);
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
               "uq.similarityScore", "dfc"
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
            "debugQuery", "true"
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
            "debugQuery", "true"
            );

      assertQ("Matchall fails",
            req,
            "//str[@name='parsedquery'][contains(.,'*:*')]",
            "//result[@name='response' and @numFound='11']"

      );

      req.close();
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
                DisMaxParams.PF, "f1_lc f2_lc");


        assertQ("Analysis not applied for pf",
                req,
                // Query terms should be lower-cased in analysis for pf fields
                "//str[@name='parsedquery'][contains(.,'DisjunctionMaxQuery((f1_lc:\"k l m\" | f2_lc:\"k l m\"))')]",
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
                DisMaxParams.PF2, "f1_lc f2_lc");


        assertQ("Analysis not applied for pf2",
                req,
                // Query terms should be lower-cased in analysis for pf2 fields
                "//str[@name='parsedquery'][contains(.,'DisjunctionMaxQuery(((f1_lc:\"k l\" f1_lc:\"l m\") | (f2_lc:\"k l\" f2_lc:\"l m\")))')]",
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
                DisMaxParams.PF3, "f1_lc f2_lc");


        assertQ("Analysis not applied for pf",
                req,
                // Query terms should be lower-cased in analysis for pf fields
                "//str[@name='parsedquery'][contains(.,'DisjunctionMaxQuery((f1_lc:\"k l m\" | f2_lc:\"k l m\"))')]",
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
            DisMaxParams.PF2, "f1^2 f40^0.5");

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
            DisMaxParams.PF2, "f1 str f_no_tfp");

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
            DisMaxParams.PF3, "f1^1.2 str^2 f_no_tfp^0.5 f4^4");

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
            "debugQuery", "true");

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
            "debugQuery", "true");

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
            "debugQuery", "true");

      assertQ(GFB + " not working",
            req,
            "//str[@name='parsedquery'][contains(.,'f1:a^2.0 | f1:x^1.6')]",
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
            "debugQuery", "true");

      assertQ("Generated query field boosts not working",
            req,
            "//str[@name='parsedquery'][contains(.,'f1:a^2.0 | f2:a^3.0 | f2:x^10.0')]"
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
            "debugQuery", "true");

      assertQ("Generated query fields not working",
            req,
            "//str[@name='parsedquery'][contains(.,'f1:a^2.0 | f2:a^3.0 | f2:x^2.4 | f4:x^0.8')]"
      );
      req.close();
   }

    @Test
    public void testThatBoostUpInPurelyNegativeSingleTokenQueryIsApplied() {
        SolrQueryRequest req = req("q", "xx uu",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1",
                "defType", "querqy",
                "debugQuery", "true");

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
                "debugQuery", "true");

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
                "debugQuery", "true");

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
                "debugQuery", "true");

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
                "debugQuery", "true");

        assertQ("Default ranking for picking up wildcard not working",
                req,
                "//result/doc[1]/str[@name='id'][text()='7']"
        );
        req.close();

        SolrQueryRequest req2 = req("q", "aaa w87",
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.MM, "100%",
                "defType", "querqy",
                "debugQuery", "true");

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
                HighlightParams.HIGHLIGHT, "true",
                HighlightParams.FIELDS, "f1",
                HighlightParams.SIMPLE_PRE, "PRE",
                HighlightParams.SIMPLE_POST, "POST",
                "defType", "querqy",
                "debugQuery", "true");

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
                "debugQuery", "true");

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
                "debugQuery", "true");

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
                "defType", "querqyMatchAllAndFilter",
                "debugQuery", "true");

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
                "debugQuery", "true");

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
                "debugQuery", "true");

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
                "debugQuery", "true");

        assertQ("bq not applied to MatchAll",
                req,
                "//result[@numFound='11']",
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
                "debugQuery", "true");

        assertQ("bq not applied to MatchAll",
                req,
                "//result[@numFound='11']",
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
                "debugQuery", "true");

        assertQ("bq not applied",
                req,
                "//lst[@name='explain']/str[@name='8'][contains(.,'weight(FunctionScoreQuery(f1:aaa, " +
                        "scored by boost(score((f2:w87)^100.0))))')]",
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
                "debugQuery", "true");

        assertQ("bq not applied",
                req,
                "//lst[@name='explain']/str[@name='8'][contains(.,'weight(FunctionScoreQuery(f1:aaa, " +
                        "scored by boost(product(query((f2:w87)^100.0,def=1.0),query((f2:w87)^200.0,def=1.0)))))')]",
                "//doc[1]/str[@name='id'][text()='8']"

        );
        req.close();
    }


    public void verifyQueryString(SolrQueryRequest req, String q, String... expectedSubstrings) throws Exception {

      QuerqyDismaxQParser parser = new QuerqyDismaxQParser(q, null, req.getParams(), req,
           new WhiteSpaceQuerqyParser(), new RewriteChain(), new InfoLogging(Collections.emptyMap()), null);
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

    public static class MatchAllRewriter implements FactoryAdapter<RewriterFactory> {

        @Override
        public RewriterFactory createFactory(final String rewriterId, NamedList<?> args, ResourceLoader resourceLoader) {
            return new RewriterFactory(rewriterId) {
                @Override
                public QueryRewriter createRewriter(ExpandedQuery input, SearchEngineRequestAdapter searchEngineRequestAdapter) {
                    return query -> {
                        query.setUserQuery(new MatchAllQuery());
                        query.addFilterQuery(WhiteSpaceQuerqyParser.parseString("a"));
                        return query;
                    };
                }

                @Override
                public Set<Term> getCacheableGenerableTerms() {
                    return Collections.emptySet();
                }
            };
        }

        @Override
        public Class<?> getCreatedClass() {
            return QueryRewriter.class;
        }
    }
}
