package querqy.rewrite.lookup.triemap.model;

import querqy.model.Term;
import querqy.trie.States;

import java.util.List;

public class TrieMapEvaluation<ValueT> {

    private final List<Term> previousTerms;
    private final Term lastTerm;
    private final States<ValueT> states;

    private TrieMapEvaluation(final List<Term> previousTerms, final Term lastTerm, final States<ValueT> states) {
        this.previousTerms = previousTerms;
        this.lastTerm = lastTerm;
        this.states = states;
    }

    public List<Term> getPreviousTerms() {
        return previousTerms;
    }

    public Term getLastTerm() {
        return lastTerm;
    }

    public States<ValueT> getStates() {
        return states;
    }

    public static <ValueT> TrieMapEvaluation<ValueT> of(final List<Term> previousTerms, final Term lastTerm, final States<ValueT> states) {
        return new TrieMapEvaluation<>(previousTerms, lastTerm, states);
    }
}
