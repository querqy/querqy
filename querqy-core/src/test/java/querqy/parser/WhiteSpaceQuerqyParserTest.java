package querqy.parser;

import static org.hamcrest.MatcherAssert.assertThat;
import static querqy.QuerqyMatchers.bq;
import static querqy.QuerqyMatchers.dmq;
import static querqy.QuerqyMatchers.must;
import static querqy.QuerqyMatchers.mustNot;
import static querqy.QuerqyMatchers.term;

import org.junit.Test;

import querqy.model.Query;
import querqy.parser.WhiteSpaceQuerqyParser;

public class WhiteSpaceQuerqyParserTest {

    @Test
    public void testSingleTerm() {
        WhiteSpaceQuerqyParser parser = new WhiteSpaceQuerqyParser();
        Query query = parser.parse("abc");
        assertThat(query,
                bq(
                        dmq(
                                term("abc")
                           )
                ));
        
    }
    
    @Test
    public void testSingleCharacterSingleTerm() throws Exception {
        WhiteSpaceQuerqyParser parser = new WhiteSpaceQuerqyParser();
        Query query = parser.parse("a");
        assertThat(query,
                bq(
                        dmq(
                                term("a")
                           )
                ));
    }
    
    @Test
    public void testSingleCharacterSingleTermMust() throws Exception {
        WhiteSpaceQuerqyParser parser = new WhiteSpaceQuerqyParser();
        Query query = parser.parse("+a");
        assertThat(query,
                bq(
                        dmq(must(), term("a")
                           )
                ));
    }
    
    @Test
    public void testSingleCharacterTermAtEnd() throws Exception {
        WhiteSpaceQuerqyParser parser = new WhiteSpaceQuerqyParser();
        Query query = parser.parse("cde a");
        assertThat(query,
                bq(
                        dmq(term("cde")),
                        dmq(term("a")
                           )
                ));
    }
    
    @Test
    public void testSingleCharacterTermAtBegin() throws Exception {
        WhiteSpaceQuerqyParser parser = new WhiteSpaceQuerqyParser();
        Query query = parser.parse("a cde");
        assertThat(query,
                bq(
                        dmq(term("a")),
                        dmq(term("cde")
                           )
                ));
    }
    
    @Test
    public void testSingleOperatorWithoutTerm() throws Exception {
        WhiteSpaceQuerqyParser parser = new WhiteSpaceQuerqyParser();
        Query query = parser.parse("+");
        assertThat(query,
                bq(
                        dmq(term("+"))
                           
                ));
    }
    
    @Test
    public void testOperatorWithoutTerm() throws Exception {
        WhiteSpaceQuerqyParser parser = new WhiteSpaceQuerqyParser();
        Query query = parser.parse("abc + def");
        assertThat(query,
                bq(
                        dmq(term("abc")),
                        dmq(term("+")),
                        dmq(term("def"))
                           
                ));
    }
    
    
    @Test
    public void testSingleCharacterTermAtEndMust() throws Exception {
        WhiteSpaceQuerqyParser parser = new WhiteSpaceQuerqyParser();
        Query query = parser.parse("cde +a");
        assertThat(query,
                bq(
                        dmq(term("cde")),
                        dmq(must(), term("a")
                           )
                ));
    }
    
    @Test
    public void testSingleTermMust() {
        WhiteSpaceQuerqyParser parser = new WhiteSpaceQuerqyParser();
        Query query = parser.parse("+abc");
        assertThat(query,
                bq(
                        dmq(
                            must(), term("abc")
                           )
                ));
        
    }
    @Test
    public void testSingleTermMustNot() {
        WhiteSpaceQuerqyParser parser = new WhiteSpaceQuerqyParser();
        Query query = parser.parse("-abc");
        assertThat(query,
                bq(
                        dmq(
                            mustNot(), term("abc")
                           )
                ));
        
    }
    
    
    @Test
    public void testMultipleTerms() {
        WhiteSpaceQuerqyParser parser = new WhiteSpaceQuerqyParser();
        Query query = parser.parse("abc def ghijkl");
        assertThat(query,
                bq(
                        dmq(
                                term("abc")
                           ),
                        dmq(
                                term("def")
                           ),
                              
                        dmq(
                                term("ghijkl")
                           )
                           
                ));
        
    }
    
    @Test
    public void testMultipleTermsWithDuplicate() {
        WhiteSpaceQuerqyParser parser = new WhiteSpaceQuerqyParser();
        Query query = parser.parse("abc def def ghijkl");
        assertThat(query,
                bq(
                        dmq(
                                term("abc")
                           ),
                        dmq(
                                term("def")
                           ),
                        dmq(
                                term("def")
                           ),
                        dmq(
                                term("ghijkl")
                           )
                           
                ));
        
    }

    @Test
    public void testMultipleTermsWithDuplicateWithBooleanOp() {
        WhiteSpaceQuerqyParser parser = new WhiteSpaceQuerqyParser();
        Query query = parser.parse("-abc +def def ghijkl");
        assertThat(query,
                bq(
                        dmq(
                                mustNot(), term("abc")
                           ),
                        dmq(
                                must(), term("def")
                           ),
                        dmq(
                                term("def")
                           ),
                        dmq(
                                term("ghijkl")
                           )
                           
                ));
        
    }
    
  /*  @Test
    public void testSpeed() throws Exception {
        
        WhiteSpaceQuerqyParser parser = new WhiteSpaceQuerqyParser();
        Query query = parser.parse("-abc +def def ghijkl");
        
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < 10000000; i ++) {
            query = parser.parse("-abc +def def ghijkl");
        }
        long t2 = System.currentTimeMillis();
        System.out.println(t2 - t1);
        
    }*/
}
