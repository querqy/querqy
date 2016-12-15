package querqy.solr;

import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.WrappedQuery;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import querqy.parser.WhiteSpaceQuerqyParser;
import querqy.rewrite.RewriteChain;

@SolrTestCaseJ4.SuppressSSL
public class DefaultQuerqyDismaxQParserTest extends SolrTestCaseJ4 {

   public void index() throws Exception {

      assertU(adoc("id", "1", "f1", "a"));
      assertU(adoc("id", "2", "f1", "a"));
      assertU(adoc("id", "3", "f2", "a"));
      assertU(adoc("id", "4", "f1", "b"));
      assertU(adoc("id", "5", "f1", "spellcheck", "f2", "test"));
      assertU(adoc("id", "6", "f1", "spellcheck filtered", "f2", "test"));
      assertU(adoc("id", "7", "f1", "aaa"));
      assertU(adoc("id", "8", "f1", "aaa bbb ccc", "f2", "w87"));

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
    public void testLocalParams() throws Exception {
        SolrQueryRequest req = req("q", "{!querqy qf='f1 f2'}a b");
    
        assertQ("local params don't work",
             req,"//result[@name='response' and @numFound='4']");

        req.close();
    }
   
    @Test
    public void testThatFilterRulesFromCollationDontEndUpInMainQuery() throws Exception {
        
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

        QuerqyDismaxQParser parser = new QuerqyDismaxQParser("a b c", null, req.getParams(), req, new RewriteChain(),
            new WhiteSpaceQuerqyParser(), null);

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

        QuerqyDismaxQParser parser = new QuerqyDismaxQParser(q, null, req.getParams(), req, new RewriteChain(),
            new WhiteSpaceQuerqyParser(), null);

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
        QuerqyDismaxQParser parser = new QuerqyDismaxQParser(q, null, req.getParams(), req, new RewriteChain(),
            new WhiteSpaceQuerqyParser(), null);
        Query query = parser.parse();

        req.close();

        BooleanQuery bq = assertBQ(query);
        assertEquals(0, bq.getMinimumNumberShouldMatch());
    }

    @Test
    public void testThatPfIsAppliedOnlyToExistingField() throws Exception {

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
   public void testThatMatchAllDoesNotThrowException() throws Exception {
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
            "//result[@name='response' and @numFound='8']"

      );

      req.close();
   }

   @Test
   public void testThatPfIsAppliedOnlyToFieldsWithTermPositions() throws Exception {

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
    public void testThatAnalysisIsRunForPf() throws Exception {

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
    public void testThatAnalysisIsRunForPf2() throws Exception {

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
    public void testThatAnalysisIsRunForPf3() throws Exception {

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
   public void testThatPf2IsAppliedOnlyToExistingField() throws Exception {

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
   public void testThatPf2IsAppliedOnlyToFieldsWithTermPositions() throws Exception {

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
   public void testThatPf3IsAppliedOnlyToFieldsWithTermPositions() throws Exception {

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
   public void testThatPFWorksWithSynonymRewriting() throws Exception {

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
   public void testThatPF23FWorksWithSynonymRewriting() throws Exception {

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
   public void testThatGeneratedTermsArePenalised() throws Exception {
      SolrQueryRequest req = req("q", "a b",
            DisMaxParams.QF, "f1^2",
            DisMaxParams.PF, "f1^0.5",
            QuerqyDismaxQParser.GFB, "0.8",
            "defType", "querqy",
            "debugQuery", "true");

      assertQ(QuerqyDismaxQParser.GFB + " not working",
            req,
            "//str[@name='parsedquery'][contains(.,'f1:a^2.0 | f1:x^1.6')]",
            "//str[@name='parsedquery'][contains(.,'PhraseQuery(f1:\"a b\")^0.5')]");

      req.close();
   }
   
   @Test
   public void testThatGeneratedQueryFieldBoostsAreApplied() throws Exception {
      SolrQueryRequest req = req("q", "a",
            DisMaxParams.QF, "f1^2 f2^3",
            QuerqyDismaxQParser.GFB, "0.8",
            QuerqyDismaxQParser.GQF, "f2^10",
            "defType", "querqy",
            "debugQuery", "true");

      assertQ("Generated query field boosts not working",
            req,
            "//str[@name='parsedquery'][contains(.,'f1:a^2.0 | f2:a^3.0 | f2:x^10.0')]"
      );
      req.close();
   }
   
   @Test
   public void testThatGeneratedQueryFieldsAreApplied() throws Exception {
      SolrQueryRequest req = req("q", "a",
            DisMaxParams.QF, "f1^2 f2^3",
            QuerqyDismaxQParser.GFB, "0.8",
            QuerqyDismaxQParser.GQF, "f2 f4",
            "defType", "querqy",
            "debugQuery", "true");

      assertQ("Generated query fields not working",
            req,
            "//str[@name='parsedquery'][contains(.,'f1:a^2.0 | f2:a^3.0 | f2:x^2.4 | f4:x^0.8')]"
      );
      req.close();
   }

    @Test
    public void testThatUpRuleCanPickUpPlaceHolder() throws Exception {

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

    public void verifyQueryString(SolrQueryRequest req, String q, String... expectedSubstrings) throws Exception {

      QuerqyDismaxQParser parser = new QuerqyDismaxQParser(q, null, req.getParams(), req, new RewriteChain(),
           new WhiteSpaceQuerqyParser(), null);
      Query query = parser.parse();
      req.close();
      assertTrue(query instanceof BooleanQuery);
      BooleanQuery bq = (BooleanQuery) query;
      String qStr = bq.toString();
      for (String exp : expectedSubstrings) {
         assertTrue("Missing: " + exp + " in " + bq, qStr.indexOf(exp) > -1);
      }

   }

    protected BooleanQuery assertBQ(Query query) {

        BooleanQuery bq = null;
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

}
