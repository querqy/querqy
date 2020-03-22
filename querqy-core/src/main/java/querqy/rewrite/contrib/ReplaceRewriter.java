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

        List<SuffixMatch<CharSequence>> suffixMatches = ruleExtractor.findRulesBySingleTermSuffixMatch(collectedTerms);
        if (!suffixMatches.isEmpty()) {
            this.hasReplacement = true;
            suffixMatches.forEach(
                    suffixMatch ->
                        collectedTerms.set(
                                suffixMatch.getLookupOffset(),
                                new CompoundCharSequence(
                                        "",
                                        collectedTerms.get(suffixMatch.getLookupOffset()).subSequence(0, suffixMatch.startSubstring),
                                        suffixMatch.match)));
        }

        List<PrefixMatch<CharSequence>> prefixMatches = ruleExtractor.findRulesBySingleTermPrefixMatch(collectedTerms);
        if (!prefixMatches.isEmpty()) {
            this.hasReplacement = true;
            prefixMatches.forEach(
                    prefixMatch ->
                        collectedTerms.set(
                                prefixMatch.getLookupOffset(),
                                new CompoundCharSequence(
                                        "",
                                        collectedTerms.get(prefixMatch.getLookupOffset()).subSequence(0, prefixMatch.exclusiveEnd),
                                        prefixMatch.match)));
        }

        List<ExactMatch<Queue<CharSequence>>> exactMatches = ruleExtractor.findRulesByExactMatch(collectedTerms);
        if (!exactMatches.isEmpty()) {
            this.hasReplacement = true;
            RuleExtractorUtils.removeSubsetsAndSmallerOverlaps(exactMatches)
                    .forEach(exactMatch -> {
                        IntStream.range(exactMatch.lookupStart, exactMatch.lookupExclusiveEnd)
                                .map(i -> exactMatch.lookupExclusiveEnd - i + exactMatch.lookupStart - 1)
                                .forEach(index -> collectedTerms.remove(index));
                        collectedTerms.addAll(exactMatch.lookupStart, exactMatch.value); });
        }

        return hasReplacement ? buildQueryFromSeqList(expandedQuery, collectedTerms) : expandedQuery;
    }

    private ExpandedQuery buildQueryFromSeqList(ExpandedQuery oldQuery, LinkedList<CharSequence> tokens) {
        final Query query = new Query();
        tokens.forEach(token -> {
            final DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, Clause.Occur.SHOULD, false);
            query.addClause(dmq);
            final Term term = new Term(dmq, token);
            dmq.addClause(term);
        });

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
