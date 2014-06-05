package qp.parser;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Before;
import org.junit.Test;

import qp.AbstractQueryTest;
import qp.QueryTransformerVisitor;
import qp.model.Query;
import qp.parser.QueryParser.QueryContext;

public class QueryTransformerVisitorTest extends AbstractQueryTest {

    protected Query makeQuery(String input) {
        QueryLexer lex = new QueryLexer(new ANTLRInputStream(input));
        CommonTokenStream tokens = new CommonTokenStream(lex);
        QueryParser parser = new QueryParser(tokens);
        
        QueryContext t = parser.query();
        return (Query) t.accept(new QueryTransformerVisitor(input.toCharArray()));
    }

    
    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testSingleToken() {
        Query q = makeQuery("a");
        
        
        assertThat(q, 
                bq(
                        bq(
                                dmq(
                                        term("a")
                                )
                )));
    }
    
    @Test
    public void testTwoTokens() {
        Query q = makeQuery("a b");
        
        
        assertThat(q, 
                bq(
                        bq(
                                dmq(term("a")),
                                dmq(term("b"))
                                
                )));
    }
    
    @Test
    public void testTwoTokensSecondMandatory() {
        Query q = makeQuery("a +b");
        
        
        assertThat(q, 
                bq(
                        bq(
                                dmq(term("a")),
                                dmq(must(), term("b"))
                                
                )));
        
    }
    
    @Test
    public void testTwoTokensFirstMandatory() {
        Query q =  makeQuery("+a b");    
        assertThat(q, 
                bq(
                        bq(
                                dmq(must(), term("a")),
                                dmq(term("b"))
                                
                )));
        
        
    }
    
    @Test
    public void testTwoTokensAllMandatory() {
        Query q =  makeQuery("+a +b");    
        assertThat(q, 
                bq(
                        bq(
                                dmq(must(), term("a")),
                                dmq(must(), term("b"))
                                
                )));
        
        
    }
    
    @Test
    public void testTwoTokensByAndMandatory() {
        Query q =  makeQuery("a AND b");    
        assertThat(q, 
                bq(
                        bq(
                                dmq(must(), term("a")),
                                dmq(must(), term("b"))
                                
                )));
        
        
    }


}
