package querqy.trie.model;

import querqy.trie.State;

import java.util.Queue;

public class LookupState<T> {

    public final int lookupOffsetStart;
    public final Queue<CharSequence> terms;

    private State<T> state;

    public LookupState(int lookupOffsetStart, Queue<CharSequence> terms, State<T> state) {
        this.lookupOffsetStart = lookupOffsetStart;
        this.terms = terms;
        this.state = state;
    }

    public State<T> getState() {
        return state;
    }

    public LookupState<T> setState(State<T> state) {
        this.state = state;
        return this;
    }

    public LookupState<T> addTerm(CharSequence term) {
        terms.add(term);
        return this;
    }
}
