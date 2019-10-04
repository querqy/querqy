package querqy.rewrite.contrib;

import querqy.ComparableCharSequence;
import querqy.CompoundCharSequence;
import querqy.LowerCaseCharSequence;
import querqy.model.AbstractNodeVisitor;
import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.Node;
import querqy.model.Query;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.trie.State;
import querqy.trie.TrieMap;

import java.util.LinkedList;
import java.util.List;

import static querqy.rewrite.contrib.ReplaceRewriterParser.TOKEN_SEPARATOR;

public class ReplaceRewriter extends AbstractNodeVisitor<Node> implements QueryRewriter {

    private final TrieMap<List<ComparableCharSequence>> replaceRules;
    private final boolean ignoreCase;

    public ReplaceRewriter(TrieMap<List<ComparableCharSequence>> replaceRules, boolean ignoreCase) {
        this.replaceRules = replaceRules;
        this.ignoreCase = ignoreCase;
    }

    private LinkedList<ComparableCharSequence> querySeq = new LinkedList<>();
    private LinkedList<ComparableCharSequence> matchSeq = new LinkedList<>();
    private State<List<ComparableCharSequence>> priorMatch = new State<>(false, null, null);
    private boolean hasReplacement = false;

    @Override
    public ExpandedQuery rewrite(ExpandedQuery query) {
        querySeq.clear();
        matchSeq.clear();

        visit((Query) query.getUserQuery());

        if (priorMatch.isFinal()) {
            hasReplacement = true;
            querySeq.addAll(priorMatch.value);
        } else {
            if (!matchSeq.isEmpty()) {
                querySeq.addAll(matchSeq);
            }
        }

        return hasReplacement ? buildQueryFromSeqList(querySeq) : query;
    }

    private ExpandedQuery buildQueryFromSeqList(LinkedList<ComparableCharSequence> tokens) {
        Query query = new Query();
        tokens.forEach(token -> {
            DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, Clause.Occur.SHOULD, false);
            query.addClause(dmq);
            Term term = new Term(dmq, token);
            dmq.addClause(term);
        });

        return new ExpandedQuery(query);
    }

    @Override
    public Node visit(final Term term) {

        if (term.isGenerated()) {
            return null;
        }

        ComparableCharSequence token = term.getValue();

        matchSeq.addLast(token);

        ComparableCharSequence seqForMatching = new CompoundCharSequence(TOKEN_SEPARATOR, matchSeq);
        State<List<ComparableCharSequence>> match = replaceRules.get(
                ignoreCase ? new LowerCaseCharSequence(seqForMatching) : seqForMatching).getStateForCompleteSequence();

        if (!match.isKnown) {
            if (priorMatch.isFinal()) {
                hasReplacement = true;
                querySeq.addAll(priorMatch.value);
                matchSeq.clear();

            } else {
                matchSeq.removeLast();

                if (!matchSeq.isEmpty()) {
                    querySeq.addAll(matchSeq);
                    matchSeq.clear();
                }
            }

            priorMatch = replaceRules.get(token).getStateForCompleteSequence();
            if (priorMatch.isKnown) {
                matchSeq.addLast(token);
            } else {
                querySeq.addLast(token);
            }

        } else {
            priorMatch = match;
        }

        return null;
    }
}
