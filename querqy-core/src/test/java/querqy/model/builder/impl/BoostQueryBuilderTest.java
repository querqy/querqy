package querqy.model.builder.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import querqy.model.BoostQuery;
import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.Query;
import querqy.model.Term;
import querqy.model.builder.AbstractBuilderTest;
import querqy.model.builder.model.QuerqyQueryBuilder;
import querqy.model.builder.QueryBuilderException;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static querqy.model.builder.impl.BooleanQueryBuilder.bq;
import static querqy.model.builder.impl.BoostQueryBuilder.FIELD_NAME_BOOST;
import static querqy.model.builder.impl.BoostQueryBuilder.FIELD_NAME_QUERY;
import static querqy.model.builder.impl.BoostQueryBuilder.boost;

@RunWith(MockitoJUnitRunner.class)
public class BoostQueryBuilderTest extends AbstractBuilderTest {

    @Test
    public void testThatExceptionIsThrownQueryValueIsNull() {
        assertThatThrownBy(() -> new BoostQueryBuilder(Collections.emptyMap()).build())
                .isInstanceOf(QueryBuilderException.class);
    }

    @Test
    public void testThatNoExceptionIsThrownIfQueryIsNotNull() {
        assertThatCode(() -> new BoostQueryBuilder(mock(QuerqyQueryBuilder.class)).build()).doesNotThrowAnyException();
    }

    @Test
    public void testSetAttributesFromMap() {
        assertThat(new BoostQueryBuilder(
                map(
                        entry(BoostQueryBuilder.NAME_OF_QUERY_TYPE,
                                map(
                                        entry(FIELD_NAME_QUERY, bq("a").toMap()),
                                        entry(FIELD_NAME_BOOST, 1.0f)))

                ))).isEqualTo(boost(bq("a"), 1.0f));
    }

    @Test
    public void testBuilderToMap() {
        BoostQueryBuilder boostBuilder = boost(bq("a"), 1.0f);

        assertThat(boostBuilder.toMap())
                .isEqualTo(
                        map(
                                entry(
                                        BoostQueryBuilder.NAME_OF_QUERY_TYPE,
                                        map(
                                                entry(FIELD_NAME_QUERY, bq("a").toMap()),
                                                entry(FIELD_NAME_BOOST, 1.0f))
                                )
                        )
                );
    }

    @Test
    public void testSetAttributesFromObject() {
        BoostQueryBuilder boostBuilder = boost(bq("a"), 1.0f);

        Query query = new Query();

        DisjunctionMaxQuery dmq1 = new DisjunctionMaxQuery(query, Clause.Occur.SHOULD, false);
        Term term1 = new Term(dmq1, "a");
        dmq1.addClause(term1);
        query.addClause(dmq1);

        assertThat(new BoostQueryBuilder(new BoostQuery(query, 1.0f))).isEqualTo(boostBuilder);
    }

    @Test
    public void testBuild() {
        BoostQueryBuilder boostBuilder = boost(bq("a"), 1.0f);

        Query query = new Query();

        DisjunctionMaxQuery dmq1 = new DisjunctionMaxQuery(query, Clause.Occur.SHOULD, false);
        Term term1 = new Term(dmq1, "a");
        dmq1.addClause(term1);
        query.addClause(dmq1);

        BoostQuery expectedBoostQuery = new BoostQuery(query, 1.0f);
        BoostQuery actualBoostQuery = boostBuilder.build();

        assertThat(actualBoostQuery).isEqualTo(expectedBoostQuery);
    }

}
