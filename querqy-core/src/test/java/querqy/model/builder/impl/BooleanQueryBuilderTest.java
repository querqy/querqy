package querqy.model.builder.impl;

import org.junit.Test;
import querqy.model.BooleanQuery;
import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.Term;
import querqy.model.builder.AbstractBuilderTest;
import querqy.model.builder.model.Occur;

import static org.assertj.core.api.Assertions.assertThat;
import static querqy.model.builder.impl.BooleanQueryBuilder.FIELD_NAME_CLAUSES;
import static querqy.model.builder.impl.BooleanQueryBuilder.FIELD_NAME_IS_GENERATED;
import static querqy.model.builder.impl.BooleanQueryBuilder.FIELD_NAME_OCCUR;
import static querqy.model.builder.impl.BooleanQueryBuilder.bq;
import static querqy.model.builder.impl.DisjunctionMaxQueryBuilder.dmq;

public class BooleanQueryBuilderTest extends AbstractBuilderTest {

    @Test
    public void testSetAttributesFromMap() {
        assertThat(new BooleanQueryBuilder(
                map(
                        entry(BooleanQueryBuilder.NAME_OF_QUERY_TYPE,
                                map(
                                        entry(FIELD_NAME_CLAUSES,
                                                list(
                                                        dmq("a").toMap(),
                                                        dmq("b").toMap())),
                                        entry(FIELD_NAME_OCCUR, Occur.MUST.typeName),
                                        entry(FIELD_NAME_IS_GENERATED, "true")))

                ))).isEqualTo(
                        bq(list(dmq("a"), dmq("b")), Occur.MUST, true));
    }

    @Test
    public void testBuilderToMap() {
        BooleanQueryBuilder bqBuilder = bq(list(dmq("a"), dmq("b")), Occur.MUST, true);

        assertThat(bqBuilder.toMap())
                .isEqualTo(
                        map(
                                entry(BooleanQueryBuilder.NAME_OF_QUERY_TYPE,
                                        map(
                                                entry(FIELD_NAME_CLAUSES,
                                                        list(
                                                                dmq("a").toMap(),
                                                                dmq("b").toMap())),
                                                entry(FIELD_NAME_OCCUR, Occur.MUST.typeName),
                                                entry(FIELD_NAME_IS_GENERATED, true)))));
    }

    @Test
    public void testSetAttributesFromObject() {
        BooleanQueryBuilder bqBuilder = bq(list(dmq("a"), dmq("b")), Occur.MUST, true);

        BooleanQuery bq = new BooleanQuery(null, Clause.Occur.MUST, true);

        DisjunctionMaxQuery dmq1 = new DisjunctionMaxQuery(bq, Clause.Occur.SHOULD, false);
        Term term1 = new Term(dmq1, "a");
        dmq1.addClause(term1);
        bq.addClause(dmq1);

        DisjunctionMaxQuery dmq2 = new DisjunctionMaxQuery(bq, Clause.Occur.SHOULD, false);
        Term term2 = new Term(dmq2, "b");
        dmq2.addClause(term2);
        bq.addClause(dmq2);

        assertThat(new BooleanQueryBuilder(bq)).isEqualTo(bqBuilder);
    }

    @Test
    public void testBuild() {
        BooleanQueryBuilder bqBuilder = bq(list(dmq("a"), dmq("b")), Occur.MUST, true);

        BooleanQuery bq = new BooleanQuery(null, Clause.Occur.MUST, true);

        DisjunctionMaxQuery dmq1 = new DisjunctionMaxQuery(bq, Clause.Occur.SHOULD, false);
        Term term1 = new Term(dmq1, "a");
        dmq1.addClause(term1);
        bq.addClause(dmq1);

        DisjunctionMaxQuery dmq2 = new DisjunctionMaxQuery(bq, Clause.Occur.SHOULD, false);
        Term term2 = new Term(dmq2, "b");
        dmq2.addClause(term2);
        bq.addClause(dmq2);

        assertThat(bqBuilder.build()).isEqualTo(bq);
    }

}
