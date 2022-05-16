package querqy.rewrite.commonrules;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.Matchers.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import org.assertj.core.api.Assertions;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import querqy.model.*;
import querqy.model.convert.builder.BooleanQueryBuilder;
import querqy.rewrite.commonrules.model.BoostInstruction;
import querqy.rewrite.commonrules.model.*;
import querqy.rewrite.commonrules.model.BoostInstruction.BoostDirection;
import querqy.rewrite.commonrules.model.BoostInstruction.BoostMethod;
import querqy.rewrite.commonrules.model.DecorateInstruction;
import querqy.rewrite.commonrules.model.DeleteInstruction;
import querqy.rewrite.commonrules.model.FilterInstruction;
import querqy.rewrite.commonrules.model.Instruction;
import querqy.rewrite.commonrules.model.PrefixTerm;
import querqy.rewrite.commonrules.model.Term;
import querqy.rewrite.commonrules.select.booleaninput.BooleanInputParser;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInputElement;

@Deprecated
public class LineParserTest {

    static final Input SIMPLE_INPUT_PATTERN = new Input.SimpleInput(singletonList(new Term("a".toCharArray(), 0, 1,
            null)), false, false, "a");

    static final Input BOOLEAN_INPUT_PATTERN = new Input.BooleanInput(singletonList(new BooleanInputElement("input",
            BooleanInputElement.Type.TERM)),
            new BooleanInputParser(), "input");

    private Locale locale;

    @Before
    public void saveDefaultLocale() {
        locale = Locale.getDefault();
    }

    @After
    public void restoreDefaultLocale() {
        Locale.setDefault(locale);
    }

    @Test(expected = RuleParseException.class)
    public void testInvalidDefinitionOfParametrizedRawQueryForMissingClosingChars() throws RuleParseException {
        LineParser.parseRawQuery(" query %% query", Clause.Occur.SHOULD);
    }

    @Test(expected = RuleParseException.class)
    public void testInvalidDefinitionOfParametrizedRawQueryForMissingClosingChars2() throws RuleParseException {
        LineParser.parseRawQuery(" query %% param %% query %%", Clause.Occur.SHOULD);
    }

    @Test(expected = RuleParseException.class)
    public void testInvalidDefinitionOfParametrizedRawQueryForEmptyQuery() throws RuleParseException {
        LineParser.parseRawQuery("%%", Clause.Occur.SHOULD);
    }

    @Test
    public void testRawQueryParsingFromRule() {
        final WhiteSpaceQuerqyParserFactory factory = new WhiteSpaceQuerqyParserFactory();

        Object result;

        result = LineParser.parse("FILTER: * query", SIMPLE_INPUT_PATTERN, factory);
        Assertions.assertThat(result).isInstanceOf(FilterInstruction.class);
        Assertions.assertThat(result).isEqualTo(new FilterInstruction(
                new StringRawQuery(null, "query", Clause.Occur.MUST, false)));

        result = LineParser.parse("FILTER: * q %% param %% q", SIMPLE_INPUT_PATTERN, factory);
        Assertions.assertThat(result).isInstanceOf(FilterInstruction.class);
        Assertions.assertThat(result).isEqualTo(new FilterInstruction(
                new ParametrizedRawQuery(null,
                        Arrays.asList(
                                new ParametrizedRawQuery.Part("q ", ParametrizedRawQuery.Part.Type.QUERY_PART),
                                new ParametrizedRawQuery.Part(" param ", ParametrizedRawQuery.Part.Type.PARAMETER),
                                new ParametrizedRawQuery.Part(" q", ParametrizedRawQuery.Part.Type.QUERY_PART)),
                        Clause.Occur.MUST,
                        false)));

        result = LineParser.parse("UP(1.0): * q %% param %% q", SIMPLE_INPUT_PATTERN, factory);
        Assertions.assertThat(result).isInstanceOf(BoostInstruction.class);
        Assertions.assertThat(result).isEqualTo(new BoostInstruction(
                new ParametrizedRawQuery(
                        null,
                        Arrays.asList(
                                new ParametrizedRawQuery.Part("q ", ParametrizedRawQuery.Part.Type.QUERY_PART),
                                new ParametrizedRawQuery.Part(" param ", ParametrizedRawQuery.Part.Type.PARAMETER),
                                new ParametrizedRawQuery.Part(" q", ParametrizedRawQuery.Part.Type.QUERY_PART)),
                        Clause.Occur.SHOULD,
                        false),
                BoostDirection.UP,
                BoostMethod.ADDITIVE,
                1.0f));
    }

    @Test
    public void testParseRawQuery() throws RuleParseException {
        RawQuery rawQuery;

        rawQuery = LineParser.parseRawQuery("query %% param %% query2", Clause.Occur.SHOULD);
        Assertions.assertThat(rawQuery).isInstanceOf(ParametrizedRawQuery.class);
        Assertions.assertThat(((ParametrizedRawQuery) rawQuery).getParts()).hasSize(3);
        Assertions.assertThat(((ParametrizedRawQuery) rawQuery).getParts().get(0).part).isEqualTo("query ");
        Assertions.assertThat(((ParametrizedRawQuery) rawQuery).getParts().get(1).part).isEqualTo(" param ");
        Assertions.assertThat(((ParametrizedRawQuery) rawQuery).getParts().get(2).part).isEqualTo(" query2");
        Assertions.assertThat(((ParametrizedRawQuery) rawQuery).getParts().get(0).type)
                .isEqualTo(ParametrizedRawQuery.Part.Type.QUERY_PART);
        Assertions.assertThat(((ParametrizedRawQuery) rawQuery).getParts().get(1).type)
                .isEqualTo(ParametrizedRawQuery.Part.Type.PARAMETER);
        Assertions.assertThat(((ParametrizedRawQuery) rawQuery).getParts().get(2).type)
                .isEqualTo(ParametrizedRawQuery.Part.Type.QUERY_PART);

        rawQuery = LineParser.parseRawQuery("query%%param%%query2", Clause.Occur.SHOULD);
        Assertions.assertThat(((ParametrizedRawQuery) rawQuery).getParts()).hasSize(3);

        rawQuery = LineParser.parseRawQuery(" %%  %% \n\t\r %% param %%query2", Clause.Occur.SHOULD);
        Assertions.assertThat(((ParametrizedRawQuery) rawQuery).getParts()).hasSize(5);

        rawQuery = LineParser.parseRawQuery("query %% param %% query1 %% param2 %% query2", Clause.Occur.SHOULD);
        Assertions.assertThat(((ParametrizedRawQuery) rawQuery).getParts()).hasSize(5);

        rawQuery = LineParser.parseRawQuery("query query2", Clause.Occur.SHOULD);
        Assertions.assertThat(rawQuery).isInstanceOf(StringRawQuery.class);
    }

    @Test
    public void testThatBooleanInputOnlyAllowsCertainInstructions() {
        final WhiteSpaceQuerqyParserFactory factory = new WhiteSpaceQuerqyParserFactory();


        Assertions.assertThat(LineParser.parse("FILTER: f", BOOLEAN_INPUT_PATTERN, factory))
                .isInstanceOf(Instruction.class);

        Assertions.assertThat(LineParser.parse("UP: f", BOOLEAN_INPUT_PATTERN, factory))
                .isInstanceOf(Instruction.class);

        Assertions.assertThat(LineParser.parse("DOWN: f", BOOLEAN_INPUT_PATTERN, factory))
                .isInstanceOf(Instruction.class);

        Assertions.assertThat(LineParser.parse("DECORATE: f", BOOLEAN_INPUT_PATTERN, factory))
                .isInstanceOf(Instruction.class);

        Assertions.assertThat(LineParser.parse("SYNONYM: f", BOOLEAN_INPUT_PATTERN, factory))
                .isInstanceOf(ValidationError.class);

        Assertions.assertThat(LineParser.parse("DELETE: f", BOOLEAN_INPUT_PATTERN, factory))
                .isInstanceOf(ValidationError.class);
    }

    @Test
    public void testPredicatesWithVaryingLocales() {

        final WhiteSpaceQuerqyParserFactory rhsParserFactory = new WhiteSpaceQuerqyParserFactory();

        for (final Locale locale: Arrays.asList(Locale.ENGLISH, new Locale("tr", "CY"))) {

            Locale.setDefault(locale);

            assertTrue(LineParser.parse("filter: f", SIMPLE_INPUT_PATTERN, rhsParserFactory) instanceof
                    FilterInstruction);
            assertTrue(LineParser.parse("FILTER: f", SIMPLE_INPUT_PATTERN, rhsParserFactory) instanceof
                    FilterInstruction);
            assertTrue(LineParser.parse("up: f", SIMPLE_INPUT_PATTERN, rhsParserFactory) instanceof
                    BoostInstruction);
            assertTrue(LineParser.parse("UP: f", SIMPLE_INPUT_PATTERN, rhsParserFactory) instanceof
                    BoostInstruction);
            assertTrue(LineParser.parse("down: f", SIMPLE_INPUT_PATTERN, rhsParserFactory) instanceof
                    BoostInstruction);
            assertTrue(LineParser.parse("DOWN: f", SIMPLE_INPUT_PATTERN, rhsParserFactory) instanceof
                    BoostInstruction);
            assertTrue(LineParser.parse("delete: a", SIMPLE_INPUT_PATTERN, rhsParserFactory) instanceof
                    DeleteInstruction);
            assertTrue(LineParser.parse("DELETE: a", SIMPLE_INPUT_PATTERN, rhsParserFactory) instanceof
                    DeleteInstruction);

        }

    }

    @Test
    public void testParseTermValueOnly() {
        Term term = LineParser.parseTerm("abc");
        assertEquals(3, term.length());
        assertArrayEquals(new char[] {'a', 'b', 'c'},new char[] {term.charAt(0), term.charAt(1), term.charAt(2)});
        assertFalse(term instanceof PrefixTerm);
        assertNull(term.getFieldNames());
    }
    
    @Test
    public void testParseSingleLetterValue() {
        Term term = LineParser.parseTerm("a");
        assertEquals(1, term.length());
        assertArrayEquals(new char[] {'a'},new char[] {term.charAt(0)});
        assertFalse(term instanceof PrefixTerm);
        assertNull(term.getFieldNames());
    }
    
    @Test
    public void testParseTermWithFieldName() {
        Term term = LineParser.parseTerm("f1:abc");
        assertEquals(3, term.length());
        assertArrayEquals(new char[] {'a', 'b', 'c'},new char[] {term.charAt(0), term.charAt(1), term.charAt(2)});
        assertFalse(term instanceof PrefixTerm);
        assertEquals(singletonList("f1"), term.getFieldNames());
    }
    
    @Test
    public void testParseSingleLetterValueWithFieldName() {
        Term term = LineParser.parseTerm("f1:a");
        assertEquals(1, term.length());
        assertArrayEquals(new char[] {'a'},new char[] {term.charAt(0)});
        assertFalse(term instanceof PrefixTerm);
        assertEquals(singletonList("f1"), term.getFieldNames());
    }
    
    @Test
    public void testParseTermWithFieldNames() {
        Term term = LineParser.parseTerm("{f1,f2}:abc");
        assertEquals(3, term.length());
        assertArrayEquals(new char[] {'a', 'b', 'c'},new char[] {term.charAt(0), term.charAt(1), term.charAt(2)});
        assertFalse(term instanceof PrefixTerm);
        assertThat(term.getFieldNames(), containsInAnyOrder("f1", "f2"));
    }
    
    @Test
    public void testParseTermWithFieldNamesContainingSpace() {
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
    public void testParseSingleLetterPrefix() {
        assertThat(LineParser.parseTerm("a*"), prefix("a"));
    }
    
    @Test
    public void testParsePrefixWithFieldName() {
        assertThat(LineParser.parseTerm("f1:abc*"), prefix("abc", "f1"));
    }
    
    @Test
    public void testParsePrefixWithFieldNames() {
        assertThat(LineParser.parseTerm("{f1,f2}:abc*"), prefix("abc", "f1", "f2"));
    }
    
    @Test
    public void testThatWildcardOnlyTermIsNotAllowed() {
        try {
            LineParser.parseTerm("*");
            fail("Wildcard-only term must not be allowed");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
    
    @Test
    public void testThatWildcardOnlyTermIsNotAllowedWithFieldName() {
        try {
            LineParser.parseTerm("f1:*");
            fail("Wildcard-only term must not be allowed with fieldname");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
    
    @Test
    public void testThatWildcardOnlyTermIsNotAllowedWithFieldNames() {
        try {
            LineParser.parseTerm("{f1,f2}:*");
            fail("Wildcard-only term must not be allowed with fieldname");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
    
    @Test
    public void testThatWildcardCannotBeFollowedByRightBoundary() {
        Object parseResult = LineParser.parseInput("a" + LineParser.WILDCARD + LineParser.BOUNDARY);
        assertEquals("Wildcard should not be allowed before right boundary", 
                new ValidationError(LineParser.WILDCARD + " cannot be combined with right boundary"), parseResult);
    }
    
    @Test
    public void testThatWildcardCanBeCombinedWithLeftBoundary() {
        Object parseResult = LineParser.parseInput(LineParser.BOUNDARY + "a" + LineParser.WILDCARD);
        assertTrue(parseResult instanceof Input.SimpleInput);
        Input.SimpleInput input = (Input.SimpleInput) parseResult;
        assertTrue(input.isRequiresLeftBoundary());
        assertFalse(input.isRequiresRightBoundary());
    }
    
    @Test
    public void testThatBoundariesAreParsedInInput() {
        Object parseResult = LineParser.parseInput(LineParser.BOUNDARY + "a" + LineParser.BOUNDARY);
        assertTrue(parseResult instanceof Input.SimpleInput);
        Input.SimpleInput input = (Input.SimpleInput) parseResult;
        assertTrue(input.isRequiresLeftBoundary());
        assertTrue(input.isRequiresRightBoundary());
    }
    
    @Test
    public void testThatBoundariesAreParsedInOtherwiseEmptyInput() {
        Object parseResult = LineParser.parseInput(LineParser.BOUNDARY + "" + LineParser.BOUNDARY);
        assertTrue(parseResult instanceof Input.SimpleInput);
        Input.SimpleInput input = (Input.SimpleInput) parseResult;
        assertTrue(input.isRequiresLeftBoundary());
        assertTrue(input.isRequiresRightBoundary());
    }

    @Test
    public void testThatSpaceVsInput() {
        assertEquals(new InputString("input"), LineParser.parse("input =>", null, null));
        assertEquals(new InputString("in put"), LineParser.parse(" in put  =>", null, null));
        assertEquals(new InputString("input"), LineParser.parse(" input=>", null, null));
        assertTrue(LineParser.parse(" =>", null, null) instanceof ValidationError);
    }


    @Test
    public void testThatBoostInstructionWithSingleLetterTermIsAccepted() {
        String line = "UP: x";
        String lcLine = line.toLowerCase();
        final Object instruction = LineParser
                .parseBoostInstruction(line, lcLine, 2, BoostDirection.UP, BoostMethod.ADDITIVE, new WhiteSpaceQuerqyParserFactory());
        assertTrue(instruction instanceof BoostInstruction);
    }

    @Test
    public void testThatBoostInstructionWithSingleLetterTermAndBoostFactorIsAccepted() {
        String line = "UP(5): x";
        String lcLine = line.toLowerCase();
        final Object instruction = LineParser
                .parseBoostInstruction(line, lcLine, 2, BoostDirection.UP, BoostMethod.ADDITIVE, new WhiteSpaceQuerqyParserFactory());
        assertTrue(instruction instanceof BoostInstruction);
    }

    @Test
    public void testThatBoostInstructionsAreParsedAsMultiplicativeIfConfigured() {
        String line = "UP(5): x";
        final Object instruction = LineParser.parse(line, SIMPLE_INPUT_PATTERN, new WhiteSpaceQuerqyParserFactory(), true);
        assertTrue(instruction instanceof BoostInstruction);
        assertThat(((BoostInstruction) instruction).getDirection(), is(BoostDirection.UP));
        assertThat(((BoostInstruction) instruction).getBoostMethod(), is(BoostMethod.MULTIPLICATIVE));
    }

    @Test
    public void testThatPlayholdersAreParsedForBoostInstruction() {
        String line = "UP(500): 3$1";
        String lcLine = line.toLowerCase();
        final Object instruction = LineParser
                .parseBoostInstruction(line, lcLine, 2, BoostDirection.UP, BoostMethod.ADDITIVE, new WhiteSpaceQuerqyParserFactory());
        assertTrue(instruction instanceof BoostInstruction);
        assertTrue(((BoostInstruction) instruction).hasPlaceHolderInBoostQuery());
    }
    
    @Test
    public void testUnweightedSynonym() {
        String line = "SYNONYM: 3$1";
        final Object instruction = LineParser.parse(line, SIMPLE_INPUT_PATTERN, new WhiteSpaceQuerqyParserFactory());
        assertTrue(instruction instanceof SynonymInstruction);
        assertThat(((SynonymInstruction) instruction).getTermBoost(), is(SynonymInstruction.DEFAULT_TERM_BOOST));
    }
    
    @Test
    public void testFallbackToUnweightedSynonym() {
        String line = "SYNONYM(1.0): 3$1";
        final Object instruction = LineParser.parse(line, SIMPLE_INPUT_PATTERN, new WhiteSpaceQuerqyParserFactory());
        assertTrue(instruction instanceof SynonymInstruction);
        assertThat(((SynonymInstruction) instruction).getTermBoost(), is(SynonymInstruction.DEFAULT_TERM_BOOST));
    }

    @Test
    public void testWeightedSynonym() {
        String line = "SYNONYM(0.5): 3$1";
        final Object instruction = LineParser.parse(line, SIMPLE_INPUT_PATTERN, new WhiteSpaceQuerqyParserFactory());
        assertTrue(instruction instanceof SynonymInstruction);
        assertThat(((SynonymInstruction) instruction).getTermBoost(), is(0.5f));
    }
        
    @Test
    public void testMalformedWeightedSynonym() {
        Object instruction = LineParser.parse("SYNONYM(): 3$1", SIMPLE_INPUT_PATTERN,
                new WhiteSpaceQuerqyParserFactory());
        assertTrue(instruction instanceof ValidationError);
        
        instruction = LineParser.parse("SYNONYM(-): 3$1", SIMPLE_INPUT_PATTERN, new WhiteSpaceQuerqyParserFactory());
        assertTrue(instruction instanceof ValidationError);
        
        instruction = LineParser.parse("SYNONYM(-0.5): 3$1", SIMPLE_INPUT_PATTERN, new WhiteSpaceQuerqyParserFactory());
        assertTrue(instruction instanceof ValidationError);
        
        instruction = LineParser.parse("SYNONYM(3e): 3$1", SIMPLE_INPUT_PATTERN, new WhiteSpaceQuerqyParserFactory());
        assertTrue(instruction instanceof ValidationError);
        
        instruction = LineParser.parse("SYNONYM(sausage): 3$1", SIMPLE_INPUT_PATTERN,
                new WhiteSpaceQuerqyParserFactory());
        assertTrue(instruction instanceof ValidationError);
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
        assertThat((List<Term>) LineParser.parseTermExpression("abc* def ghij* klmn"), contains(prefix("abc"),
                term("def"), prefix("ghij"), term("klmn")));
    }
    
    @Test
    public void testInputWithWildcard() {
        assertTrue("parseInput must not allow wildcard in the middle",
                LineParser.parseInput("abc* def ghij*") instanceof ValidationError);

    }
    
    @Test
    public void testParseTermExpressionDoesNotAllowAWildCardOnly() {
        assertTrue("parseTermExpression must not allow single wild card",
                LineParser.parseTermExpression("*") instanceof ValidationError);
    }
    
    @Test
    public void testThatCaseIsPreservedInDecorateInstruction() {
        assertEquals(new DecorateInstruction("Some Deco"), LineParser.parse("DECORATE: Some Deco", SIMPLE_INPUT_PATTERN,
                null));
    }

    @Test
    public void testThatDecorateInputIsInvalidIfOpeningBracketIsMissing() {
        Assertions.assertThat(LineParser.parse("DECORATEkey): Some Deco", SIMPLE_INPUT_PATTERN, null))
                .isInstanceOf(ValidationError.class);
    }

    @Test
    public void testThatDecorateInputIsInvalidIfClosingBracketIsMissing() {
        Assertions.assertThat(LineParser.parse("DECORATE(key: Some Deco", SIMPLE_INPUT_PATTERN, null))
                .isInstanceOf(ValidationError.class);
    }

    @Test
    public void testThatDecorateInputIsInvalidIfOpeningBracketAndKeyAreMissing() {
        Assertions.assertThat(LineParser.parse("DECORATE):Deco", SIMPLE_INPUT_PATTERN, null))
                .isInstanceOf(ValidationError.class);
    }

    @Test
    public void testThatDecorateInputIsInvalidIfClosingBracketAndKeyAreMissing() {
        Assertions.assertThat(LineParser.parse("DECORATE(: Some ):Deco", SIMPLE_INPUT_PATTERN, null))
                .isInstanceOf(ValidationError.class);
    }

    @Test
    public void testThatDecorateInputIsInvalidIfKeyIsMissing() {
        Assertions.assertThat(LineParser.parse("DECORATE():Deco", SIMPLE_INPUT_PATTERN, null))
                .isInstanceOf(ValidationError.class);
    }

    @Test
    public void testThatDecorateInputIsInvalidIfKeyContainsCharThatIsNotAllowed() {
        Assertions.assertThat(LineParser.parse("DECORATE(k-ey):Deco", SIMPLE_INPUT_PATTERN, null))
                .isInstanceOf(ValidationError.class);
    }

    @Test
    public void testValidDecorateKeyInput() {
        assertEquals(new DecorateInstruction("key", "value"), LineParser.parse("DECORATE(key): value",
                SIMPLE_INPUT_PATTERN, null));
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
                                && new HashSet<>(Arrays.asList(fieldNames)).equals(new HashSet<>(
                                        item.getFieldNames()))))
                            ;
           
        }
        
    }


}
