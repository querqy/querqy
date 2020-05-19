package querqy.model.builder;

import querqy.model.DisjunctionMaxClause;
import querqy.model.DisjunctionMaxQuery;

public interface DmqClauseBuilder {

    DisjunctionMaxClause build();
    DmqClauseBuilder setParent(DisjunctionMaxQuery dmq);

}
