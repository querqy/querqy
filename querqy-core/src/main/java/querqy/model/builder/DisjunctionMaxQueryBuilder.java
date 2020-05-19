package querqy.model.builder;

import querqy.ComparableCharSequence;
import querqy.model.BooleanQuery;
import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.Term;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static querqy.model.builder.TermBuilder.term;

public class DisjunctionMaxQueryBuilder {
    private final List<DmqClauseBuilder> clauses;
    private BooleanQuery parent;

    public DisjunctionMaxQueryBuilder(final BooleanQuery parent, final List<DmqClauseBuilder> clauses) {
        this.parent = parent;
        this.clauses = clauses;
    }

    public DisjunctionMaxQueryBuilder setParent(BooleanQuery parent) {
        this.parent = parent;
        return this;
    }

    public DisjunctionMaxQueryBuilder addDmqClauseBuilder(DmqClauseBuilder clause) {
        this.clauses.add(clause);
        return this;
    }

    public DisjunctionMaxQuery build() {
        final DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(this.parent, Clause.Occur.SHOULD, false);

        clauses.stream().map(clause -> clause.setParent(dmq).build()).forEach(dmq::addClause);

        return dmq;
    }

    public static DisjunctionMaxQueryBuilder builder() {
        return builder(null);
    }

    public static DisjunctionMaxQueryBuilder builder(final BooleanQuery parent) {
        return new DisjunctionMaxQueryBuilder(parent, new LinkedList<>());
    }

    public static DisjunctionMaxQueryBuilder fromQuery(DisjunctionMaxQuery dmq) {
        final DisjunctionMaxQueryBuilder builder = builder();

        dmq.getClauses().stream()
                .map(clause -> {

                    if (clause instanceof Term) {
                        return TermBuilder.fromQuery((Term) clause);

                    } else if (clause instanceof BooleanQuery){
                        return BooleanQueryBuilder.fromQuery((BooleanQuery) clause);

                    } else {
                        throw new UnsupportedOperationException("The structure of this query is currently not supported by builders");
                    }})

                .forEach(builder::addDmqClauseBuilder);

        return builder;
    }

    public static DisjunctionMaxQueryBuilder dmq(DmqClauseBuilder... clauses) {
        return new DisjunctionMaxQueryBuilder(null, Arrays.asList(clauses));
    }

    public static DisjunctionMaxQueryBuilder dmq(ComparableCharSequence... terms) {
        return new DisjunctionMaxQueryBuilder(null, Arrays.stream(terms).map(TermBuilder::term).collect(Collectors.toList()));
    }

    public static DisjunctionMaxQueryBuilder dmq(String... terms) {
        return new DisjunctionMaxQueryBuilder(null, Arrays.stream(terms)
                .map(TermBuilder::term).collect(Collectors.toList()));
    }

    @Override
    public String toString() {

        return "dmq(" +
                String.join(", ", clauses.stream()
                        .map(Object::toString).collect(Collectors.toList())) + ")";
    }



}
