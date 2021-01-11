package querqy.model.builder.impl;

import org.junit.Test;
import querqy.model.BoostQuery;
import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.model.Term;
import querqy.model.builder.AbstractBuilderTest;

import static org.assertj.core.api.Assertions.assertThat;
import static querqy.model.builder.impl.BooleanQueryBuilder.bq;
import static querqy.model.builder.impl.BoostQueryBuilder.boost;
import static querqy.model.builder.impl.ExpandedQueryBuilder.FIELD_NAME_BOOST_DOWN_QUERIES;
import static querqy.model.builder.impl.ExpandedQueryBuilder.FIELD_NAME_BOOST_UP_QUERIES;
import static querqy.model.builder.impl.ExpandedQueryBuilder.FIELD_NAME_FILTER_QUERIES;
import static querqy.model.builder.impl.ExpandedQueryBuilder.FIELD_NAME_USER_QUERY;

public class ExpandedQueryBuilderTest extends AbstractBuilderTest {

    @Test
    public void testSetAttributesFromMap() {
        ExpandedQueryBuilder expandedBuilder = new ExpandedQueryBuilder(
                bq("a"), list(bq("b")), list(boost(bq("c"), 1.0f)), list(boost(bq("d"), 1.0f)));

        assertThat(new ExpandedQueryBuilder(
                map(
                        entry(ExpandedQueryBuilder.NAME_OF_QUERY_TYPE,
                                map(
                                        entry(FIELD_NAME_USER_QUERY, bq("a").toMap()),
                                        entry(FIELD_NAME_FILTER_QUERIES, list(bq("b").toMap())),
                                        entry(FIELD_NAME_BOOST_UP_QUERIES, list(boost(bq("c"), 1.0f).toMap())),
                                        entry(FIELD_NAME_BOOST_DOWN_QUERIES, list(boost(bq("d"), 1.0f).toMap())))

                )))).isEqualTo(expandedBuilder);
    }

    @Test
    public void testBuilderToMap() {
        ExpandedQueryBuilder expandedBuilder = new ExpandedQueryBuilder(
                bq("a"), list(bq("b")), list(boost(bq("c"), 1.0f)), list(boost(bq("d"), 1.0f)));

        assertThat(expandedBuilder.toMap())
                .isEqualTo(
                        map(
                                entry(ExpandedQueryBuilder.NAME_OF_QUERY_TYPE,
                                        map(
                                                entry(FIELD_NAME_USER_QUERY, bq("a").toMap()),
                                                entry(FIELD_NAME_FILTER_QUERIES, list(bq("b").toMap())),
                                                entry(FIELD_NAME_BOOST_UP_QUERIES, list(boost(bq("c"), 1.0f).toMap())),
                                                entry(FIELD_NAME_BOOST_DOWN_QUERIES, list(boost(bq("d"), 1.0f).toMap())))))
                );
    }

    @Test
    public void testSetAttributesFromObject() {
        ExpandedQueryBuilder expandedBuilder = new ExpandedQueryBuilder(
                bq("a"), list(bq("b")), list(boost(bq("c"), 1.0f)), list(boost(bq("d"), 1.0f)));

        ExpandedQuery expandedQuery = new ExpandedQuery(createQuery("a"));
        expandedQuery.addFilterQuery(createQuery("b"));
        expandedQuery.addBoostUpQuery(new BoostQuery(createQuery("c"), 1.0f));
        expandedQuery.addBoostDownQuery(new BoostQuery(createQuery("d"), 1.0f));

        assertThat(new ExpandedQueryBuilder(expandedQuery)).isEqualTo(expandedBuilder);
    }


    @Test
    public void testBuild() {
        ExpandedQueryBuilder expandedBuilder = new ExpandedQueryBuilder(
                bq("a"), list(bq("b")), list(boost(bq("c"), 1.0f)), list(boost(bq("d"), 1.0f)));

        ExpandedQuery expandedQuery = new ExpandedQuery(createQuery("a"));
        expandedQuery.addFilterQuery(createQuery("b"));
        expandedQuery.addBoostUpQuery(new BoostQuery(createQuery("c"), 1.0f));
        expandedQuery.addBoostDownQuery(new BoostQuery(createQuery("d"), 1.0f));

        assertThat(expandedBuilder.build()).isEqualTo(expandedQuery);
    }

    private Query createQuery(String term) {
        Query query = new Query();

        DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, Clause.Occur.SHOULD, false);
        dmq.addClause(new Term(dmq, term));
        query.addClause(dmq);

        return query;
    }
}
