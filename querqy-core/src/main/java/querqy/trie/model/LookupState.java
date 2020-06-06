package querqy.trie.model;

import querqy.trie.State;

import java.util.Queue;

public class LookupState<T> {

    public final int lookupOffsetStart;
    public final Queue<CharSequence> terms;

    private State<T> state;

    public LookupState(final int lookupOffsetStart, final Queue<CharSequence> terms, final State<T> state) {
        this.lookupOffsetStart = lookupOffsetStart;
        this.terms = terms;
        this.state = state;
    }

    public State<T> getState() {
        return state;
    }

    public LookupState<T> setState(final State<T> state) {
        this.state = state;
        return this;
    }

    public LookupState<T> addTerm(final CharSequence term) {
        terms.add(term);
        return this;
    }
}
