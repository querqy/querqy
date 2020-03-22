package querqy.trie;

import org.junit.Test;
import querqy.trie.model.ExactMatch;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RuleExtractorUtilsTest {

    @Test
    public void testManyOverlappingBoundaries() {
        List<ExactMatch<String>> exactMatches = RuleExtractorUtils.removeSubsetsAndSmallerOverlaps(Arrays.asList(
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
        List<ExactMatch<String>> exactMatches = RuleExtractorUtils.removeSubsetsAndSmallerOverlaps(Arrays.asList(
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
        List<ExactMatch<String>> exactMatches = RuleExtractorUtils.removeSubsetsAndSmallerOverlaps(Arrays.asList(
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
        List<ExactMatch<String>> exactMatches = RuleExtractorUtils.removeSubsetsAndSmallerOverlaps(Arrays.asList(
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
        List<ExactMatch<String>> exactMatches = RuleExtractorUtils.removeSubsetsAndSmallerOverlaps(Arrays.asList(
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
}
