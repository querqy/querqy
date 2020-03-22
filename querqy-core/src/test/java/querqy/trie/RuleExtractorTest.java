package querqy.trie;

import org.junit.Test;
import querqy.trie.model.ExactMatch;
import querqy.trie.model.PrefixMatch;
import querqy.trie.model.SuffixMatch;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class RuleExtractorTest {

    @Test
    public void testNoMatch() {
        RuleExtractor<String, String> ruleExtractor = new RuleExtractor<>(false);
        ruleExtractor.putSuffix("suffix", "suffix");
        ruleExtractor.putPrefix("prefix", "prefix");
        ruleExtractor.put(createStringList("exact"), "exact");

        List<CharSequence> input = createTermSeq("term");

        List<SuffixMatch<String>> suffixMatches = ruleExtractor.findRulesBySingleTermSuffixMatch(input);
        assertThat(suffixMatches).hasSize(0);

        List<PrefixMatch<String>> prefixMatches = ruleExtractor.findRulesBySingleTermPrefixMatch(input);
        assertThat(prefixMatches).hasSize(0);

        List<ExactMatch<String>> exactMatches = ruleExtractor.findRulesByExactMatch(input);
        assertThat(exactMatches).hasSize(0);
    }

    @Test
    public void testSuffixIgnoreCase() {
        RuleExtractor<String, String> ruleExtractor = new RuleExtractor<>(false);
        ruleExtractor.putSuffix("1SUFFIX", "upper");
        ruleExtractor.putSuffix("2suffix", "lower");

        List<SuffixMatch<String>> suffixMatches;
        suffixMatches = ruleExtractor.findRulesBySingleTermSuffixMatch(createTermSeq(
                "prefix1suffix", "PREFIX2SUFFIX", "PREFIX1SUFFIX", "prefix2suffix"));
        assertThat(suffixMatches).hasSize(2);
        assertThat(suffixMatches).containsExactlyInAnyOrder(
                new SuffixMatch<>(6, "upper").setLookupOffset(2),
                new SuffixMatch<>(6, "lower").setLookupOffset(3)
        );
    }

    @Test
    public void testSuffixDuplicate() {
        RuleExtractor<String, String> ruleExtractor = new RuleExtractor<>();
        ruleExtractor.putSuffix("1suffix", "value1");

        List<SuffixMatch<String>> suffixMatches;
        suffixMatches = ruleExtractor.findRulesBySingleTermSuffixMatch(createTermSeq("prefix1suffix", "prefix1suffix"));
        assertThat(suffixMatches).hasSize(2);
        assertThat(suffixMatches).containsExactlyInAnyOrder(
                new SuffixMatch<>(6, "value1").setLookupOffset(0),
                new SuffixMatch<>(6, "value1").setLookupOffset(1)
        );
    }

    @Test
    public void testSuffixSubset() {
        RuleExtractor<String, String> ruleExtractor = new RuleExtractor<>();
        ruleExtractor.putSuffix("11suffix", "value1");
        ruleExtractor.putSuffix("fix", "value2");
        ruleExtractor.putSuffix("suffix", "value3");

        List<SuffixMatch<String>> suffixMatches;
        suffixMatches = ruleExtractor.findRulesBySingleTermSuffixMatch(createTermSeq("prefix1suffix", "term", "prefix11suffix"));
        assertThat(suffixMatches).hasSize(2);
        assertThat(suffixMatches).containsExactlyInAnyOrder(
                new SuffixMatch<>(7, "value3").setLookupOffset(0),
                new SuffixMatch<>(6, "value1").setLookupOffset(2)
        );
    }

    @Test
    public void testSuffixMultipleMatches() {
        RuleExtractor<String, String> ruleExtractor = new RuleExtractor<>();
        ruleExtractor.putSuffix("1suffix", "value1");
        ruleExtractor.putSuffix("11suffix", "value2");

        List<SuffixMatch<String>> suffixMatches;
        suffixMatches = ruleExtractor.findRulesBySingleTermSuffixMatch(createTermSeq("prefix1suffix", "term", "prefix11suffix"));
        assertThat(suffixMatches).hasSize(2);
        assertThat(suffixMatches).containsExactlyInAnyOrder(
                new SuffixMatch<>(6, "value1").setLookupOffset(0),
                new SuffixMatch<>(6, "value2").setLookupOffset(2)
        );
    }


    @Test
    public void testPrefixIgnoreCase() {
        RuleExtractor<String, String> ruleExtractor = new RuleExtractor<>(false);
        ruleExtractor.putPrefix("PREFIX1", "upper");
        ruleExtractor.putPrefix("prefix2", "lower");

        List<PrefixMatch<String>> prefixMatches;
        prefixMatches = ruleExtractor.findRulesBySingleTermPrefixMatch(createTermSeq(
                "prefix1suffix", "PREFIX2suffix", "PREFIX1suffix", "prefix2suffix"));
        assertThat(prefixMatches).hasSize(2);
        assertThat(prefixMatches).containsExactlyInAnyOrder(
                new PrefixMatch<>(7, "upper").setLookupOffset(2),
                new PrefixMatch<>(7, "lower").setLookupOffset(3)
        );
    }

    @Test
    public void testPrefixDuplicate() {
        RuleExtractor<String, String> ruleExtractor = new RuleExtractor<>();
        ruleExtractor.putPrefix("prefix1", "value1");

        List<PrefixMatch<String>> prefixMatches;
        prefixMatches = ruleExtractor.findRulesBySingleTermPrefixMatch(createTermSeq("prefix1suffix", "prefix1suffix"));
        assertThat(prefixMatches).hasSize(2);
        assertThat(prefixMatches).containsExactlyInAnyOrder(
                new PrefixMatch<>(7, "value1").setLookupOffset(0),
                new PrefixMatch<>(7, "value1").setLookupOffset(1)
        );
    }

    @Test
    public void testPrefixSubset() {
        RuleExtractor<String, String> ruleExtractor = new RuleExtractor<>();
        ruleExtractor.putPrefix("prefix11", "value1");
        ruleExtractor.putPrefix("pref", "value2");
        ruleExtractor.putPrefix("prefix", "value3");

        List<PrefixMatch<String>> prefixMatches;
        prefixMatches = ruleExtractor.findRulesBySingleTermPrefixMatch(createTermSeq("prefix1suffix", "term", "prefix11suffix"));
        assertThat(prefixMatches).hasSize(2);
        assertThat(prefixMatches).containsExactlyInAnyOrder(
                new PrefixMatch<>(6, "value3").setLookupOffset(0),
                new PrefixMatch<>(8, "value1").setLookupOffset(2)
        );
    }

    @Test
    public void testPrefixMultipleMatches() {
        RuleExtractor<String, String> ruleExtractor = new RuleExtractor<>();
        ruleExtractor.putPrefix("prefix1", "value1");
        ruleExtractor.putPrefix("prefix11", "value2");

        List<PrefixMatch<String>> prefixMatches;
        prefixMatches = ruleExtractor.findRulesBySingleTermPrefixMatch(createTermSeq("prefix1suffix", "term", "prefix11suffix"));
        assertThat(prefixMatches).hasSize(2);
        assertThat(prefixMatches).containsExactlyInAnyOrder(
                new PrefixMatch<>(7, "value1").setLookupOffset(0),
                new PrefixMatch<>(8, "value2").setLookupOffset(2)
        );
    }

    @Test
    public void testMatchingOfMultiTermOverlaps() {
        RuleExtractor<String, String> ruleExtractor = new RuleExtractor<>();
        ruleExtractor.put(createStringList("term1", "term2", "term3", "term4"), "value1");
        ruleExtractor.put(createStringList("term2", "term3", "term4"), "value2");
        ruleExtractor.put(createStringList("term2", "term3"), "value3");
        ruleExtractor.put(createStringList("term2", "term3", "term4", "term5", "term6"), "value4");
        ruleExtractor.put(createStringList("term4", "term5", "term6"), "value5");
        ruleExtractor.put(createStringList("term7", "term8"), "value6");
        ruleExtractor.put(createStringList("term7", "term8", "term9"), "value7");

        List<ExactMatch<String>> exactMatches;
        exactMatches = ruleExtractor.findRulesByExactMatch(createTermSeq("term2", "term3", "term4", "term5", "term6", "term7"));
        assertThat(exactMatches).hasSize(4);
        assertThat(exactMatches).containsExactlyInAnyOrder(
                new ExactMatch<>(0, 3, "value2"),
                new ExactMatch<>(0, 2, "value3"),
                new ExactMatch<>(0, 5, "value4"),
                new ExactMatch<>(2, 5, "value5")
        );

        exactMatches = ruleExtractor.findRulesByExactMatch(createTermSeq("term8", "term9"));
        assertThat(exactMatches).hasSize(0);

        exactMatches = ruleExtractor.findRulesByExactMatch(createTermSeq("term7", "term8", "term9"));
        assertThat(exactMatches).hasSize(2);
        assertThat(exactMatches).containsExactlyInAnyOrder(
                new ExactMatch<>(0, 2, "value6"),
                new ExactMatch<>(0, 3, "value7")
        );
    }

    @Test
    public void testEmptyInput() {
        RuleExtractor<String, String> ruleExtractor = new RuleExtractor<>();
        ruleExtractor.put(createStringList("term1", "term2"), "value1");

        List<ExactMatch<String>> exactMatches;
        exactMatches = ruleExtractor.findRulesByExactMatch(createTermSeq());
        assertThat(exactMatches).isEmpty();

        exactMatches = ruleExtractor.findRulesByExactMatch(createTermSeq(""));
        assertThat(exactMatches).isEmpty();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPutEmptyInputString() {
        RuleExtractor<String, String> ruleExtractor = new RuleExtractor<>();
        ruleExtractor.put(createStringList(""), "value1");
    }

    @Test
    public void testMatchingOfSubsets() {
        RuleExtractor<String, String> ruleExtractor = new RuleExtractor<>();
        ruleExtractor.put(createStringList("term1", "term2"), "value1");
        ruleExtractor.put(createStringList("term1", "term2", "term3"), "value2");
        ruleExtractor.put(createStringList("term2", "term3"), "value3");

        List<ExactMatch<String>> exactMatches;
        exactMatches = ruleExtractor.findRulesByExactMatch(createTermSeq("term1", "term2", "term3"));
        assertThat(exactMatches).hasSize(3);
        assertThat(exactMatches).containsExactlyInAnyOrder(
                new ExactMatch<>(0, 2, "value1"),
                new ExactMatch<>(0, 3, "value2"),
                new ExactMatch<>(1, 3, "value3")
        );
    }

    @Test
    public void testBasicMatchingOnSingleTerms() {
        RuleExtractor<String, String> ruleExtractor = new RuleExtractor<>();
        ruleExtractor.put(createStringList("term1"), "value1");
        ruleExtractor.put(createStringList("term2"), "value2");
        ruleExtractor.put(createStringList("term3"), "value3");

        List<ExactMatch<String>> exactMatches;
        exactMatches = ruleExtractor.findRulesByExactMatch(createTermSeq("term1", "term2"));
        assertThat(exactMatches).hasSize(2);
        assertThat(exactMatches).containsExactlyInAnyOrder(
                new ExactMatch<>(0, 1, "value1"),
                new ExactMatch<>(1, 2, "value2")
        );

        exactMatches = ruleExtractor.findRulesByExactMatch(createTermSeq("term3", "-term"));
        assertThat(exactMatches).hasSize(1);
        assertThat(exactMatches).containsExactlyInAnyOrder(
                new ExactMatch<>(0, 1, "value3")
        );

        exactMatches = ruleExtractor.findRulesByExactMatch(createTermSeq("-term1", "-term2", "-term3"));
        assertThat(exactMatches).hasSize(0);
    }

    @Test
    public void testBasicMatchingOnMultipleTerms() {
        RuleExtractor<String, String> ruleExtractor = new RuleExtractor<>();
        ruleExtractor.put(createStringList("term1", "term2"), "value1");
        ruleExtractor.put(createStringList("term2", "term1"), "value2");
        ruleExtractor.put(createStringList("term1", "term2", "term3"), "value3");

        List<ExactMatch<String>> exactMatches;
        exactMatches = ruleExtractor.findRulesByExactMatch(createTermSeq("term1", "term2"));
        assertThat(exactMatches).hasSize(1);
        assertThat(exactMatches.get(0).lookupStart).isEqualTo(0);
        assertThat(exactMatches.get(0).lookupExclusiveEnd).isEqualTo(2);
        assertThat(exactMatches.get(0).value).isEqualTo("value1");

        exactMatches = ruleExtractor.findRulesByExactMatch(createTermSeq("term2", "term1"));
        assertThat(exactMatches).hasSize(1);
        assertThat(exactMatches.get(0).lookupStart).isEqualTo(0);
        assertThat(exactMatches.get(0).lookupExclusiveEnd).isEqualTo(2);
        assertThat(exactMatches.get(0).value).isEqualTo("value2");

        exactMatches = ruleExtractor.findRulesByExactMatch(createTermSeq("term1", "term2", "-term"));
        assertThat(exactMatches).hasSize(1);
        assertThat(exactMatches.get(0).lookupStart).isEqualTo(0);
        assertThat(exactMatches.get(0).lookupExclusiveEnd).isEqualTo(2);
        assertThat(exactMatches.get(0).value).isEqualTo("value1");

        exactMatches = ruleExtractor.findRulesByExactMatch(createTermSeq("term1"));
        assertThat(exactMatches).hasSize(0);

        exactMatches = ruleExtractor.findRulesByExactMatch(createTermSeq("term2"));
        assertThat(exactMatches).hasSize(0);

        exactMatches = ruleExtractor.findRulesByExactMatch(createTermSeq("term2", "term2"));
        assertThat(exactMatches).hasSize(0);

        exactMatches = ruleExtractor.findRulesByExactMatch(createTermSeq("term2", "term3"));
        assertThat(exactMatches).hasSize(0);

        exactMatches = ruleExtractor.findRulesByExactMatch(createTermSeq("term1", "term1"));
        assertThat(exactMatches).hasSize(0);
    }

    private List<CharSequence> createStringList(String... terms) {
        return Arrays.asList(terms);
    }

    private List<CharSequence> createTermSeq(String... terms) {
        return Arrays.stream(terms).collect(Collectors.toList());
    }
}
