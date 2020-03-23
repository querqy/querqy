package querqy.rewrite.contrib;

import querqy.CompoundCharSequence;
import querqy.model.AbstractNodeVisitor;
import querqy.model.BoostQuery;
import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.Node;
import querqy.model.QuerqyQuery;
import querqy.model.Query;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.trie.RuleExtractor;
import querqy.trie.RuleExtractorUtils;
import querqy.trie.model.ExactMatch;
import querqy.trie.model.PrefixMatch;
import querqy.trie.model.SuffixMatch;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.IntStream;

public class ReplaceRewriter extends AbstractNodeVisitor<Node> implements QueryRewriter {

    private final RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor;

    public ReplaceRewriter(
            RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor) {
        this.ruleExtractor= ruleExtractor;
    }

    private boolean hasReplacement = false;
    private LinkedList<CharSequence> collectedTerms;

    @Override
    public ExpandedQuery rewrite(ExpandedQuery expandedQuery) {

        final QuerqyQuery<?> querqyQuery = expandedQuery.getUserQuery();

        if (!(querqyQuery instanceof Query)) {
            return expandedQuery;
        }

        collectedTerms = new LinkedList<>();

        visit((Query) querqyQuery);

        final List<SuffixMatch<CharSequence>> suffixMatches = ruleExtractor.findRulesBySingleTermSuffixMatch(collectedTerms);
        if (!suffixMatches.isEmpty()) {
            this.hasReplacement = true;

            for (final SuffixMatch<CharSequence> suffixMatch : suffixMatches) {
                collectedTerms.set(
                        suffixMatch.getLookupOffset(),
                        new CompoundCharSequence(
                                "",
                                collectedTerms.get(suffixMatch.getLookupOffset()).subSequence(0, suffixMatch.startSubstring),
                                suffixMatch.match));
            }
        }

        final List<PrefixMatch<CharSequence>> prefixMatches = ruleExtractor.findRulesBySingleTermPrefixMatch(collectedTerms);
        if (!prefixMatches.isEmpty()) {
            this.hasReplacement = true;

            for (final PrefixMatch<CharSequence> prefixMatch : prefixMatches) {
                CharSequence replacementTerm = collectedTerms.get(prefixMatch.getLookupOffset());

                collectedTerms.set(
                        prefixMatch.getLookupOffset(),
                        new CompoundCharSequence(
                                "",
                                prefixMatch.match,
                                replacementTerm.subSequence(prefixMatch.exclusiveEnd, replacementTerm.length())));
            }
        }

        for (int i = collectedTerms.size() - 1; i >= 0; i--) {
            if (collectedTerms.get(i).length() == 0) {
                collectedTerms.remove(i);
            }
        }

        final List<ExactMatch<Queue<CharSequence>>> exactMatches = ruleExtractor.findRulesByExactMatch(collectedTerms);
        if (!exactMatches.isEmpty()) {
            this.hasReplacement = true;

            int indexOffsetAfterReplacement = 0;

            final List<ExactMatch<Queue<CharSequence>>> exactMatchesFiltered = RuleExtractorUtils.removeSubsetsAndSmallerOverlaps(exactMatches);

            for (final ExactMatch<Queue<CharSequence>> exactMatch : exactMatchesFiltered) {
                final int numberOfTermsToBeReplaced = exactMatch.lookupExclusiveEnd - exactMatch.lookupStart;

                final int indexStart = exactMatch.lookupStart + indexOffsetAfterReplacement;
                final int indexExclusiveEnd = exactMatch.lookupExclusiveEnd + indexOffsetAfterReplacement;

                IntStream.range(0, indexExclusiveEnd - indexStart).forEach(i -> collectedTerms.remove(indexStart));
                collectedTerms.addAll(indexStart, exactMatch.value);

                final int querySizeDelta = exactMatch.value.size() - numberOfTermsToBeReplaced;
                indexOffsetAfterReplacement += querySizeDelta;
            }
        }

        return hasReplacement ? buildQueryFromSeqList(expandedQuery, collectedTerms) : expandedQuery;
    }

    private ExpandedQuery buildQueryFromSeqList(ExpandedQuery oldQuery, LinkedList<CharSequence> tokens) {
        final Query query = new Query();

        for (final CharSequence token : tokens) {
            final DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, Clause.Occur.SHOULD, false);
            query.addClause(dmq);
            final Term term = new Term(dmq, token);
            dmq.addClause(term);
        }

        final ExpandedQuery newQuery = new ExpandedQuery(query);

        final Collection<BoostQuery> boostDownQueries = oldQuery.getBoostDownQueries();
        if (boostDownQueries != null) {
            boostDownQueries.forEach(newQuery::addBoostDownQuery);
        }

        final Collection<BoostQuery> boostUpQueries = oldQuery.getBoostUpQueries();
        if (boostUpQueries != null) {
            boostUpQueries.forEach(newQuery::addBoostUpQuery);
        }

        final Collection<QuerqyQuery<?>> filterQueries = oldQuery.getFilterQueries();
        if (filterQueries != null) {
            filterQueries.forEach(newQuery::addFilterQuery);
        }

        return newQuery;
    }

    @Override
    public Node visit(final Term term) {
        if (!term.isGenerated()) {
            collectedTerms.addLast(term.getValue());
        }
        return null;
    }

}
