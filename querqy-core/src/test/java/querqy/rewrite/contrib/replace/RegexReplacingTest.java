package querqy.rewrite.contrib.replace;

import org.junit.Test;

public class RegexReplacingTest {

    @Test
    public void testReplacementWithoutPlaceholder() {
        RegexReplacing regexReplacing = new RegexReplacing();
        regexReplacing.put("abc", "ABC");
        regexReplacing.put("def\\d+", "DEF");
        regexReplacing.put("gh (ikk)+ lmn", "XYZ");
        regexReplacing.put("(\\d+) x (\\d+) ((\\d{1,2})m)", "${1}x${2} ${4}000ft");

        System.out.println(regexReplacing.replace("abc abc abcd abc abck"));
        System.out.println(regexReplacing.replace("abc hello"));
        System.out.println(regexReplacing.replace("hello abc abc bye"));
        System.out.println(regexReplacing.replace("def12 abc"));
        System.out.println(regexReplacing.replace("hello gh ikkikk lmn bye"));
        System.out.println(regexReplacing.replace("763 x 23 2m"));



    }

}