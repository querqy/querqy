package querqy.rewrite.commonrules;

import static org.junit.Assert.*;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.Matchers.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import querqy.parser.WhiteSpaceQuerqyParserFactory;
import querqy.rewrite.commonrules.model.*;
import querqy.rewrite.commonrules.model.BoostInstruction.BoostDirection;

public class LineParserTest {

    @Test
    public void testParseTermValueOnly() {
        Term term = LineParser.parseTerm("abc");
        assertEquals(3, term.length());
        assertArrayEquals(new char[] {'a', 'b', 'c'},new char[] {term.charAt(0), term.charAt(1), term.charAt(2)});
        assertFalse(term instanceof PrefixTerm);
        assertNull(term.getFieldNames());
    }
    
    @Test
    public void testParseSingleLetterValue() throws Exception {
        Term term = LineParser.parseTerm("a");
        assertEquals(1, term.length());
        assertArrayEquals(new char[] {'a'},new char[] {term.charAt(0)});
        assertFalse(term instanceof PrefixTerm);
        assertNull(term.getFieldNames());
    }
    
    @Test
    public void testParseTermWithFieldName() throws Exception {
        Term term = LineParser.parseTerm("f1:abc");
        assertEquals(3, term.length());
        assertArrayEquals(new char[] {'a', 'b', 'c'},new char[] {term.charAt(0), term.charAt(1), term.charAt(2)});
        assertFalse(term instanceof PrefixTerm);
        assertEquals(Arrays.asList("f1"), term.getFieldNames());
    }
    
    @Test
    public void testParseSingleLetterValueWithFieldName() throws Exception {
        Term term = LineParser.parseTerm("f1:a");
        assertEquals(1, term.length());
        assertArrayEquals(new char[] {'a'},new char[] {term.charAt(0)});
        assertFalse(term instanceof PrefixTerm);
        assertEquals(Arrays.asList("f1"), term.getFieldNames());
    }
    
    @Test
    public void testParseTermWithFieldNames() throws Exception {
        Term term = LineParser.parseTerm("{f1,f2}:abc");
        assertEquals(3, term.length());
        assertArrayEquals(new char[] {'a', 'b', 'c'},new char[] {term.charAt(0), term.charAt(1), term.charAt(2)});
        assertFalse(term instanceof PrefixTerm);
        assertThat(term.getFieldNames(), containsInAnyOrder("f1", "f2"));
    }
    
    @Test
    public void testParseTermWithFieldNamesContainingSpace() throws Exception {
        assertThat(LineParser.parseTerm("{ f1 , f2 }:abc"), term("abc", "f1", "f2"));
    }
    
    @Test
    public void testParsePrefixOnly() {
        Term term = LineParser.parseTerm("abc*");
        assertEquals(3, term.length());
        assertArrayEquals(new char[] {'a', 'b', 'c'},new char[] {term.charAt(0), term.charAt(1), term.charAt(2)});
        assertTrue(term instanceof PrefixTerm);
        assertNull(term.getFieldNames());
    }
    
    @Test
    public void testParseSingleLetterPrefix() throws Exception {
        assertThat(LineParser.parseTerm("a*"), prefix("a"));
    }
    
    @Test
    public void testParsePrefixWithFieldName() throws Exception {
        assertThat(LineParser.parseTerm("f1:abc*"), prefix("abc", "f1"));
    }
    
    @Test
    public void testParsePrefixWithFieldNames() throws Exception {
        assertThat(LineParser.parseTerm("{f1,f2}:abc*"), prefix("abc", "f1", "f2"));
    }
    
    @Test
    public void testThatWildcardOnlyTermIsNotAllowed() throws Exception {
        try {
            LineParser.parseTerm("*");
            fail("Wildcard-only term must not be allowed");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
    
    @Test
    public void testThatWildcardOnlyTermIsNotAllowedWithFieldName() throws Exception {
        try {
            LineParser.parseTerm("f1:*");
            fail("Wildcard-only term must not be allowed with fieldname");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
    
    @Test
    public void testThatWildcardOnlyTermIsNotAllowedWithFieldNames() throws Exception {
        try {
            LineParser.parseTerm("{f1,f2}:*");
            fail("Wildcard-only term must not be allowed with fieldname");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
    
    @Test
    public void testThatWildcardCannotBeFollowedByRightBoundary() throws Exception {
        Object parseResult = LineParser.parseInput("a" + LineParser.WILDCARD + LineParser.BOUNDARY);
        assertEquals("Wildcard should not be allowed before right boundary", 
                new ValidationError(LineParser.WILDCARD + " cannot be combined with right boundary"), parseResult);
    }
    
    @Test
    public void testThatWildcardCanBeCombinedWithLeftBoundary() throws Exception {
        Object parseResult = LineParser.parseInput(LineParser.BOUNDARY + "a" + LineParser.WILDCARD);
        assertTrue(parseResult instanceof Input);
        Input input = (Input) parseResult;
        assertTrue(input.requiresLeftBoundary());
        assertFalse(input.requiresRightBoundary());
    }
    
    @Test
    public void testThatBoundariesAreParsedInInput() throws Exception {
        Object parseResult = LineParser.parseInput(LineParser.BOUNDARY + "a" + LineParser.BOUNDARY);
        assertTrue(parseResult instanceof Input);
        Input input = (Input) parseResult;
        assertTrue(input.requiresLeftBoundary());
        assertTrue(input.requiresRightBoundary());
    }
    
    @Test
    public void testThatBoundariesAreParsedInOtherwiseEmptyInput() throws Exception {
        Object parseResult = LineParser.parseInput(LineParser.BOUNDARY + "" + LineParser.BOUNDARY);
        assertTrue(parseResult instanceof Input);
        Input input = (Input) parseResult;
        assertTrue(input.requiresLeftBoundary());
        assertTrue(input.requiresRightBoundary());   
    }

    @Test
    public void testThatBoostInstructionWithSingleLetterTermIsAccepted() throws Exception {
        String line = "UP: x";
        String lcLine = line.toLowerCase();
        final Object instruction = LineParser
                .parseBoostInstruction(line, lcLine, 2, BoostDirection.UP, new WhiteSpaceQuerqyParserFactory());
        assertTrue(instruction instanceof BoostInstruction);
    }

    @Test
    public void testThatBoostInstructionWithSingleLetterTermAndBoostFactorIsAccepted() throws Exception {
        String line = "UP(5): x";
        String lcLine = line.toLowerCase();
        final Object instruction = LineParser
                .parseBoostInstruction(line, lcLine, 2, BoostDirection.UP, new WhiteSpaceQuerqyParserFactory());
        assertTrue(instruction instanceof BoostInstruction);
    }

    @Test
    public void testThatPlayholdersAreParsedForBoostInstruction() throws Exception {
        String line = "UP(500): 3$1";
        String lcLine = line.toLowerCase();
        final Object instruction = LineParser
                .parseBoostInstruction(line, lcLine, 2, BoostDirection.UP, new WhiteSpaceQuerqyParserFactory());
        assertTrue(instruction instanceof BoostInstruction);
        assertTrue(((BoostInstruction) instruction).hasPlaceHolderInBoostQuery());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testParseTermExpressionSingleTerm() {
        assertThat((List<Term>) LineParser.parseTermExpression("abc"), contains(term("abc")));

    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testParseTermExpressionSingleLetter() {
        assertThat((List<Term>) LineParser.parseTermExpression("a"), contains(term("a")));

    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testParseTermExpressionMultipleTerms() {
        assertThat((List<Term>) LineParser.parseTermExpression("abc def"), contains(term("abc"), term("def")));

    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testParseTermExpressionMultiplePrefixes() {
        assertThat((List<Term>) LineParser.parseTermExpression("abc* def*"), contains(prefix("abc"), prefix("def")));
        
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testParseTermExpressionMixed() {
        assertThat((List<Term>) LineParser.parseTermExpression("abc* def ghij* klmn"), contains(prefix("abc"), term("def"), prefix("ghij"), term("klmn")));
    }
    
    @Test
    public void testInputWithWildcard() {
        assertTrue("parseInput must not allow wildcard in the middle", LineParser.parseInput("abc* def ghij*") instanceof ValidationError);

    }
    
    @Test
    public void testParseTermExpressionDoesNotAllowAWildCardOnly() {
        assertTrue("parseTermExpression must not allow single wild card", LineParser.parseTermExpression("*") instanceof ValidationError);
    }
    
    @Test
    public void testThatCaseIsPreservedInDecorateInstruction() throws Exception {
        Input input = (Input) LineParser.parseInput("in");
        assertEquals(new DecorateInstruction("Some Deco"), LineParser.parse("DECORATE: Some Deco", input, null));
    }

    TermMatcher term(String value, String...fieldNames) {
        return new TermMatcher(Term.class, value, fieldNames);
    }
    
    TermMatcher prefix(String value, String...fieldNames) {
        return new TermMatcher(PrefixTerm.class, value, fieldNames);
    }
    
    private static class TermMatcher extends TypeSafeMatcher<Term> {
        
        final String value;
        final String[] fieldNames;
        final Class<?> clazz;
        
        public TermMatcher(Class<?> clazz, String value, String...fieldNames) {
            this.clazz = clazz;
            this.fieldNames = fieldNames.length == 0 ? null : fieldNames;
            this.value = value;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("value: ").appendValue(value).appendText("fieldNames: " ).appendValue(fieldNames);
        }

        @Override
        protected boolean matchesSafely(Term item) {
            return item.getClass().equals(clazz)
                    && item.compareTo(value) == 0
                    && ((fieldNames == null && null == item.getFieldNames()) 
                        || 
                        ((fieldNames != null) 
                                && (item.getFieldNames() != null)
                                && new HashSet<String>(Arrays.asList(fieldNames)).equals(new HashSet<>(item.getFieldNames()))))
                            ;
           
        }
        
    }


}
