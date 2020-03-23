package querqy.rewrite.contrib;

import org.junit.Test;
import querqy.parser.WhiteSpaceQuerqyParser;
import querqy.trie.RuleExtractor;
import querqy.trie.model.ExactMatch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class ReplaceRewriterParserTest {

    @Test(expected = IOException.class)
    public void testCombinedInputWIthPrefixAndSuffix() throws IOException {
        String rules = " ab* \t *bc \t abc => cd";
        RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor = createParser(rules, false).parseConfig();
    }

    @Test
    public void testCombinedInputOnlyExactMatch() throws IOException {
        String rules = " ab \t bc \t abc => cd";
        RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor = createParser(rules, false).parseConfig();
    }

    @Test
    public void testEmptyOutput() throws IOException {
        String rules = " abc => ";
        RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor = createParser(rules, false).parseConfig();
        assertThat(ruleExtractor.findRulesByExactMatch(Arrays.asList("abc", "cab", "dabc"))).hasSize(1);
    }

    @Test
    public void testEmptyOutputSuffix() throws IOException {
        String rules = " *abc => ";
        RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor = createParser(rules, false).parseConfig();
        assertThat(ruleExtractor.findRulesBySingleTermSuffixMatch(Arrays.asList("dabc", "cab"))).hasSize(1);
    }

    @Test
    public void testDuplicateSuffixCaseSensitive() throws IOException {
        String rules = "*ab => af \n" +
                "*AB => ag";
        RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor = createParser(rules, false).parseConfig();
        assertThat(ruleExtractor.findRulesBySingleTermSuffixMatch(Arrays.asList("cAB", "cab", "dabc"))).hasSize(2);
    }

    @Test
    public void testDuplicatePrefixCaseSensitive() throws IOException {
        String rules = "ab* => af \n" +
                "AB* => ag";
        RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor = createParser(rules, false).parseConfig();
        assertThat(ruleExtractor.findRulesBySingleTermPrefixMatch(Arrays.asList("ABc", "abc", "dabc"))).hasSize(2);
    }

    @Test(expected = IOException.class)
    public void testDuplicatePrefix() throws IOException {
        String rules = "ab* => af \n" +
                "ab* => ag";
        createParser(rules).parseConfig();
    }

    @Test(expected = IOException.class)
    public void testDuplicateSuffix() throws IOException {
        String rules = "*ab => af \n" +
                "*ab => ag";
        createParser(rules).parseConfig();
    }

    @Test(expected = IOException.class)
    public void testTwoWildCardsPerTerm() throws IOException {
        String rules = "*ab* => af";
        createParser(rules).parseConfig();
    }

    @Test(expected = IOException.class)
    public void testWildcardOnly() throws IOException {
        String rules = "* => af";
        createParser(rules).parseConfig();
    }

    @Test(expected = IOException.class)
    public void testEmptyInput() throws IOException {
        String rules = " => af";
        createParser(rules).parseConfig();
    }


    private RuleExtractor<CharSequence, Queue<CharSequence>> createRuleExtractor(String rules) throws IOException {
        return createParser(rules).parseConfig();
    }

    private ReplaceRewriterParser createParser(String rules) {
        return createParser(rules, true);
    }

    private ReplaceRewriterParser createParser(String rules, boolean ignoreCase) {
        InputStreamReader input = new InputStreamReader(new ByteArrayInputStream(rules.getBytes()));
        return new ReplaceRewriterParser(
                input, ignoreCase, "\t", new WhiteSpaceQuerqyParser());
    }

    @Test
    public void testSuffixInput() throws IOException {
        String rules = "# comment\n"
                + " *abc => ae \n"
                + " *ab => af \n";

        RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor = createRuleExtractor(rules);
        assertThat(ruleExtractor.findRulesBySingleTermSuffixMatch(Arrays.asList("a", "ab", "dabc"))).hasSize(2);
    }

    @Test(expected = IOException.class)
    public void testImproperSuffixInput() throws IOException {
        String rules = "df *ab => af";
        createParser(rules).parseConfig();
    }

    @Test
    public void testPrefixInput() throws IOException {
        String rules = "# comment\n"
                + " abc* => ae \n"
                + " ab* => af \n";

        InputStreamReader input = new InputStreamReader(new ByteArrayInputStream(rules.getBytes()));
        ReplaceRewriterParser replaceRewriterParser = new ReplaceRewriterParser(
                input, true, "\t", new WhiteSpaceQuerqyParser());

        RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor = replaceRewriterParser.parseConfig();
        assertThat(ruleExtractor.findRulesBySingleTermPrefixMatch(Arrays.asList("a", "ab", "abcd"))).hasSize(2);
    }

    @Test(expected = IOException.class)
    public void testImproperPrefixInput() throws IOException {
        String rules = " ab* df => af \n";

        createParser(rules).parseConfig();
    }

    @Test(expected = IOException.class)
    public void testWrongConfigurationDuplicateInputStringCaseInsensitive() throws IOException {
        String rules = "# comment\n"
                + "c => d \n"
                + " a   B => b \n"
                + "e d \t a b => c";

        createParser(rules).parseConfig();
    }

    @Test
    public void testWrongConfigurationDuplicateInputStringCaseSensitive() throws IOException {
        String rules = "# comment\n"
                + "c => d \n"
                + " a   B => b \n"
                + "e d \t a b => c";

        createParser(rules, false).parseConfig();
    }

    @Test(expected = IOException.class)
    public void testWrongConfiguration() throws IOException {
        String rules = "# comment\n"
                + "something wrong \n"
                + " FG => hi jk  \n ";

        createParser(rules).parseConfig();
    }

    @Test
    public void testMappingCaseInsensitive() throws IOException {
        String rules = "# comment\n"
                + "\n"
                + " ab  \t c d => e \n"
                + " FG => hi jk  \n ";

        RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor = createParser(rules).parseConfig();;

        List<ExactMatch<Queue<CharSequence>>> exactMatches;

        exactMatches = ruleExtractor.findRulesByExactMatch(list("ab"));
        assertThat(exactMatches).containsExactlyInAnyOrder(
                new ExactMatch<>(0, 1, queue("e"))
        );

        exactMatches = ruleExtractor.findRulesByExactMatch(list("c"));
        assertThat(exactMatches).isEmpty();

        exactMatches = ruleExtractor.findRulesByExactMatch(list("c", "d"));
        assertThat(exactMatches).containsExactlyInAnyOrder(
                new ExactMatch<>(0, 2, queue("e"))
        );

        exactMatches = ruleExtractor.findRulesByExactMatch(list("fg"));
        assertThat(exactMatches).containsExactlyInAnyOrder(
                new ExactMatch<>(0, 1, queue("hi", "jk"))
        );
    }

    @Test
    public void testMappingCaseSensitive() throws IOException {
        String rules = "AB => cd";

        RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor = createParser(rules, false).parseConfig();


        List<ExactMatch<Queue<CharSequence>>> exactMatches;

        exactMatches = ruleExtractor.findRulesByExactMatch(list("AB"));
        assertThat(exactMatches).containsExactlyInAnyOrder(
                new ExactMatch<>(0, 1, queue("cd"))
        );

        exactMatches = ruleExtractor.findRulesByExactMatch(list("ab"));
        assertThat(exactMatches).isEmpty();
    }

    private Queue<CharSequence> queue(String... str) {
        return Arrays.stream(str).collect(Collectors.toCollection(LinkedList::new));
    }

    private List<CharSequence> list(String... str) {
        return Arrays.stream(str).collect(Collectors.toCollection(ArrayList::new));
    }
}
