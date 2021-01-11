package querqy.model.builder.model;

import lombok.AllArgsConstructor;
import querqy.model.Clause;
import querqy.model.builder.QueryBuilderException;

@AllArgsConstructor
public enum Occur {
    MUST("must", Clause.Occur.MUST),
    SHOULD("should", Clause.Occur.SHOULD),
    MUST_NOT("must_not", Clause.Occur.MUST_NOT);

    public final String typeName;
    public final Clause.Occur objectForClause;

    public static Occur getOccurByTypeName(final String typeName) {
        if (SHOULD.typeName.equals(typeName)) {
            return SHOULD;

        } else if (MUST.typeName.equals(typeName)) {
            return MUST;

        } else if (MUST_NOT.typeName.equals(typeName)) {
            return MUST_NOT;

        }

        throw new QueryBuilderException(String.format("Occur of type %s is unknown", typeName));
    }

    public static Occur getOccurByClauseObject(final Clause.Occur clauseObject) {
        if (SHOULD.objectForClause.equals(clauseObject)) {
            return SHOULD;

        } else if (MUST.objectForClause.equals(clauseObject)) {
            return MUST;

        } else if (MUST_NOT.objectForClause.equals(clauseObject)) {
            return MUST_NOT;

        }

        throw new QueryBuilderException(String.format("Occur of type %s is unknown", clauseObject.toString()));
    }


}
