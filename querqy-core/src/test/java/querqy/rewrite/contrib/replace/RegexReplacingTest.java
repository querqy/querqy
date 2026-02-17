package querqy.rewrite.contrib.replace;

import java.util.Optional;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import querqy.rewrite.replace.RegexReplacing;


public class RegexReplacingTest {

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

    private static void assertReplacement(final RegexReplacing regexReplacing, final String input,
                                          final String expected) {
        final Optional<RegexReplacing.ReplacementResult> resultOptional = regexReplacing.replace(input);
        assertTrue(resultOptional.isPresent());
        final RegexReplacing.ReplacementResult replacementResult = resultOptional.get();
        assertEquals(expected, replacementResult.replacement());
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