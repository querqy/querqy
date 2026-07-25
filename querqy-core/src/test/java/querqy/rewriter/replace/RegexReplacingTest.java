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
package querqy.rewriter.replace;

import java.util.Collections;
import java.util.Optional;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import querqy.rewriter.regexreplace.RegexReplacing;


public class RegexReplacingTest {

    @Test(timeout = 10_000)
    public void testReplacementDoesNotStackOverflowOnManySegments() {
        // A query with many independently matchable segments used to recurse once per
        // segment with no depth limit, overflowing the stack. This must complete normally.
        final RegexReplacing regexReplacing = new RegexReplacing(true, null);
        regexReplacing.put("a", "x");

        final String input = String.join(" ", Collections.nCopies(300, "a"));
        final Optional<RegexReplacing.ReplacementResult> result = regexReplacing.replace(input);

        assertTrue(result.isPresent());
    }

    @Test(timeout = 5_000)
    public void testReplacementScalesLinearlyWithManySegments() {
        // Matching used to wrap every pattern in a "skip any number of leading/trailing words"
        // regex so a single NFA simulation could find a match anywhere in the string. That
        // caused the number of live simulation states (and so the total work) to grow with
        // input length, making this exact scenario (a few thousand matchable segments) time
        // out. Matching is now done by searching token-aligned candidate substrings directly,
        // so this completes quickly.
        final RegexReplacing regexReplacing = new RegexReplacing(true, null);
        regexReplacing.put("a", "x");

        final String input = String.join(" ", Collections.nCopies(5_000, "a"));
        final Optional<RegexReplacing.ReplacementResult> result = regexReplacing.replace(input);

        assertTrue(result.isPresent());
    }

    @Test
    public void testPatternDoesNotMatchWithinALongerWord() {
        // Matches must still be aligned to whole-token boundaries, not just found anywhere
        // as a substring.
        final RegexReplacing regexReplacing = new RegexReplacing(true, null);
        regexReplacing.put("cat", "X");

        assertNoReplacement(regexReplacing, "cats");
        assertNoReplacement(regexReplacing, "scat");
        assertNoReplacement(regexReplacing, "cats dog");
    }

    @Test
    public void testReplacementToleratesIrregularWhitespace() {
        // Tokens are found by splitting on runs of one or more spaces, so leading/trailing/
        // repeated spaces around an otherwise-matchable token do not prevent a match.
        final RegexReplacing regexReplacing = new RegexReplacing(true, null);
        regexReplacing.put("cat", "x");

        assertReplacement(regexReplacing, " cat", "x");
        assertReplacement(regexReplacing, "cat  dog", "x  dog");
    }

    private static void assertNoReplacement(final RegexReplacing regexReplacing, final String input) {
        assertTrue(regexReplacing.replace(input).isEmpty());
    }

    @Test(timeout = 5_000)
    public void testReplacementScalesLinearlyAcrossRecursiveSplits() {
        // applyReplacement() used to recurse into a freshly re-scanned prefix/suffix substring
        // after every match, which multiplied a full rescan by the recursion depth. Matches for
        // the whole input are now computed once and looked up by position for each recursive
        // split, so this completes quickly even for many independently matchable segments.
        final RegexReplacing regexReplacing = new RegexReplacing(true, null);
        regexReplacing.put("a", "x");

        final String input = String.join(" ", Collections.nCopies(100_000, "a"));
        final Optional<RegexReplacing.ReplacementResult> result = regexReplacing.replace(input);

        assertTrue(result.isPresent());
    }

    @Test
    public void testMultiTokenMatchAtRecursiveSplitBoundary() {
        // "c" is the best (only non-overlapping) match, splitting "a b c" into the prefix
        // window "a b". The multi-token pattern "a b" must still be found there, exercising the
        // case where a match's end lands exactly on the (trimmed) window boundary.
        final RegexReplacing regexReplacing = new RegexReplacing(true, null);
        regexReplacing.put("a b", "y");
        regexReplacing.put("c", "z");

        assertReplacement(regexReplacing, "a b c", "y z");
    }

    @Test
    public void testOverlappingCandidateAtTiedStartPositionIsNotDoubleMatched() {
        // "a" and "a b" both start at position 0. Whichever the top-level pick is, the
        // recursive split by position must not let a candidate spill across a split point.
        final RegexReplacing regexReplacing = new RegexReplacing(true, null);
        regexReplacing.put("a", "x");
        regexReplacing.put("a b", "y");
        regexReplacing.put("c", "z");

        assertReplacement(regexReplacing, "a b c", "y z");
    }

    @Test
    public void testReplacementWithoutPlaceholderConsideringCase() {
        RegexReplacing regexReplacing = new RegexReplacing(false, null);
        regexReplacing.put("abc", "ABC");
        assertReplacement(regexReplacing, "aBc abc abcd abc abck", "aBc ABC abcd ABC abck");
    }

    @Test
    public void testReplacementWithoutPlaceholderIgnoringCase() {
        RegexReplacing regexReplacing = new RegexReplacing(true, null);
        regexReplacing.put("abc", "ABC");
        assertReplacement(regexReplacing, "aBc abc abcd abc abck", "abc abc abcd abc abck");
    }

    @Test
    public void testReplacement() {
        RegexReplacing regexReplacing = new RegexReplacing(true, null);
        regexReplacing.put("ak", "dy");
        assertReplacement(regexReplacing, "ak ", "dy");
    }



    private static void assertReplacement(final RegexReplacing regexReplacing, final String input,
                                          final String expected) {
        final Optional<RegexReplacing.ReplacementResult> resultOptional = regexReplacing.replace(input);
        assertTrue(resultOptional.isPresent());
        final RegexReplacing.ReplacementResult replacementResult = resultOptional.get();
        assertEquals(expected, replacementResult.replacement());
    }

    @Test
    public void testMultiplePatterns() {
        RegexReplacing regexReplacing = new RegexReplacing(true, null);
        regexReplacing.put("a", "x");
        regexReplacing.put("d", "y");
        assertReplacement(regexReplacing, "a", "x");

    }

    @Test
    public void testReplacementWithPlaceholderAndGroups() {
        RegexReplacing regexReplacing = new RegexReplacing(false, null);
        regexReplacing.put("abc", "ABC");
        regexReplacing.put("def\\d+", "DEF");
        regexReplacing.put("gh (ikk)+ lmn", "XYZ");
        regexReplacing.put("(\\d+) x (\\d+) ((\\d{1,2})m)", "${1}x${2} ${4}00mm");

        assertReplacement(regexReplacing, "aBc abc abcd abc abck", "aBc ABC abcd ABC abck");
        assertReplacement(regexReplacing, "abc hello", "ABC hello");
        assertReplacement(regexReplacing, "hello abc abc bye", "hello ABC ABC bye");
        assertReplacement(regexReplacing, "def12 abc", "DEF ABC");
        assertReplacement(regexReplacing, "hello gh ikkikk lmn bye", "hello XYZ bye");
        assertReplacement(regexReplacing, "763 x 23 2m", "763x23 200mm");

    }

}