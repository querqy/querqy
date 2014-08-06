package querqy.solr;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QueryParsing;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import querqy.rewrite.RewriteChain;
import querqy.rewrite.lucene.IndexStats;

public class QueryQParserTest extends SolrTestCaseJ4 {
	
	static final IndexStats DUMMY_INDEX_STATS = new IndexStats() {
        
        @Override
        public int df(Term term) {
            return 10;
        }
    };
    
    @BeforeClass
    public static void beforeClass() {
    	System.setProperty("tests.codec", "Lucene46");
	}
	
	@Test
	public void testThatAMMof2getsSetFor3optionalClauses() throws Exception {
		initCore("solrconfig.xml", "schema.xml");
		SolrQueryRequest req = req("q", "a b c", 
				DisMaxParams.QF, "f1 f2",
				DisMaxParams.MM, "2");
		QuerqyQParser parser = new QuerqyQParser("a b c", null, req.getParams(), req, new RewriteChain(), DUMMY_INDEX_STATS, new WhiteSpaceQuerqyParser());
		Query query = parser.parse();
		assertTrue(query instanceof BooleanQuery);
		BooleanQuery bq = (BooleanQuery) query;
		assertEquals(2, bq.getMinimumNumberShouldMatch());
	}
	
	@Test
	public void testThatMMIsAppliedWhileQueryContainsMUSTBooleanOperators() throws Exception {
		
		initCore("solrconfig.xml", "schema.xml");
		
		String q = "a +b c +d e f";
		
		SolrQueryRequest req = req("q", q, 
				DisMaxParams.QF, "f1 f2",
				DisMaxParams.MM, "3");
		QuerqyQParser parser = new QuerqyQParser(q, null, req.getParams(), req, new RewriteChain(), DUMMY_INDEX_STATS, new WhiteSpaceQuerqyParser());
		Query query = parser.parse();
		assertTrue(query instanceof BooleanQuery);
		BooleanQuery bq = (BooleanQuery) query;
		assertEquals(3, bq.getMinimumNumberShouldMatch());
	}
	
	@Test
	public void testThatMMIsNotAppliedWhileQueryContainsMUSTNOTBooleanOperator() throws Exception {
		
		initCore("solrconfig.xml", "schema.xml");
		
		String q = "a +b c -d e f";
		
		SolrQueryRequest req = req("q", q, 
				DisMaxParams.QF, "f1 f2",
				DisMaxParams.MM, "3",
				QueryParsing.OP, "OR"
				);
		QuerqyQParser parser = new QuerqyQParser(q, null, req.getParams(), req, new RewriteChain(), DUMMY_INDEX_STATS, new WhiteSpaceQuerqyParser());
		Query query = parser.parse();
		assertTrue(query instanceof BooleanQuery);
		BooleanQuery bq = (BooleanQuery) query;
		assertEquals(0, bq.getMinimumNumberShouldMatch());
	}
	
	@Test
	public void testThatPfIsApplied() throws Exception {
		
		initCore("solrconfig.xml", "schema.xml");
		
		String q = "a b c d";
		verifyQueryString(req("q", q, 
				DisMaxParams.QF, "f1 f2",
				DisMaxParams.MM, "3",
				QueryParsing.OP, "OR",
				DisMaxParams.PF, "f3^2 f4^0.5"
				), q , "f3:\"a b c d\"^2.0", "f4:\"a b c d\"^0.5");
		
	}
	
	@Test
	public void testThatPf2IsApplied() throws Exception {
		
		initCore("solrconfig.xml", "schema.xml");
		
		String q = "a b c d";
		verifyQueryString(req("q", q, 
				DisMaxParams.QF, "f1 f2",
				DisMaxParams.MM, "3",
				QueryParsing.OP, "OR",
				DisMaxParams.PF2, "f1^2 f4^0.5"
				), q , 
					"f1:\"a b\"^2.0", "f1:\"b c\"^2.0", "f1:\"c d\"^2.0",
					"f4:\"a b\"^0.5", "f4:\"b c\"^0.5", "f4:\"c d\"^0.5"
				
				);
		
		
	}
	
	@Test
	public void testThatPf3IsApplied() throws Exception {
		
		initCore("solrconfig.xml", "schema.xml");
		
		String q = "a b c d";
		verifyQueryString(req("q", q, 
				DisMaxParams.QF, "f1 f2",
				DisMaxParams.MM, "3",
				QueryParsing.OP, "OR",
				DisMaxParams.PF3, "f2^2.5 f3^1.5"
				), q , 
					"f2:\"a b c\"^2.5", "f2:\"b c d\"^2.5",
					"f3:\"a b c\"^1.5", "f3:\"b c d\"^1.5"
				
				);
		
		
	}
	
	@Test
	public void testThatPFSkipsMustNotClauses() throws Exception {
		initCore("solrconfig.xml", "schema.xml");
		
		String q = "a b -c d e f";
		verifyQueryString(req("q", q, 
				DisMaxParams.QF, "f1 f2",
				DisMaxParams.MM, "3",
				QueryParsing.OP, "OR",
				DisMaxParams.PF, "f2^1.5 f3^1.5",
				DisMaxParams.PF2, "f1^2.1 f2^2.1",
				DisMaxParams.PF3, "f3^3.9 f1^3.9"
				), q , 
					"f2:\"a b d e f\"^1.5", "f3:\"a b d e f\"^1.5",
					"f1:\"a b\"^2.1", "f1:\"b d\"^2.1", "f1:\"d e\"^2.1", "f1:\"e f\"^2.1", 
					"f2:\"a b\"^2.1", "f2:\"b d\"^2.1", "f2:\"d e\"^2.1", "f2:\"e f\"^2.1", 
					"f3:\"a b d\"^3.9", "f3:\"b d e\"^3.9", "f3:\"d e f\"^3.9", 
					"f1:\"a b d\"^3.9", "f1:\"b d e\"^3.9", "f1:\"d e f\"^3.9"
				
				);
		
	}
	
	public void verifyQueryString(SolrQueryRequest req, String q, String ...expectedSubstrings) throws Exception {
		
		QuerqyQParser parser = new QuerqyQParser(q, null, req.getParams(), req, new RewriteChain(), DUMMY_INDEX_STATS, new WhiteSpaceQuerqyParser());
		Query query = parser.parse();
		assertTrue(query instanceof BooleanQuery);
		BooleanQuery bq = (BooleanQuery) query;
		String qStr = bq.toString();
		for (String exp : expectedSubstrings) {
			assertTrue("Missing: " + exp, qStr.indexOf(exp) > -1);
		}

	}
	
	@After
	public void tearDown() throws Exception {
		deleteCore();
		super.tearDown();
	}

}
