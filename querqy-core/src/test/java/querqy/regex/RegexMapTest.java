package querqy.regex;

import org.junit.Test;
import querqy.regex.MatchResult.GroupMatch;

import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RegexMapTest {

    @Test
    public void testLiteralsWithExactQuantifier() {
        RegexMap<String> lookup = new RegexMap<>();
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
        RegexMap<String> lookup = new RegexMap<>();
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
        RegexMap<String> lookup = new RegexMap<>();
        lookup.put("ab{1,2}c", "1");
        lookup.put("kl{2,3}m", "2");
        assertEquals(Set.of(matchResult("2", "kllm")), lookup.getAll("kllm"));
        assertEquals(Set.of(matchResult("2", "klllm")), lookup.getAll("klllm"));
    }

//    @Test
//    public void testPrefixes2() throws IOException {
//        RegexMap<String> lookup = new RegexMap<>();
//        lookup.put("a.{2}c", "1");
//       // lookup.put("ghi.{3}", "2");
//        NFADebugPrinter.printDot(lookup.prefixlessStart,
//                new PrintStream(new FileOutputStream("/Users/rene/Developer/projects/querqy/querqy/graph")));
//
//    }
//    @Test
//    public void testPrefixes() {
//        RegexMap<String> lookup = new RegexMap<>();
//        lookup.put("xc", "8", "([^ ]+)?");
//        lookup.put("lm", "9", "([^ ]+)?");
//        NFADebugPrinter.printDot(lookup.prefixlessStart, System.out);
//    }


    @Test
    public void testAnyCharNoQuantifier() {
        RegexMap<String> lookup = new RegexMap<>();
        lookup.put("a.c", "1");
        lookup.put(".ef", "2");
        lookup.put("ghi.", "3");
        lookup.put(".", "4");
        lookup.put("..", "5");
        lookup.put(".k.", "6");
        lookup.put("(.y).ft", "7");
        lookup.put("([^ ]+)?xc", "8");
        //NFADebugPrinter.print(lookup.prefixlessStart);
       // NFADebugPrinter.printDot(lookup.prefixlessStart, System.out);
        assertEquals(Set.of(matchResult("1", "abc")), lookup.getAll("abc"));
        assertEquals(Set.of(matchResult("2", "def")), lookup.getAll("def"));
        assertEquals(Set.of(matchResult("3", "ghij")), lookup.getAll("ghij"));
        assertEquals(Set.of(matchResult("4", "z")), lookup.getAll("z"));
        assertEquals(Set.of(matchResult("5", "km")), lookup.getAll("km"));
        assertEquals(Set.of(matchResult("6", "iko")), lookup.getAll("iko"));
        assertEquals(Set.of(matchResult("7", group("xy2ft", 0),
                group("xy", 0))), lookup.getAll("xy2ft"));

    }

    @Test
    public void testAnyCharWithExactQuantifier() throws FileNotFoundException {
        RegexMap<String> lookup = new RegexMap<>();
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

        assertEquals(Set.of(matchResult("6", group("12y789ft",0),
                group("12y",0))), lookup.getAll("12y789ft"));

    }


    @Test
    public void testAnyCharWithRangeQuantifier() {
        RegexMap<String> lookup = new RegexMap<>();
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
        RegexMap<String> regexMap = new RegexMap<>();
        regexMap.put("e(fg){2}d", "2");
        regexMap.put("h(bc){2,3}e", "3");
        regexMap.put("([^ ]+ ){0,}(abc)( [^ ]+){0,}", "4");
        assertEquals(Set.of(matchResult("2", group("efgfgd", 0), group("fg", 3))),
                regexMap.getAll("efgfgd"));
        assertEquals(Set.of(matchResult("3", group("hbcbce", 0), group("bc", 3))),
                regexMap.getAll("hbcbce"));
        assertEquals(Set.of(matchResult("3", group("hbcbcbce", 0), group("bc", 5))),
                regexMap.getAll("hbcbcbce"));
        assertEquals(Set.of(matchResult("4", group("abc", 0), null,
                group("abc", 0))), regexMap.getAll("abc"));
        assertEquals(Set.of(matchResult("4", group("abc hello", 0), null,
                group("abc", 0), group(" hello", 3))), regexMap.getAll("abc hello"));
    }

    @Test
    public void testCharacterClass() {
        RegexMap<String> lookup = new RegexMap<>();
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
        RegexMap<String> lookup = new RegexMap<>();
        lookup.put("a(b|c)d", "1");
        assertEquals(Set.of(matchResult("1", group("abd", 0), group("b", 1))),
                lookup.getAll("abd"));
        assertTrue(lookup.getAll("abc").isEmpty());

    }

    @Test
    public void testReplaceExactlyOnceQuantifier() {
        assertEquals("abc", RegexMap.replaceExactlyOnceQuantifier("{1}abc"));
        assertEquals("abc", RegexMap.replaceExactlyOnceQuantifier("a{1}b{1}c{1}"));
        assertEquals("\\{1}a\\{1}}bc", RegexMap.replaceExactlyOnceQuantifier("\\{1}a\\{1}}bc"));
        assertEquals("{1}", RegexMap.replaceExactlyOnceQuantifier("{1}"));
    }


    <T> MatchResult<T> matchResult(final T value, final String match) {
        return new MatchResult(value, Map.of(0, new GroupMatch(match, 0)));
    }

    <T> MatchResult<T> matchResult(final T value, final GroupMatch... groups) {

        return new MatchResult(value, IntStream.range(0, groups.length)
                .filter(i -> groups[i] != null)
                .boxed()
                .collect(Collectors.toMap(
                        i -> i,
                        i -> groups[i]
                )));

    }

    GroupMatch group(final String match, final int position) {
        return new GroupMatch(match, position);
    }

}
