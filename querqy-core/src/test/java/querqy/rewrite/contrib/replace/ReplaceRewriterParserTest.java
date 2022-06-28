package querqy.rewrite.contrib.replace;

import org.junit.Test;
import querqy.parser.WhiteSpaceQuerqyParser;
import querqy.trie.SequenceLookup;
import querqy.trie.model.ExactMatch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class ReplaceRewriterParserTest {

    @Test(expected = IOException.class)
    public void testCombinedInputWIthPrefixAndSuffix() throws IOException {
        String rules = " ab* \t *bc \t abc => cd";
        SequenceLookup<ReplaceInstruction> sequenceLookup = createParser(rules, false).parseConfig();
    }

    @Test
    public void testCombinedInputOnlyExactMatch() throws IOException {
        String rules = " ab \t bc \t abc => cd";
        SequenceLookup<ReplaceInstruction> sequenceLookup = createParser(rules, false).parseConfig();
    }

    @Test
    public void testEmptyOutput() throws IOException {
        String rules = " abc => ";
        SequenceLookup<ReplaceInstruction> sequenceLookup = createParser(rules, false).parseConfig();
        assertThat(sequenceLookup.findExactMatches(Arrays.asList("abc", "cab", "dabc"))).hasSize(1);
    }

    @Test
    public void testEmptyOutputSuffix() throws IOException {
        String rules = " *abc => ";
        SequenceLookup<ReplaceInstruction> sequenceLookup = createParser(rules, false).parseConfig();
        assertThat(sequenceLookup.findSingleTermSuffixMatches(Arrays.asList("dabc", "cab"))).hasSize(1);
    }

    @Test
    public void testDuplicateSuffixCaseSensitive() throws IOException {
        String rules = "*ab => af \n" +
                "*AB => ag";
        SequenceLookup<ReplaceInstruction> sequenceLookup = createParser(rules, false).parseConfig();
        assertThat(sequenceLookup.findSingleTermSuffixMatches(Arrays.asList("cAB", "cab", "dabc"))).hasSize(2);
    }

    @Test
    public void testDuplicatePrefixCaseSensitive() throws IOException {
        String rules = "ab* => af \n" +
                "AB* => ag";
        SequenceLookup<ReplaceInstruction> sequenceLookup = createParser(rules, false).parseConfig();
        assertThat(sequenceLookup.findSingleTermPrefixMatches(Arrays.asList("ABc", "abc", "dabc"))).hasSize(2);
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


    private SequenceLookup<ReplaceInstruction> createRuleExtractor(String rules) throws IOException {
        return createParser(rules).parseConfig();
    }

    private ReplaceRewriterParser createParser(String rules) {
        return createParser(rules, true);
    }

    private ReplaceRewriterParser createParser(String rules, boolean ignoreCase) {
        InputStreamReader input = new InputStreamReader(new ByteArrayInputStream(rules.getBytes(UTF_8)), UTF_8);
        return new ReplaceRewriterParser(
                input, ignoreCase, "\t", new WhiteSpaceQuerqyParser());
    }

    @Test
    public void testSuffixInput() throws IOException {
        String rules = "# comment\n"
                + " *abc => ae \n"
                + " *ab => af \n";

        SequenceLookup<ReplaceInstruction> sequenceLookup = createRuleExtractor(rules);
        assertThat(sequenceLookup.findSingleTermSuffixMatches(Arrays.asList("a", "ab", "dabc"))).hasSize(2);
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

        InputStreamReader input = new InputStreamReader(new ByteArrayInputStream(rules.getBytes(UTF_8)), UTF_8);
        ReplaceRewriterParser replaceRewriterParser = new ReplaceRewriterParser(
                input, true, "\t", new WhiteSpaceQuerqyParser());

        SequenceLookup<ReplaceInstruction> sequenceLookup = replaceRewriterParser.parseConfig();
        assertThat(sequenceLookup.findSingleTermPrefixMatches(Arrays.asList("a", "ab", "abcd"))).hasSize(2);
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

        SequenceLookup<ReplaceInstruction> sequenceLookup = createParser(rules).parseConfig();

        List<ExactMatch<ReplaceInstruction>> exactMatches;

        exactMatches = sequenceLookup.findExactMatches(list("ab"));
        assertThat(exactMatches).isNotEmpty();

        exactMatches = sequenceLookup.findExactMatches(list("c"));
        assertThat(exactMatches).isEmpty();

        exactMatches = sequenceLookup.findExactMatches(list("c", "d"));
        assertThat(exactMatches).isNotEmpty();

        exactMatches = sequenceLookup.findExactMatches(list("fg"));
        assertThat(exactMatches).isNotEmpty();
    }

    @Test
    public void testMappingCaseSensitive() throws IOException {
        String rules = "AB => cd";

        SequenceLookup<ReplaceInstruction> sequenceLookup = createParser(rules, false).parseConfig();

        List<ExactMatch<ReplaceInstruction>> exactMatches;

        exactMatches = sequenceLookup.findExactMatches(list("AB"));
        assertThat(exactMatches).hasSize(1);

        exactMatches = sequenceLookup.findExactMatches(list("ab"));
        assertThat(exactMatches).isEmpty();
    }

    private ReplaceInstruction instruction(String... str) {
        return new TermsReplaceInstruction(Arrays.asList(str));
    }

    private List<CharSequence> list(String... str) {
        return Arrays.stream(str).collect(Collectors.toCollection(ArrayList::new));
    }
}
