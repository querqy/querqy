package querqy.model.builder;

import querqy.model.DisjunctionMaxClause;
import querqy.model.DisjunctionMaxQuery;

public interface DisjunctionMaxClauseBuilder {

    DisjunctionMaxClause build();
    DisjunctionMaxClauseBuilder setParent(DisjunctionMaxQuery dmq);

}
