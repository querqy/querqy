package querqy.rewrite.contrib;

import querqy.model.AbstractNodeVisitor;
import querqy.model.BoostQuery;
import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.Node;
import querqy.model.QuerqyQuery;
import querqy.model.Query;
import querqy.rewrite.logging.ActionLogging;
import querqy.rewrite.logging.RewriterLogging;
import querqy.rewrite.RewriterOutput;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.contrib.replace.ReplaceInstruction;
import querqy.trie.LookupUtils;
import querqy.trie.SequenceLookup;
import querqy.trie.model.ExactMatch;
import querqy.trie.model.PrefixMatch;
import querqy.trie.model.SuffixMatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class ReplaceRewriter extends AbstractNodeVisitor<Node> implements QueryRewriter {

    private final SequenceLookup<ReplaceInstruction> sequenceLookup;

    public ReplaceRewriter(final SequenceLookup<ReplaceInstruction> sequenceLookup) {
        this.sequenceLookup = sequenceLookup;
    }

    private boolean hasReplacement = false;
    private LinkedList<CharSequence> collectedTerms;
    protected SearchEngineRequestAdapter searchEngineRequestAdapter;

    @Override
    public RewriterOutput rewrite(final ExpandedQuery expandedQuery,
                                  final SearchEngineRequestAdapter searchEngineRequestAdapter) {

        final QuerqyQuery<?> querqyQuery = expandedQuery.getUserQuery();

        if (!(querqyQuery instanceof Query)) {
            return RewriterOutput.builder().expandedQuery(expandedQuery).build();
        }

        collectedTerms = new LinkedList<>();
        this.searchEngineRequestAdapter = searchEngineRequestAdapter;

        visit((Query) querqyQuery);

        final List<ActionLogging> actionLoggings = searchEngineRequestAdapter.getRewriteLoggingConfig().hasDetails()
                ? new ArrayList<>() : null;

        final List<ExactMatch<ReplaceInstruction>> exactMatches = sequenceLookup.findExactMatches(collectedTerms);
        if (!exactMatches.isEmpty()) {
            this.hasReplacement = true;

            final List<ExactMatch<ReplaceInstruction>> exactMatchesFiltered =
                    LookupUtils.removeSubsetsAndSmallerOverlaps(exactMatches);

            exactMatchesFiltered.sort(LookupUtils.COMPARE_EXACT_MATCH_BY_LOOKUP_OFFSET_DESC);

            exactMatchesFiltered.forEach(exactMatch ->
                    exactMatch.value.apply(
                            collectedTerms,
                            exactMatch.lookupStart,
                            exactMatch.lookupExclusiveEnd - exactMatch.lookupStart,
                            actionLoggings
                    )
            );
        }

        final List<SuffixMatch<ReplaceInstruction>> suffixMatches = sequenceLookup.findSingleTermSuffixMatches(collectedTerms);
        if (!suffixMatches.isEmpty()) {
            this.hasReplacement = true;

            suffixMatches.sort(LookupUtils.COMPARE_SUFFIX_MATCH_BY_LOOKUP_OFFSET_DESC);

            suffixMatches.forEach(suffixMatch ->
                    suffixMatch.match.apply(
                            collectedTerms,
                            suffixMatch.getLookupOffset(),
                            1,
                            suffixMatch.wildcardMatch,
                            actionLoggings
                    ));
        }

        final List<PrefixMatch<ReplaceInstruction>> prefixMatches = sequenceLookup.findSingleTermPrefixMatches(collectedTerms);
        if (!prefixMatches.isEmpty()) {
            this.hasReplacement = true;

            prefixMatches.sort(LookupUtils.COMPARE_PREFIX_MATCH_BY_LOOKUP_OFFSET_DESC);

            prefixMatches.forEach(prefixMatch ->
                    prefixMatch.match.apply(
                            collectedTerms,
                            prefixMatch.getLookupOffset(),
                            1,
                            prefixMatch.wildcardMatch,
                            actionLoggings
                    ));
        }

        return RewriterOutput.builder()
                .expandedQuery(hasReplacement ? buildQueryFromSeqList(expandedQuery, collectedTerms) : expandedQuery)
                .rewriterLogging(RewriterLogging.builder()
                        .hasAppliedRewriting(hasReplacement)
                        .actionLoggings(actionLoggings)
                        .build())
                .build();
    }

    private ExpandedQuery buildQueryFromSeqList(final ExpandedQuery oldQuery, final List<CharSequence> tokens) {
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

        final Collection<BoostQuery> multiplicativeBoostQueries = oldQuery.getMultiplicativeBoostQueries();
        if (multiplicativeBoostQueries != null) {
            multiplicativeBoostQueries.forEach(newQuery::addMultiplicativeBoostQuery);
        }

        final Collection<QuerqyQuery<?>> filterQueries = oldQuery.getFilterQueries();
        if (filterQueries != null) {
            filterQueries.forEach(newQuery::addFilterQuery);
        }

        return newQuery;
    }

    // TODO: Alternatives in DMQs should be considered

    @Override
    public Node visit(final Term term) {
        if (!term.isGenerated()) {
            collectedTerms.addLast(term);
        }
        return null;
    }
}
