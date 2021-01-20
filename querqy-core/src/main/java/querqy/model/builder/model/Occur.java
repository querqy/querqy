package querqy.model.builder.model;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import querqy.model.Clause;
import querqy.model.builder.QueryBuilderException;

@AllArgsConstructor
public enum Occur {
    MUST("must", Clause.Occur.MUST),
    SHOULD("should", Clause.Occur.SHOULD),
    MUST_NOT("must_not", Clause.Occur.MUST_NOT);

    private static final Map<String, Occur> MAP_FROM_TYPE_NAME = new HashMap<>(3);
    static {
        MAP_FROM_TYPE_NAME.put(SHOULD.typeName, SHOULD);
        MAP_FROM_TYPE_NAME.put(MUST.typeName, MUST);
        MAP_FROM_TYPE_NAME.put(MUST_NOT.typeName, MUST_NOT);
    }

    public final String typeName;
    public final Clause.Occur objectForClause;

    public static Occur getOccurByTypeName(final String typeName) {
        return MAP_FROM_TYPE_NAME.computeIfAbsent(typeName, key -> {
            throw new QueryBuilderException(String.format("Occur of type %s is unknown", typeName));
        });
    }

    public static Occur getOccurByClauseObject(final Clause.Occur clauseObject) {

        if (SHOULD.objectForClause == clauseObject) {
            return SHOULD;

        } else if (MUST.objectForClause == clauseObject) {
            return MUST;

        } else if (MUST_NOT.objectForClause == clauseObject) {
            return MUST_NOT;

        }

        throw new QueryBuilderException(String.format("Occur of type %s is unknown", clauseObject.toString()));
    }


}
