package querqy.model.convert;

import querqy.model.BooleanQuery;
import querqy.model.DisjunctionMaxClause;
import querqy.model.MatchAllQuery;
import querqy.model.QuerqyQuery;
import querqy.model.StringRawQuery;
import querqy.model.Term;
import querqy.model.convert.builder.BooleanQueryBuilder;
import querqy.model.convert.builder.MatchAllQueryBuilder;
import querqy.model.convert.builder.StringRawQueryBuilder;
import querqy.model.convert.builder.TermBuilder;
import querqy.model.convert.model.DisjunctionMaxClauseBuilder;
import querqy.model.convert.model.QuerqyQueryBuilder;

import java.util.Map;

public class BuilderFactory {

    private BuilderFactory() {}

    public static QuerqyQueryBuilder createQuerqyQueryBuilderFromObject(final QuerqyQuery querqyQuery) {

        if (querqyQuery instanceof BooleanQuery) {
            return new BooleanQueryBuilder((BooleanQuery) querqyQuery);

        } else if (querqyQuery instanceof StringRawQuery) {
            return new StringRawQueryBuilder((StringRawQuery) querqyQuery);

        } else if (querqyQuery instanceof MatchAllQuery) {
            return new MatchAllQueryBuilder((MatchAllQuery) querqyQuery);

        } else {
            throw new QueryBuilderException("The structure of this query is currently not supported by builders");
        }
    }

    public static QuerqyQueryBuilder createQuerqyQueryBuilderFromMap(final Map map) {
        final String nameOfQueryType = TypeCastingUtils.expectMapToContainExactlyOneEntryAndGetKey(map);

        if (BooleanQueryBuilder.NAME_OF_QUERY_TYPE.equals(nameOfQueryType)) {
            return new BooleanQueryBuilder(map);

        } else if (StringRawQueryBuilder.NAME_OF_QUERY_TYPE.equals(nameOfQueryType)) {
            return new StringRawQueryBuilder(map);

        } else if (MatchAllQueryBuilder.NAME_OF_QUERY_TYPE.equals(nameOfQueryType)) {
            return new MatchAllQueryBuilder(map);

        } else {
            throw new QueryBuilderException(String.format("Unexpected name of query type: %s", nameOfQueryType));
        }
    }

    public static DisjunctionMaxClauseBuilder createDisjunctionMaxClauseBuilderFromObject(
            final DisjunctionMaxClause clause) {

        if (clause instanceof Term) {
            return new TermBuilder((Term) clause);

        } else if (clause instanceof BooleanQuery){
            return new BooleanQueryBuilder((BooleanQuery) clause);

        } else {
            throw new QueryBuilderException("The structure of this query is currently not supported by builders");
        }
    }

    public static DisjunctionMaxClauseBuilder createDisjunctionMaxClauseBuilderFromMap(final Map map) {
        final String nameOfQueryType = TypeCastingUtils.expectMapToContainExactlyOneEntryAndGetKey(map);

        if (TermBuilder.NAME_OF_QUERY_TYPE.equals(nameOfQueryType)) {
            return new TermBuilder(map);

        } else if (BooleanQueryBuilder.NAME_OF_QUERY_TYPE.equals(nameOfQueryType)) {
            return new BooleanQueryBuilder(map);

        } else {
            throw new QueryBuilderException(String.format("Unexpected name of query type: %s", nameOfQueryType));
        }
    }



}
