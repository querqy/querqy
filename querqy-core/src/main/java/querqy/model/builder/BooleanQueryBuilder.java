package querqy.model.builder;

import querqy.model.BooleanParent;
import querqy.model.BooleanQuery;
import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class BooleanQueryBuilder implements DisjunctionMaxClauseBuilder {

    private final List<DisjunctionMaxQueryBuilder> dmqs;
    private BooleanParent parent;

    protected BooleanQueryBuilder(final BooleanParent parent, final List<DisjunctionMaxQueryBuilder> dmqs) {
        this.parent = parent;
        this.dmqs = dmqs;
    }

    @Override
    public BooleanQueryBuilder setParent(final DisjunctionMaxQuery dmq) {
        this.parent = dmq;
        return this;
    }

    public BooleanQueryBuilder addDmqBuilder(DisjunctionMaxQueryBuilder builder) {
        this.dmqs.add(builder);
        return this;
    }

    @Override
    public BooleanQuery build() {
        final BooleanQuery boolq = new BooleanQuery(this.parent, Clause.Occur.SHOULD, false);

        dmqs.stream().map(dmq -> dmq.setParent(boolq).build()).forEach(boolq::addClause);

        return boolq;
    }

    public static BooleanQueryBuilder builder() {
        return builder(null);
    }

    public static BooleanQueryBuilder builder(final BooleanParent parent) {
        return new BooleanQueryBuilder(parent, new LinkedList<>());
    }

    public static BooleanQueryBuilder fromQuery(BooleanQuery booleanQuery) {
        BooleanQueryBuilder builder = builder();

        booleanQuery.getClauses().stream()
                .map(clause -> {

                    if (clause instanceof DisjunctionMaxQuery) {
                        return DisjunctionMaxQueryBuilder.fromQuery((DisjunctionMaxQuery) clause);

                    } else {
                        throw new UnsupportedOperationException("The structure of this query is currently not supported by builders");
                    }})

                .forEach(builder::addDmqBuilder);

        return builder;
    }


    public static BooleanQueryBuilder bool(final DisjunctionMaxQueryBuilder... dmqs) {
        return new BooleanQueryBuilder(null, Arrays.stream(dmqs).collect(Collectors.toList()));
    }

    @Override
    public String toString() {
        return "bool[" +
                String.join(", ", dmqs.stream()
                        .map(DisjunctionMaxQueryBuilder::toString).collect(Collectors.toList())) + "]";
    }



}
