package querqy.nfa;

import org.junit.Test;
import querqy.regex.MatchResult;
import querqy.regex.RegexLookup;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RegexLookupTest {

    @Test
    public void testOne() {
        RegexLookup lookup = new RegexLookup();
        //lookup.put("abc", "ABC");
        //lookup.put("a(c{1,8}){3}d", "QQ");
        //lookup.put("a((c){1,2})", "QQ");
        lookup.put("a\\d{2,3}c", "QQ");
        System.out.println(lookup.getAll("a12c"));
       // System.out.println(lookup.get("acccd"));
    }

    @Test
    public void testLiteralsWithExactQuantifier() {
        RegexLookup lookup = new RegexLookup();
        lookup.put("ab{1}c", "1");
        lookup.put("bab{2}c", "2");
        lookup.put("cabc{3}", "3");
        lookup.put("d{4}abc", "4");

        assertEquals(Set.of(matchResult("1", "abc")), lookup.getAll("abc"));
        assertEquals(Set.of(matchResult("2", "babbc")), lookup.getAll("babbc"));
        assertEquals(Set.of(matchResult("3", "cabccc")), lookup.getAll("cabccc"));
        assertEquals(Set.of(matchResult("4", "ddddabc")), lookup.getAll("ddddabc"));

        assertTrue(lookup.getAll("abbc").isEmpty());
        assertTrue(lookup.getAll("ac").isEmpty());
        assertTrue(lookup.getAll("babc").isEmpty());
        assertTrue(lookup.getAll("cabcc").isEmpty());
        assertTrue(lookup.getAll("dddddabc").isEmpty());

   }

    @Test
    public void testLiteralsWithMinQuantifier() {
        RegexLookup lookup = new RegexLookup();
        lookup.put("ab{1,}c", "1");
        lookup.put("bab{2,}c", "2");
        lookup.put("cabc{3,}", "3");
        lookup.put("d{4,}abc", "4");

        assertEquals(Set.of(matchResult("1", "abc")), lookup.getAll("abc"));
        assertEquals(Set.of(matchResult("1", "abbc")), lookup.getAll("abbc"));
        assertEquals(Set.of(matchResult("1", "abbbc")), lookup.getAll("abbbc"));
        assertEquals(Set.of(matchResult("2", "babbc")), lookup.getAll("babbc"));
        assertEquals(Set.of(matchResult("2", "babbbc")), lookup.getAll("babbbc"));
        assertEquals(Set.of(matchResult("3", "cabccccc")), lookup.getAll("cabccccc"));
        assertEquals(Set.of(matchResult("4", "dddddabc")), lookup.getAll("dddddabc"));

        assertTrue(lookup.getAll("ac").isEmpty());
        assertTrue(lookup.getAll("babc").isEmpty());
        assertTrue(lookup.getAll("cabcc").isEmpty());
        assertTrue(lookup.getAll("dddabc").isEmpty());

    }

    @Test
    public void testLiteralsWithMinMaxQuantifier() {
        RegexLookup lookup = new RegexLookup();
        lookup.put("ab{1,2}c", "1");
        lookup.put("kl{2,3}m", "2");
        assertEquals(Set.of(matchResult("2", "kllm")), lookup.getAll("kllm"));
        assertEquals(Set.of(matchResult("2", "klllm")), lookup.getAll("klllm"));
    }


    @Test
    public void testAnyCharNoQuantifier() {
        RegexLookup lookup = new RegexLookup();
        lookup.put("a.c", "1");
        lookup.put(".ef", "2");
        lookup.put("ghi.", "3");
        lookup.put(".", "4");
        lookup.put("..", "5");
        lookup.put(".k.", "6");
        lookup.put("(.y).ft", "7");
        assertEquals(Set.of(matchResult("1", "abc")), lookup.getAll("abc"));
        assertEquals(Set.of(matchResult("2", "def")), lookup.getAll("def"));
        assertEquals(Set.of(matchResult("3", "ghij")), lookup.getAll("ghij"));
        assertEquals(Set.of(matchResult("4", "z")), lookup.getAll("z"));
        assertEquals(Set.of(matchResult("5", "km")), lookup.getAll("km"));
        assertEquals(Set.of(matchResult("6", "iko")), lookup.getAll("iko"));
        assertEquals(Set.of(matchResult("7", "xy2ft", "xy")), lookup.getAll("xy2ft"));


        //lookup.put("abc", "ABC");
        //lookup.put("a(c{1,8}){3}d", "QQ");
        //lookup.put("a((c){1,2})", "QQ");
        //lookup.put("a(1){2,3}c", "QQ");
       // System.out.println(lookup.get("ac"));
        // System.out.println(lookup.get("acccd"));
    }

    @Test
    public void testAnyCharWithExactQuantifier() {
        RegexLookup lookup = new RegexLookup();
        lookup.put("a.{2}c", "1");
        lookup.put("ghi.{3}", "2");
        lookup.put(".{4}", "3");
        lookup.put(".{2}.{3}", "4");
        lookup.put(".{3}k.{2}", "5");
        lookup.put("(.{2}y).{3}ft", "6");

        assertEquals(Set.of(
                matchResult("1", "abbc"),
                matchResult("3", "abbc")
        ), lookup.getAll("abbc"));
        assertTrue(lookup.getAll("abc").isEmpty());

        assertEquals(Set.of(matchResult("2", "ghijjj")), lookup.getAll("ghijjj"));
        assertTrue(lookup.getAll("ghijjjj").isEmpty());
        // gets value "4" but not "2"!
        assertEquals(Set.of(matchResult("4", "ghijj")), lookup.getAll("ghijj"));

        assertEquals(Set.of(matchResult("3", "klmn")), lookup.getAll("klmn"));

        assertEquals(Set.of(matchResult("5", "123k78")), lookup.getAll("123k78"));

        assertEquals(Set.of(matchResult("6", "12y789ft", "12y")), lookup.getAll("12y789ft"));

    }


    @Test
    public void testAnyCharWithRangeQuantifier() {
        RegexLookup lookup = new RegexLookup();
        lookup.put("a.{2,3}c", "1");
        lookup.put(".{0,2}k", "2");

        assertEquals(Set.of(matchResult("1", "azyc")), lookup.getAll("azyc"));
        assertTrue(lookup.getAll("azc").isEmpty());
        assertEquals(Set.of(matchResult("1", "azyxc")), lookup.getAll("azyxc"));
        assertEquals(Set.of(matchResult("2", "k")), lookup.getAll("k"));
        assertEquals(Set.of(matchResult("2", "lk")), lookup.getAll("lk"));
        assertEquals(Set.of(matchResult("2", "mlk")), lookup.getAll("mlk"));
        assertTrue(lookup.getAll("nmlk").isEmpty());
    }

    @Test
    public void testGroupQuantifiers() {
        RegexLookup lookup = new RegexLookup();
        lookup.put("a(bc)d", "1");
        lookup.put("e(fg){2}d", "2");
        lookup.put("h(bc){2,3}e", "3");
    }

    @Test
    public void testCharacterClass() {
        RegexLookup lookup = new RegexLookup();
        lookup.put("a[a-zA-Z]c", "1");
        lookup.put("k[0-9&&[^45]]l", "2");

        assertEquals(Set.of(matchResult("1", "abc")), lookup.getAll("abc"));
        assertTrue(lookup.getAll("a1c").isEmpty());
        assertEquals(Set.of(matchResult("1", "aBc")), lookup.getAll("aBc"));
        assertTrue(lookup.getAll("nmlk").isEmpty());

        assertEquals(Set.of(matchResult("2", "k1l")), lookup.getAll("k1l"));
        assertTrue(lookup.getAll("k4l").isEmpty());

    }

    @Test
    public void testAlternation() {
        RegexLookup lookup = new RegexLookup();
        lookup.put("a(b|c)d", "1");

        System.out.println(lookup.getAll("abd"));

    }




    MatchResult matchResult(final Object value, final String... groupMatches) {

        return new MatchResult(value, IntStream.range(0, groupMatches.length)
                .boxed()
                .collect(Collectors.toMap(
                        i -> i,
                        i -> groupMatches[i]
                )));

    }


}
