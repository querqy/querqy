package qp.rewrite.synonyms.lucene;

import static org.hamcrest.MatcherAssert.assertThat;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Before;
import org.junit.Test;

import qp.QueryTransformerVisitor;
import qp.model.Query;
import qp.parser.QueryLexer;
import qp.parser.QueryParser;
import qp.parser.QueryParser.QueryContext;
import qp.rewrite.QueryRewriter;

public class LuceneSynonymsRewriterTest extends AbtractQueryTest {
	
	QueryRewriter rewriter;

	@Before
	public void setUp() throws Exception {
		
		LuceneSynonymsRewriterFactory factory = new LuceneSynonymsRewriterFactory(
				getClass().getClassLoader().getResourceAsStream("synonyms-test.txt"));
		rewriter = factory.getRewriter();
	}
	
	protected Query makeQuery(String input) {
		QueryLexer lex = new QueryLexer(new ANTLRInputStream(input));
		CommonTokenStream tokens = new CommonTokenStream(lex);
		QueryParser parser = new QueryParser(tokens);
		
		QueryContext t = parser.query();
		return (Query) t.accept(new QueryTransformerVisitor());
	}

	@Test
	public void testSingleClauseExpansion() {
		Query q = makeQuery("a");
		
		
		assertThat(rewriter.rewrite(q), 
				bq(
						noOp(), 
						bq(
								noOp(), 
								dmq(
										term("a"),
										term("x")
								)
				)));
		
	}
	
	@Test
	public void testSingleClauseExpansionWithMultiCharWords() {
		Query q = makeQuery("abc");
		
		
		assertThat(rewriter.rewrite(q), 
				bq(
						noOp(), 
						bq(
								noOp(), 
								dmq(
										term("abc"),
										term("def")
								)
				)));
		
	}
	
	@Test
	public void testSingleClauseInputToMultiClauseOutput() {
		Query q = makeQuery("f");
		
		
		assertThat(rewriter.rewrite(q), 
				bq(
						noOp(), 
						bq(
								noOp(), 
								dmq(
										term("f"),
										bq(
												and(),
												dmq(must(), term("k")),
												dmq(must(), term("l"))
										)
								)
				)));
		
	}
	
	@Test
    public void testThatPartialMatchDoesntGetExpanded() throws Exception {
	    // abc => ...
	    // is in the synonym dict
	    Query q = makeQuery("ab c");
       
	    assertThat(rewriter.rewrite(q),
                bq(
                        noOp(), 
                        bq(
                                noOp(), 
                                dmq(term("ab")),
                                dmq(term("c"))
                        )
                
                ));
    }
	
	@Test
	public void testSingleClauseInputToMixedOutput() {
	    
		Query q = makeQuery("j");
		assertThat(rewriter.rewrite(q), 
				bq(
						noOp(), 
						bq(
								noOp(), 
								dmq(
										term("j"),
										bq(
												and(),
												dmq(must(), term("s")),
												dmq(must(), term("t"))
										),
										term("q")
								)
				)));
		
	}
	
	@Test
    public void testTwoClausesToOne() throws Exception {
	    Query q = makeQuery("b c");
	    assertThat(rewriter.rewrite(q), 
                bq(
                        noOp(), 
                        bq(
                                noOp(), 
                                dmq(
                                        term("b"),
                                        bq(
                                                and(),
                                                dmq(must(), term("y")),
                                                bq(
                                                        mustNot(), 
                                                        and(),
                                                        dmq(must(), term("b")),
                                                        dmq(must(), term("c")))
                                        )
                                ),
                                dmq(
                                        term("c"),
                                        bq(
                                                
                                                and(),
                                                dmq(must(), term("y")),
                                                bq(
                                                        mustNot(), 
                                                        and(),
                                                        dmq(must(), term("b")),
                                                        dmq(must(), term("c")))
                                                
                                        )
                                )
                                
                )));
        
    }
	
	@Test
    public void testThreeClausesToTwo() throws Exception {
	    Query q = makeQuery("bb cc dd");
	    assertThat(rewriter.rewrite(q), 
	            bq(
	                        noOp(), 
	                        bq(
	                                noOp(), 
	                                dmq(
	                                        term("bb"),
	                                        bq(
	                                                and(),
	                                                bq(
                                                            must(), 
                                                            and(),
                                                            dmq(must(), term("z")),
                                                            dmq(must(), term("x"))),
	                                                bq(
	                                                        mustNot(), 
	                                                        and(),
	                                                        dmq(must(), term("bb")),
	                                                        dmq(must(), term("cc")),
	                                                        dmq(must(), term("dd"))
	                                                        )
	                                        )
	                                ),
                                    dmq(
                                            term("cc"),
                                            bq(
                                                    
                                                    and(),
                                                    bq(
                                                            must(), 
                                                            and(),
                                                            dmq(must(), term("z")),
                                                            dmq(must(), term("x"))),
                                                    bq(
                                                            mustNot(), 
                                                            and(),
                                                            dmq(must(), term("bb")),
                                                            dmq(must(), term("cc")),
                                                            dmq(must(), term("dd"))
                                                    
                                            )
                                    )),
                                    dmq(
                                            term("dd"),
                                            bq(
                                                    
                                                    and(),
                                                    bq(
                                                            must(), 
                                                            and(),
                                                            dmq(must(), term("z")),
                                                            dmq(must(), term("x"))),
                                                    bq(
                                                            mustNot(), 
                                                            and(),
                                                            dmq(must(), term("bb")),
                                                            dmq(must(), term("cc")),
                                                            dmq(must(), term("dd"))
                                                    
                                            )
                                    ))
	                                
	                )));
	        
	    
    }
	
	/**
	 * Rules:
	 *  b c => y
     *  b c d => z x
     *  
     * Input:
     *  b c d
     *  
     * Expected Output:
     *  
     *  (b  (y AND -(b AND c))  ((z AND x) AND -(b AND c AND d)) )  
     *  
     *  (c  (y AND -(b AND c))  ((z AND x) AND -(b AND c AND d))  )
     *  
     *  (d  ((z AND x) AND -(b AND c AND d))  )
     * 
	 * @throws Exception
	 * 
	 */
	@Test
    public void test08() throws Exception {
	    Query q = makeQuery("b c d");
        assertThat(rewriter.rewrite(q), 
                bq(
                            noOp(), 
                            bq(
                                    noOp(), 
                                    dmq(
                                            term("b"),
                                            bq(
                                                 and(),
                                                 dmq(must(), term("y")),
                                                 bq(
                                                         mustNot(),
                                                         and(),
                                                         dmq(must(), term("b")),
                                                         dmq(must(), term("c"))
                                                 )
                                            ),
                                            bq(
                                                 and(),
                                                 bq(
                                                         must(), 
                                                         and(),
                                                         dmq(must(), term("z")),
                                                         dmq(must(), term("x"))),
                                                 bq(
                                                         mustNot(), 
                                                         and(),
                                                         dmq(must(), term("b")),
                                                         dmq(must(), term("c")),
                                                         dmq(must(), term("d")))
                                            )
                                    ),
                                    dmq(
                                            term("c"),
                                            bq(
                                                 and(),
                                                 dmq(must(), term("y")),
                                                 bq(
                                                         mustNot(),
                                                         and(),
                                                         dmq(must(), term("b")),
                                                         dmq(must(), term("c"))
                                                 )
                                            ),
                                            bq(
                                                 and(),
                                                 bq(
                                                         must(), 
                                                         and(),
                                                         dmq(must(), term("z")),
                                                         dmq(must(), term("x"))),
                                                 bq(
                                                         mustNot(), 
                                                         and(),
                                                         dmq(must(), term("b")),
                                                         dmq(must(), term("c")),
                                                         dmq(must(), term("d")))
                                            )
                                    ),
                                    dmq(
                                            term("d"),
                                            bq(
                                                    
                                                 and(),
                                                 bq(
                                                         must(), 
                                                         and(),
                                                         dmq(must(), term("z")),
                                                         dmq(must(), term("x"))),
                                                 bq(
                                                         mustNot(), 
                                                         and(),
                                                         dmq(must(), term("b")),
                                                         dmq(must(), term("c")),
                                                         dmq(must(), term("d"))
                                                    
                                            )
                                    ))
                                    
                    )));
    }
	


}
