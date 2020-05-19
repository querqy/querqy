package querqy.model.builder;

import querqy.model.DisjunctionMaxQuery;
import querqy.model.Term;

public class TermBuilder implements DmqClauseBuilder {

    private CharSequence seq;
    private DisjunctionMaxQuery parent;

    private TermBuilder(final DisjunctionMaxQuery parent, final CharSequence seq) {
        this.parent = parent;
        this.seq = seq;
    }

    @Override
    public TermBuilder setParent(final DisjunctionMaxQuery dmq) {
        this.parent = dmq;
        return this;
    }

    public TermBuilder setSequence(final CharSequence seq) {
        this.seq = seq;
        return this;
    }

    public Term build() {
        return new Term(this.parent, seq, false);
    }

    public static TermBuilder builder() {
        return builder(null);
    }

    public static TermBuilder builder(final DisjunctionMaxQuery parent) {
        return new TermBuilder(parent, null);
    }

    public static TermBuilder fromQuery(Term term) {
        return term(term.getValue());
    }

    public static TermBuilder term(final CharSequence seq) {
        return new TermBuilder(null, seq);
    }

    @Override
    public String toString() {
        return seq.toString();
    }
}
