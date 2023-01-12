package querqy.rewrite.lookup.triemap.model;

import querqy.model.Term;
import querqy.trie.States;

import java.util.List;

public class TrieMapSequence<T> {

    private final States<T> states;
    private final List<Term> terms;

    private TrieMapSequence(final States<T> states, final List<Term> terms) {
        this.states = states;
        this.terms = terms;
    }

    public States<T> getStates() {
        return states;
    }

    public List<Term> getTerms() {
        return terms;
    }

    public static <T> TrieMapSequence<T> of(final States<T> states, final List<Term> terms) {
        return new TrieMapSequence<>(states, terms);
    }
}
