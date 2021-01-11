package querqy.model.builder.model;

import querqy.model.DisjunctionMaxClause;
import querqy.model.DisjunctionMaxQuery;

public interface DisjunctionMaxClauseBuilder<B extends QueryNodeBuilder, O extends DisjunctionMaxClause>
        extends QueryNodeBuilder<B, O, DisjunctionMaxQuery> {

    default DisjunctionMaxClause buildDisjunctionMaxClause(final DisjunctionMaxQuery parent) {
        return build(parent);
    }
}
