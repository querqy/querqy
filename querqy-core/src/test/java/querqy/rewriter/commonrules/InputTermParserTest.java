/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2026 Querqy Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package querqy.rewriter.commonrules;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.Matchers.contains;

import java.util.HashSet;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import querqy.model.Input;
import querqy.rewriter.commonrules.model.PrefixTerm;
import querqy.rewriter.commonrules.model.SuffixTerm;
import querqy.rewriter.commonrules.model.Term;
import querqy.rewrite.RuleParseException;

public class InputTermParserTest {

    @Test
    public void testParseTermValueOnly() {
        Term term = InputTermParser.parseTerm("abc");
        assertEquals(3, term.length());
        assertArrayEquals(new char[] {'a', 'b', 'c'},new char[] {term.charAt(0), term.charAt(1), term.charAt(2)});
        assertFalse(term instanceof PrefixTerm);
        assertNull(term.getFieldNames());
    }

    @Test
    public void testParseSingleLetterValue() {
        Term term = InputTermParser.parseTerm("a");
        assertEquals(1, term.length());
        assertArrayEquals(new char[] {'a'},new char[] {term.charAt(0)});
        assertFalse(term instanceof PrefixTerm);
        assertNull(term.getFieldNames());
    }

    @Test
    public void testParseTermWithFieldName() {
        Term term = InputTermParser.parseTerm("f1:abc");
        assertEquals(3, term.length());
        assertArrayEquals(new char[] {'a', 'b', 'c'},new char[] {term.charAt(0), term.charAt(1), term.charAt(2)});
        assertFalse(term instanceof PrefixTerm);
        assertEquals(singletonList("f1"), term.getFieldNames());
    }

    @Test
    public void testParseSingleLetterValueWithFieldName() {
        Term term = InputTermParser.parseTerm("f1:a");
        assertEquals(1, term.length());
        assertArrayEquals(new char[] {'a'},new char[] {term.charAt(0)});
        assertFalse(term instanceof PrefixTerm);
        assertEquals(singletonList("f1"), term.getFieldNames());
    }

    @Test
    public void testParseTermWithFieldNames() {
        Term term = InputTermParser.parseTerm("{f1,f2}:abc");
        assertEquals(3, term.length());
        assertArrayEquals(new char[] {'a', 'b', 'c'},new char[] {term.charAt(0), term.charAt(1), term.charAt(2)});
        assertFalse(term instanceof PrefixTerm);
        assertThat(term.getFieldNames(), containsInAnyOrder("f1", "f2"));
    }

    @Test
    public void testParseTermWithFieldNamesContainingSpace() {
        assertThat(InputTermParser.parseTerm("{ f1 , f2 }:abc"), term("abc", "f1", "f2"));
    }

    @Test
    public void testParsePrefixOnly() {
        Term term = InputTermParser.parseTerm("abc*");
        assertEquals(3, term.length());
        assertArrayEquals(new char[] {'a', 'b', 'c'},new char[] {term.charAt(0), term.charAt(1), term.charAt(2)});
        assertTrue(term instanceof PrefixTerm);
        assertNull(term.getFieldNames());
    }

    @Test
    public void testParseSingleLetterPrefix() {
        assertThat(InputTermParser.parseTerm("a*"), prefix("a"));
    }

    @Test
    public void testParsePrefixWithFieldName() {
        assertThat(InputTermParser.parseTerm("f1:abc*"), prefix("abc", "f1"));
    }

    @Test
    public void testParsePrefixWithFieldNames() {
        assertThat(InputTermParser.parseTerm("{f1,f2}:abc*"), prefix("abc", "f1", "f2"));
    }

    @Test
    public void testParseSuffixOnly() {
        Term term = InputTermParser.parseTerm("*abc");
        assertEquals(3, term.length());
        assertArrayEquals(new char[] {'a', 'b', 'c'},new char[] {term.charAt(0), term.charAt(1), term.charAt(2)});
        assertTrue(term instanceof SuffixTerm);
        assertNull(term.getFieldNames());
    }

    @Test
    public void testParseSingleLetterSuffix() {
        assertThat(InputTermParser.parseTerm("*a"), suffix("a"));
    }

    @Test
    public void testParseSuffixWithFieldName() {
        assertThat(InputTermParser.parseTerm("f1:*abc"), suffix("abc", "f1"));
    }

    @Test
    public void testParseSuffixWithFieldNames() {
        assertThat(InputTermParser.parseTerm("{f1,f2}:*abc"), suffix("abc", "f1", "f2"));
    }

    @Test
    public void testThatLeadingWildcardCanBeAtStartOfInput() {
        Input.SimpleInput input = InputTermParser.parseInput("*abc");
        assertThat(input.getInputTerms(), contains(suffix("abc")));
    }

    @Test
    public void testThatLeadingWildcardCanBeOnLastOfTwoTerms() {
        Input.SimpleInput input = InputTermParser.parseInput("abc *def");
        assertThat(input.getInputTerms(), contains(term("abc"), suffix("def")));
    }

    @Test
    public void testThatLeadingWildcardCanBeOnMiddleTerm() {
        Input.SimpleInput input = InputTermParser.parseInput("abc *def ghi");
        assertThat(input.getInputTerms(), contains(term("abc"), suffix("def"), term("ghi")));
    }

    @Test
    public void testThatOnlyOneWildcardIsAllowedPerInput() {
        assertThrows(RuleParseException.class, () -> InputTermParser.parseInput("*abc *def"));
        assertThrows(RuleParseException.class, () -> InputTermParser.parseInput("abc* *def"));
    }

    @Test
    public void testThatLeadingWildcardOnlyTermIsNotAllowed() {
        assertThrows(RuleParseException.class, () -> InputTermParser.parseInput("abc * def"));
    }

    @Test
    public void testThatLeadingWildcardCannotBeCombinedWithLeftBoundaryOnFirstTerm() {
        final RuleParseException e = assertThrows(RuleParseException.class,
                () -> InputTermParser.parseInput(SpecialChars.BOUNDARY + "*abc"));
        assertEquals(SpecialChars.WILDCARD + " cannot be combined with left boundary", e.getMessage());
    }

    @Test
    public void testThatLeadingWildcardCanBeCombinedWithLeftBoundaryWhenNotOnFirstTerm() {
        Input.SimpleInput input = InputTermParser.parseInput(SpecialChars.BOUNDARY + "abc *def");
        assertTrue(input.isRequiresLeftBoundary());
    }

    @Test
    public void testThatLeadingWildcardCanBeCombinedWithRightBoundaryOnLastTerm() {
        Input.SimpleInput input = InputTermParser.parseInput("abc *def" + SpecialChars.BOUNDARY);
        assertTrue(input.isRequiresRightBoundary());
    }

    @Test
    public void testThatFieldNamesAreNotAllowedOnOtherTermsWithLeadingWildcard() {
        assertThrows(RuleParseException.class, () -> InputTermParser.parseInput("f1:abc *def"));
    }

    @Test
    public void testThatWildcardOnlyTermIsNotAllowed() {
        try {
            InputTermParser.parseTerm("*");
            fail("Wildcard-only term must not be allowed");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testThatWildcardOnlyTermIsNotAllowedWithFieldName() {
        try {
            InputTermParser.parseTerm("f1:*");
            fail("Wildcard-only term must not be allowed with fieldname");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testThatWildcardOnlyTermIsNotAllowedWithFieldNames() {
        try {
            InputTermParser.parseTerm("{f1,f2}:*");
            fail("Wildcard-only term must not be allowed with fieldname");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testThatWildcardCannotBeFollowedByRightBoundary() {
        final RuleParseException e = assertThrows(RuleParseException.class,
                () -> InputTermParser.parseInput("a" + SpecialChars.WILDCARD + SpecialChars.BOUNDARY));
        assertEquals(SpecialChars.WILDCARD + " cannot be combined with right boundary", e.getMessage());
    }

    @Test
    public void testThatWildcardCanBeCombinedWithLeftBoundary() {
        Input.SimpleInput input = InputTermParser.parseInput(SpecialChars.BOUNDARY + "a" + SpecialChars.WILDCARD);
        assertTrue(input.isRequiresLeftBoundary());
        assertFalse(input.isRequiresRightBoundary());
    }

    @Test
    public void testThatBoundariesAreParsedInInput() {
        Input.SimpleInput input = InputTermParser.parseInput(SpecialChars.BOUNDARY + "a" + SpecialChars.BOUNDARY);
        assertTrue(input.isRequiresLeftBoundary());
        assertTrue(input.isRequiresRightBoundary());
    }

    @Test
    public void testThatBoundariesAreParsedInOtherwiseEmptyInput() {
        Input.SimpleInput input = InputTermParser.parseInput(SpecialChars.BOUNDARY + "" + SpecialChars.BOUNDARY);
        assertTrue(input.isRequiresLeftBoundary());
        assertTrue(input.isRequiresRightBoundary());
    }

    @Test
    public void testParseTermExpressionSingleTerm() {
        assertThat(InputTermParser.parseTermExpression("abc"), contains(term("abc")));
    }

    @Test
    public void testParseTermExpressionSingleLetter() {
        assertThat(InputTermParser.parseTermExpression("a"), contains(term("a")));
    }

    @Test
    public void testParseTermExpressionMultipleTerms() {
        assertThat(InputTermParser.parseTermExpression("abc def"), contains(term("abc"), term("def")));
    }

    @Test
    public void testParseTermExpressionMultiplePrefixes() {
        assertThat(InputTermParser.parseTermExpression("abc* def*"), contains(prefix("abc"), prefix("def")));
    }

    @Test
    public void testParseTermExpressionMixed() {
        assertThat(InputTermParser.parseTermExpression("abc* def ghij* klmn"), contains(prefix("abc"),
                term("def"), prefix("ghij"), term("klmn")));
    }

    @Test
    public void testInputWithWildcard() {
        assertThrows("parseInput must not allow wildcard in the middle",
                RuleParseException.class, () -> InputTermParser.parseInput("abc* def ghij*"));
    }

    @Test
    public void testParseTermExpressionDoesNotAllowAWildCardOnly() {
        assertThrows("parseTermExpression must not allow single wild card",
                RuleParseException.class, () -> InputTermParser.parseTermExpression("*"));
    }

    TermMatcher term(String value, String...fieldNames) {
        return new TermMatcher(Term.class, value, fieldNames);
    }

    TermMatcher prefix(String value, String...fieldNames) {
        return new TermMatcher(PrefixTerm.class, value, fieldNames);
    }

    TermMatcher suffix(String value, String...fieldNames) {
        return new TermMatcher(SuffixTerm.class, value, fieldNames);
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
