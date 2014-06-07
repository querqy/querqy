package querqy.antlr.parser;

import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Before;
import org.junit.Test;

import querqy.antlr.ANTLRQueryParser;
import querqy.model.Query;


public class QueryTransformerVisitorTest extends querqy.AbstractQueryTest {

    protected Query makeQuery(String input) {
        return new ANTLRQueryParser().parse(input);
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
