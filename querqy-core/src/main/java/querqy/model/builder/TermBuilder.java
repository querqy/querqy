package querqy.model.builder;

import querqy.ComparableCharSequenceContainer;
import querqy.ComparableCharSequence;
import querqy.ComparableCharSequenceWrapper;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.Term;

public class TermBuilder implements DisjunctionMaxClauseBuilder {

    private ComparableCharSequence seq;
    private DisjunctionMaxQuery parent;

    private TermBuilder(final DisjunctionMaxQuery parent, final ComparableCharSequence seq) {
        this.parent = parent;
        this.seq = seq;
    }

    @Override
    public TermBuilder setParent(final DisjunctionMaxQuery dmq) {
        this.parent = dmq;
        return this;
    }

    public TermBuilder setSequence(final ComparableCharSequence seq) {
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

    public static TermBuilder fromQuery(ComparableCharSequenceContainer term) {
        return term(term.getComparableCharSequence());
    }

    public static TermBuilder term(final ComparableCharSequence seq) {
        return new TermBuilder(null, seq);
    }

    public static TermBuilder term(final String seq) {
        return term(new ComparableCharSequenceWrapper(seq));
    }

    @Override
    public String toString() {
        return seq.toString();
    }
}
