package querqy.parser;

import static org.hamcrest.MatcherAssert.assertThat;
import static querqy.QuerqyMatchers.bq;
import static querqy.QuerqyMatchers.dmq;
import static querqy.QuerqyMatchers.must;
import static querqy.QuerqyMatchers.mustNot;
import static querqy.QuerqyMatchers.term;

import org.junit.Test;
import querqy.model.Query;

public class FieldAwareWhiteSpaceQuerqyParserTest {

    @Test
    public void testSingleTerm() {
        FieldAwareWhiteSpaceQuerqyParser parser = new FieldAwareWhiteSpaceQuerqyParser();
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
        FieldAwareWhiteSpaceQuerqyParser parser = new FieldAwareWhiteSpaceQuerqyParser();
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
        FieldAwareWhiteSpaceQuerqyParser parser = new FieldAwareWhiteSpaceQuerqyParser();
        Query query = parser.parse("+a");
        assertThat(query,
                bq(
                        dmq(must(), term("a")
                           )
                ));
    }
    
    @Test
    public void testSingleCharacterTermAtEnd() throws Exception {
        FieldAwareWhiteSpaceQuerqyParser parser = new FieldAwareWhiteSpaceQuerqyParser();
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
        FieldAwareWhiteSpaceQuerqyParser parser = new FieldAwareWhiteSpaceQuerqyParser();
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
        FieldAwareWhiteSpaceQuerqyParser parser = new FieldAwareWhiteSpaceQuerqyParser();
        Query query = parser.parse("+");
        assertThat(query,
                bq(
                        dmq(term("+"))
                           
                ));
    }
    
    @Test
    public void testOperatorWithoutTerm() throws Exception {
        FieldAwareWhiteSpaceQuerqyParser parser = new FieldAwareWhiteSpaceQuerqyParser();
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
        FieldAwareWhiteSpaceQuerqyParser parser = new FieldAwareWhiteSpaceQuerqyParser();
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
        FieldAwareWhiteSpaceQuerqyParser parser = new FieldAwareWhiteSpaceQuerqyParser();
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
        FieldAwareWhiteSpaceQuerqyParser parser = new FieldAwareWhiteSpaceQuerqyParser();
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
        FieldAwareWhiteSpaceQuerqyParser parser = new FieldAwareWhiteSpaceQuerqyParser();
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
        FieldAwareWhiteSpaceQuerqyParser parser = new FieldAwareWhiteSpaceQuerqyParser();
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
        FieldAwareWhiteSpaceQuerqyParser parser = new FieldAwareWhiteSpaceQuerqyParser();
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

    @Test
    public void testTwoOperatorsAreHandledAsOperatorPlusValue() {
        FieldAwareWhiteSpaceQuerqyParser parser = new FieldAwareWhiteSpaceQuerqyParser();
        assertThat(parser.parse("abc -- de"),
                bq(
                        dmq(
                                term("abc")
                        ),
                        dmq(
                                mustNot(), term("-")
                        ),
                        dmq(
                                term("de")
                        )

                ));

        assertThat(parser.parse("abc ++ de"),
                bq(
                        dmq(
                                term("abc")
                        ),
                        dmq(
                                must(), term("+")
                        ),
                        dmq(
                                term("de")
                        )

                ));

        assertThat(parser.parse("abc +- de"),
                bq(
                        dmq(
                                term("abc")
                        ),
                        dmq(
                                must(), term("-")
                        ),
                        dmq(
                                term("de")
                        )

                ));

        assertThat(parser.parse("abc -+ -de"),
                bq(
                        dmq(
                                term("abc")
                        ),
                        dmq(
                                mustNot(), term("+")
                        ),
                        dmq(
                                mustNot(), term("de")
                        )

                ));

        assertThat(parser.parse("---"),
                bq(

                        dmq(
                                mustNot(), term("--")
                        )

                ));
    }


    @Test
    public void testThatFieldnameIsRecognized() {
        FieldAwareWhiteSpaceQuerqyParser parser = new FieldAwareWhiteSpaceQuerqyParser();
        assertThat(parser.parse("f1:abc f2:d"),
                bq(
                        dmq(
                                term("f1","abc")
                        ),
                        dmq(
                                term("f2","d")
                        )

                ));
    }

    @Test
    public void testThatFieldnameCanBeCombinedWithOperator() {
        FieldAwareWhiteSpaceQuerqyParser parser = new FieldAwareWhiteSpaceQuerqyParser();
        assertThat(parser.parse("-f1:abc +f2:d f3:ef"),
                bq(
                        dmq(
                                mustNot(), term("f1","abc")
                        ),
                        dmq(
                                must(), term("f2","d")
                        ),
                        dmq(
                                term("f3","ef")
                        )


                ));
    }

    @Test
    public void testThatInitialColonBecomesPartOfValue() {
        FieldAwareWhiteSpaceQuerqyParser parser = new FieldAwareWhiteSpaceQuerqyParser();
        assertThat(parser.parse(":abc +:d ef :+gh ::ij"),
                bq(
                        dmq(
                                term(":abc")
                        ),
                        dmq(
                                must(), term(":d")
                        ),
                        dmq(
                                term("ef")
                        ),
                        dmq(
                                term(":+gh")
                        )
                        ,
                        dmq(
                                term("::ij")
                        )


                ));
    }

    @Test
    public void testThatOnlyTheFirstColonCreatesAFieldName() {
        FieldAwareWhiteSpaceQuerqyParser parser = new FieldAwareWhiteSpaceQuerqyParser();
        assertThat(parser.parse("f1:ab:c "),
                bq(
                        dmq(
                                term("f1", "ab:c")
                        )
                ));
    }

    @Test
    public void testThatColonAtTheEndBecomesPartOfTheValue() {
        FieldAwareWhiteSpaceQuerqyParser parser = new FieldAwareWhiteSpaceQuerqyParser();
        assertThat(parser.parse("f1:ab: cde: "),
                bq(
                        dmq(
                                term("f1", "ab:")
                        ),
                        dmq(
                                term("cde:")
                        )
                ));
    }

}
