package querqy.trie;

import querqy.trie.model.ExactMatch;
import querqy.trie.model.PrefixMatch;
import querqy.trie.model.SuffixMatch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Utils to handle results or temporary results of TrieMap and SequenceLookup, e. g. to compare, to sort or to filter
 */
public class LookupUtils {

    private LookupUtils(){}

    private static final Comparator<ExactMatch<?>> COMPARE_EXACT_MATCH_BY_SIZE_DESC = (o1, o2) -> o2.termSize - o1.termSize;

    private static final Comparator<ExactMatch<?>> COMPARE_EXACT_MATCH_BY_LOOKUP_START_ASC =
            Comparator.comparingInt(o -> o.lookupStart);

    public static final Comparator<ExactMatch<?>> COMPARE_EXACT_MATCH_BY_LOOKUP_OFFSET_DESC =
            (o1, o2) -> o2.lookupStart - o1.lookupStart;

    public static final Comparator<State<?>> COMPARE_STATE_BY_INDEX_DESC = Comparator.comparingInt(o -> o.index);

    public static final Comparator<SuffixMatch<?>> COMPARE_SUFFIX_MATCH_BY_LOOKUP_OFFSET_DESC =
            (o1, o2) -> o2.getLookupOffset() - o1.getLookupOffset();

    public static final Comparator<PrefixMatch<?>> COMPARE_PREFIX_MATCH_BY_LOOKUP_OFFSET_DESC =
            (o1, o2) -> o2.getLookupOffset() - o1.getLookupOffset();

    public static <T> List<ExactMatch<T>> removeSubsetsAndSmallerOverlaps(List<ExactMatch<T>> exactMatches) {

        final List<ExactMatch<T>> exactMatchesFiltered = new ArrayList<>();

        final List<ExactMatch<T>> exactMatchesSorted = new ArrayList<>(exactMatches);
        exactMatchesSorted.sort(COMPARE_EXACT_MATCH_BY_SIZE_DESC.thenComparing(COMPARE_EXACT_MATCH_BY_LOOKUP_START_ASC));

        exactMatchesSorted.stream()
                .filter(current -> exactMatchesFiltered.stream()
                        .allMatch(compared -> sequencesDoNotOverlap(current.lookupStart, current.lookupExclusiveEnd - 1,
                                compared.lookupStart, compared.lookupExclusiveEnd - 1)))
                .forEach(exactMatchesFiltered::add);

        exactMatchesFiltered.sort(COMPARE_EXACT_MATCH_BY_LOOKUP_START_ASC);

        return exactMatchesFiltered;
    }

    private static boolean sequencesDoNotOverlap(int startSeq1, int endSeq1, int startSeq2, int endSeq2) {
        return startSeq1 < startSeq2 && endSeq1 < startSeq2 || startSeq1 > endSeq2 && endSeq1 > endSeq2;
    }


}
