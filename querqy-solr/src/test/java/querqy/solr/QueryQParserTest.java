package querqy.solr;

import static org.junit.Assert.*;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.CommonParams;
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
	
	@After
	public void tearDown() throws Exception {
		deleteCore();
		super.tearDown();
	}

}
