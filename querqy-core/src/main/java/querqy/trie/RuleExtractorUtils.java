package querqy.trie;

import querqy.trie.model.ExactMatch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RuleExtractorUtils {

    private RuleExtractorUtils(){}

    private static final Comparator<ExactMatch<?>> COMPARE_BY_SIZE_DESC = (o1, o2) -> o2.termSize - o1.termSize;
    private static final Comparator<ExactMatch<?>> COMPARE_BY_ORDER_ASC = Comparator.comparingInt(o -> o.lookupStart);

    public static <T> List<ExactMatch<T>> removeSubsetsAndSmallerOverlaps(List<ExactMatch<T>> exactMatches) {
        final List<ExactMatch<T>> exactMatchesFiltered = new ArrayList<>();
        exactMatches.stream()
                .sorted(COMPARE_BY_SIZE_DESC.thenComparing(COMPARE_BY_ORDER_ASC))
                .filter(current -> exactMatchesFiltered.stream()
                        .allMatch(compared -> coordinatesDoNotOverlap(current.lookupStart, current.lookupExclusiveEnd - 1,
                                compared.lookupStart, compared.lookupExclusiveEnd - 1)))
                .forEach(exactMatchesFiltered::add);

        return exactMatchesFiltered;
    }

    private static boolean coordinatesDoNotOverlap(int a1, int a2, int b1, int b2) {
        return a1 < b1 && a2 < b1 || a1 > b2 && a2 > b2;
    }
}
