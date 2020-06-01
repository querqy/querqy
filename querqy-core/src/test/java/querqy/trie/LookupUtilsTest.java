package querqy.trie;

import org.junit.Test;
import querqy.trie.model.ExactMatch;
import querqy.trie.model.PrefixMatch;
import querqy.trie.model.SuffixMatch;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class LookupUtilsTest {

    @Test
    public void testSortingOfSuffixMatch() {
        List<SuffixMatch<String>> suffixMatches = Arrays.asList(
                suffixMatch(0),
                suffixMatch(4),
                suffixMatch(3),
                suffixMatch(1)
        );

        suffixMatches.sort(LookupUtils.COMPARE_SUFFIX_MATCH_BY_LOOKUP_OFFSET_DESC);

        assertThat(suffixMatches).containsExactly(
                suffixMatch(4),
                suffixMatch(3),
                suffixMatch(1),
                suffixMatch(0)
        );
    }

    @Test
    public void testSortingOfPrefixMatch() {
        List<PrefixMatch<String>> prefixMatches = Arrays.asList(
                prefixMatch(0),
                prefixMatch(4),
                prefixMatch(3),
                prefixMatch(1)
        );

        prefixMatches.sort(LookupUtils.COMPARE_PREFIX_MATCH_BY_LOOKUP_OFFSET_DESC);

        assertThat(prefixMatches).containsExactly(
                prefixMatch(4),
                prefixMatch(3),
                prefixMatch(1),
                prefixMatch(0)
        );
    }

    @Test
    public void testSortingOfExactMatchByLookupOffset() {
        List<ExactMatch<String>> exactMatches = Arrays.asList(
                exactMatch(1, 2),
                exactMatch(0, 1),
                exactMatch(4, 5),
                exactMatch(2, 3)
        );

        exactMatches.sort(LookupUtils.COMPARE_EXACT_MATCH_BY_LOOKUP_OFFSET_DESC);

        assertThat(exactMatches).containsExactly(
                exactMatch(4, 5),
                exactMatch(2, 3),
                exactMatch(1, 2),
                exactMatch(0, 1)
        );
    }

    @Test
    public void testManyOverlappingBoundaries() {
        List<ExactMatch<String>> exactMatches = LookupUtils.removeSubsetsAndSmallerOverlaps(Arrays.asList(
                exactMatch(1, 3),
                exactMatch(1, 2),
                exactMatch(1, 4),
                exactMatch(0, 4),
                exactMatch(4, 5),
                exactMatch(2, 3)));

        assertThat(exactMatches).hasSize(2);
        assertThat(exactMatches).containsExactlyInAnyOrder(
                exactMatch(0, 4),
                exactMatch(4, 5)
        );
    }

    @Test
    public void testOverlappingBoundariesBiggerSequenceOnRightSide() {
        List<ExactMatch<String>> exactMatches = LookupUtils.removeSubsetsAndSmallerOverlaps(Arrays.asList(
                exactMatch(3, 7),
                exactMatch(5, 10),
                exactMatch(3, 6)));

        assertThat(exactMatches).hasSize(1);
        assertThat(exactMatches).containsExactlyInAnyOrder(
                exactMatch(5, 10)
        );
    }

    @Test
    public void testOverlappingBoundariesBiggerSequenceOnLeftSide() {
        List<ExactMatch<String>> exactMatches = LookupUtils.removeSubsetsAndSmallerOverlaps(Arrays.asList(
                exactMatch(3, 7),
                exactMatch(5, 8),
                exactMatch(3, 6)));

        assertThat(exactMatches).hasSize(1);
        assertThat(exactMatches).containsExactlyInAnyOrder(
                exactMatch(3, 7)
        );
    }

    @Test
    public void testOverlappingBoundariesEquallySized() {
        List<ExactMatch<String>> exactMatches = LookupUtils.removeSubsetsAndSmallerOverlaps(Arrays.asList(
                exactMatch(0, 3),
                exactMatch(1, 4),
                exactMatch(2, 5),
                exactMatch(3, 6),
                exactMatch(4, 7),
                exactMatch(5, 8),
                exactMatch(6, 9)));

        assertThat(exactMatches).hasSize(3);
        assertThat(exactMatches).containsExactlyInAnyOrder(
                exactMatch(0, 3),
                exactMatch(3, 6),
                exactMatch(6, 9)
        );
    }

    @Test
    public void testNoOverlapWithSmoothCrossover() {
        List<ExactMatch<String>> exactMatches = LookupUtils.removeSubsetsAndSmallerOverlaps(Arrays.asList(
                exactMatch(0, 1),
                exactMatch(1, 2),
                exactMatch(2, 3)));
        assertThat(exactMatches).hasSize(3);
    }

    @Test
    public void testSingletonList() {
        List<ExactMatch<String>> exactMatches = Collections.singletonList(exactMatch(0, 1));
        assertThat(exactMatches).hasSize(1);
    }

    @Test
    public void testEmpty() {
        List<ExactMatch<String>> exactMatches = Collections.emptyList();
        assertThat(exactMatches).hasSize(0);
    }

    private ExactMatch<String> exactMatch(int lookupStart, int lookupExclusiveEnd) {
        return new ExactMatch<>(lookupStart, lookupExclusiveEnd, "");
    }

    private PrefixMatch<String> prefixMatch(int lookupOffset) {
        return new PrefixMatch<>(0, "", "").setLookupOffset(lookupOffset);
    }

    private SuffixMatch<String> suffixMatch(int lookupOffset) {
        return new SuffixMatch<>(0, "", "").setLookupOffset(lookupOffset);
    }
}
