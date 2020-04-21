package querqy.trie;

import org.junit.Test;
import querqy.ComparableCharSequence;
import querqy.ComparableCharSequenceWrapper;
import querqy.trie.model.SuffixMatch;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class SuffixTrieMapTest {

    private ComparableCharSequence matchSuffix = seq("kameras");
    private ComparableCharSequence shorterSuffix = seq("ras");
    private ComparableCharSequence similarSuffix = seq("cameras");
    private ComparableCharSequence longerSuffix = seq("lkameras");
    private ComparableCharSequence longerSimilarSuffix = seq("lcameras");

    private ComparableCharSequence termMatch = seq("digitalkameras");
    private ComparableCharSequence partialTermMatch = seq("as");
    private ComparableCharSequence termNoMatch = seq("digitalameras");

    @Test
    public void testPartialMatch() {
        SuffixTrieMap<String> suffixMap = createSuffixMap(
                matchSuffix, shorterSuffix, similarSuffix, longerSuffix, longerSimilarSuffix);

        Optional<SuffixMatch<String>> suffixMatchOptional;
        suffixMatchOptional = suffixMap.getBySuffix(partialTermMatch);
        assertThat(suffixMatchOptional).isEmpty();
    }


    @Test
    public void testNoSuffixMatch() {
        SuffixTrieMap<String> suffixMap = createSuffixMap(
                matchSuffix, similarSuffix, longerSuffix, longerSimilarSuffix);

        Optional<SuffixMatch<String>> suffixMatchOptional;
        suffixMatchOptional = suffixMap.getBySuffix(termNoMatch);
        assertThat(suffixMatchOptional).isEmpty();
    }

    @Test
    public void testShorterSuffixMatch() {
        SuffixTrieMap<String> suffixMap = createSuffixMap(
                shorterSuffix, similarSuffix, longerSimilarSuffix);

        Optional<SuffixMatch<String>> suffixMatchOptional;
        SuffixMatch<String> suffixMatch;

        suffixMatchOptional = suffixMap.getBySuffix(termMatch);
        assertThat(suffixMatchOptional).isNotEmpty();

        suffixMatch = suffixMatchOptional.get();
        assertThat(suffixMatch.match).isEqualTo(shorterSuffix.toString().toUpperCase());
        assertThat(suffixMatch.startSubstring).isEqualTo(11);
    }

    @Test
    public void testSuffixMatch() {
        SuffixTrieMap<String> suffixMap = createSuffixMap(
                matchSuffix, shorterSuffix, similarSuffix, longerSimilarSuffix);

        Optional<SuffixMatch<String>> suffixMatchOptional;
        SuffixMatch<String> suffixMatch;

        suffixMatchOptional = suffixMap.getBySuffix(termMatch);
        assertThat(suffixMatchOptional).isNotEmpty();

        suffixMatch = suffixMatchOptional.get();
        assertThat(suffixMatch.match).isEqualTo(matchSuffix.toString().toUpperCase());
        assertThat(suffixMatch.startSubstring).isEqualTo(7);
    }

    @Test
    public void testPerfectMatch() {
        SuffixTrieMap<String> suffixMap = createSuffixMap(
                matchSuffix, shorterSuffix, similarSuffix, longerSuffix, longerSimilarSuffix);

        Optional<SuffixMatch<String>> suffixMatchOptional;
        SuffixMatch<String> suffixMatch;

        suffixMatchOptional = suffixMap.getBySuffix(matchSuffix);
        assertThat(suffixMatchOptional).isNotEmpty();

        suffixMatch = suffixMatchOptional.get();
        assertThat(suffixMatch.match).isEqualTo(matchSuffix.toString().toUpperCase());
        assertThat(suffixMatch.startSubstring).isEqualTo(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPutEmptyString() {
        new SuffixTrieMap<>().putSuffix("", null);
    }

    @Test
    public void testGetWithEmptyString() {
        assertThat(new SuffixTrieMap<>().getBySuffix("")).isEmpty();
    }

    private SuffixTrieMap<String> createSuffixMap(ComparableCharSequence... terms) {
        SuffixTrieMap<String> suffixMap = new SuffixTrieMap<>();
        Arrays.stream(terms).forEach(term -> suffixMap.putSuffix(term, term.toString().toUpperCase(), true));
        return suffixMap;
    }

    private ComparableCharSequence seq(String str) {
        return new ComparableCharSequenceWrapper(str);
    }
}
