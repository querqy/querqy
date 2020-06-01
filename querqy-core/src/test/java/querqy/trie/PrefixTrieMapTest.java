package querqy.trie;

import org.junit.Test;
import querqy.ComparableCharSequence;
import querqy.ComparableCharSequenceWrapper;
import querqy.trie.model.PrefixMatch;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class PrefixTrieMapTest {

    private ComparableCharSequence matchSuffix = seq("abcde");
    private ComparableCharSequence shorterSuffix = seq("abcd");
    private ComparableCharSequence similarSuffix = seq("abced");
    private ComparableCharSequence longerSuffix = seq("abcdef");
    private ComparableCharSequence longerSimilarSuffix = seq("abcedf");

    private ComparableCharSequence termMatch = seq("abcdexxxx");
    private ComparableCharSequence partialTermMatch = seq("ab");
    private ComparableCharSequence termNoMatch = seq("abcfed");

    @Test
    public void testPartialMatch() {
        PrefixTrieMap<String> prefixMap = createprefixMap(
                matchSuffix, shorterSuffix, similarSuffix, longerSuffix, longerSimilarSuffix);

        Optional<PrefixMatch<String>> prefixMatchOptional;
        prefixMatchOptional = prefixMap.getPrefix(partialTermMatch);
        assertThat(prefixMatchOptional).isEmpty();
    }

    @Test
    public void testNoPrefixMatch() {
        PrefixTrieMap<String> prefixMap = createprefixMap(
                matchSuffix, similarSuffix, longerSuffix, longerSimilarSuffix);

        Optional<PrefixMatch<String>> prefixMatchOptional;
        prefixMatchOptional = prefixMap.getPrefix(termNoMatch);
        assertThat(prefixMatchOptional).isEmpty();
    }

    @Test
    public void testShorterPrefixMatch() {
        PrefixTrieMap<String> prefixMap = createprefixMap(
                shorterSuffix, similarSuffix, longerSimilarSuffix);

        Optional<PrefixMatch<String>> prefixMatchOptional;
        PrefixMatch<String> PrefixMatch;

        prefixMatchOptional = prefixMap.getPrefix(termMatch);
        assertThat(prefixMatchOptional).isNotEmpty();

        PrefixMatch = prefixMatchOptional.get();
        assertThat(PrefixMatch.match).isEqualTo(shorterSuffix.toString().toUpperCase());
        assertThat(PrefixMatch.exclusiveEnd).isEqualTo(4);
        assertThat(PrefixMatch.wildcardMatch).isEqualTo("exxxx");
    }

    @Test
    public void testPrefixMatch() {
        PrefixTrieMap<String> prefixMap = createprefixMap(
                matchSuffix, shorterSuffix, similarSuffix, longerSimilarSuffix);

        Optional<PrefixMatch<String>> prefixMatchOptional;
        PrefixMatch<String> PrefixMatch;

        prefixMatchOptional = prefixMap.getPrefix(termMatch);
        assertThat(prefixMatchOptional).isNotEmpty();

        PrefixMatch = prefixMatchOptional.get();
        assertThat(PrefixMatch.match).isEqualTo(matchSuffix.toString().toUpperCase());
        assertThat(PrefixMatch.exclusiveEnd).isEqualTo(5);
        assertThat(PrefixMatch.wildcardMatch).isEqualTo("xxxx");
    }

    @Test
    public void testPerfectMatch() {
        PrefixTrieMap<String> prefixMap = createprefixMap(
                matchSuffix, shorterSuffix, similarSuffix, longerSuffix, longerSimilarSuffix);

        Optional<PrefixMatch<String>> prefixMatchOptional;
        PrefixMatch<String> PrefixMatch;

        prefixMatchOptional = prefixMap.getPrefix(matchSuffix);
        assertThat(prefixMatchOptional).isNotEmpty();

        PrefixMatch = prefixMatchOptional.get();
        assertThat(PrefixMatch.match).isEqualTo(matchSuffix.toString().toUpperCase());
        assertThat(PrefixMatch.exclusiveEnd).isEqualTo(matchSuffix.length());
        assertThat(PrefixMatch.wildcardMatch).isEqualTo("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPutEmptyString() {
        new PrefixTrieMap<>().putPrefix("", null);
    }

    @Test
    public void testGetWithEmptyString() {
        assertThat(new PrefixTrieMap<>().getPrefix("")).isEmpty();
    }

    private PrefixTrieMap<String> createprefixMap(ComparableCharSequence... terms) {
        PrefixTrieMap<String> prefixMap = new PrefixTrieMap<>();
        Arrays.stream(terms).forEach(term -> prefixMap.putPrefix(term, term.toString().toUpperCase(), true));
        return prefixMap;
    }

    private ComparableCharSequence seq(String str) {
        return new ComparableCharSequenceWrapper(str);
    }
}
