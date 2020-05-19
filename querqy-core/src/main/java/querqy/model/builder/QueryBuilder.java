package querqy.model.builder;

import querqy.ComparableCharSequenceWrapper;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.Node;
import querqy.model.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class QueryBuilder {

    private final List<DisjunctionMaxQueryBuilder> dmqs;

    private QueryBuilder(final List<DisjunctionMaxQueryBuilder> dmqs) {
        this.dmqs = dmqs;
    }

    public QueryBuilder setParent(Node parent) {
        throw new UnsupportedOperationException("Not allowed to set parent Node for QueryBuilder");
    }

    public QueryBuilder addDmqBuilder(DisjunctionMaxQueryBuilder builder) {
        this.dmqs.add(builder);
        return this;
    }

    public Query build() {
        final Query query = new Query();
        dmqs.stream().map(dmq -> dmq.setParent(query).build()).forEach(query::addClause);
        return query;
    }

    public static QueryBuilder builder() {
        return new QueryBuilder(new ArrayList<>());
    }

    public static QueryBuilder fromQuery(Query query) {
        QueryBuilder builder = builder();

        query.getClauses().stream()
                .map(clause -> {

                    if (clause instanceof DisjunctionMaxQuery) {
                        return DisjunctionMaxQueryBuilder.fromQuery((DisjunctionMaxQuery) clause);

                    } else {
                        throw new UnsupportedOperationException("The structure of this query is currently not supported by builders");
                    }})

                .forEach(builder::addDmqBuilder);

        return builder;
    }

    public static QueryBuilder query() {
        return new QueryBuilder(Collections.emptyList());
    }

    public static QueryBuilder query(final DisjunctionMaxQueryBuilder... dmqs) {
        return new QueryBuilder(Arrays.stream(dmqs).collect(Collectors.toList()));
    }

    public static QueryBuilder query(final String... terms) {
        return new QueryBuilder(Arrays.stream(terms)
                .map(ComparableCharSequenceWrapper::new)
                .map(DisjunctionMaxQueryBuilder::dmq)
                .collect(Collectors.toList()));
    }

    @Override
    public String toString() {
        return "query[" +
                String.join(", ", dmqs.stream()
                        .map(Object::toString).collect(Collectors.toList())) + "]";
    }
}
