package querqy.model.builder.impl;

import org.junit.Test;
import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.Term;
import querqy.model.builder.AbstractBuilderTest;
import querqy.model.builder.model.Occur;

import static org.assertj.core.api.Assertions.assertThat;
import static querqy.model.builder.impl.DisjunctionMaxQueryBuilder.FIELD_NAME_CLAUSES;
import static querqy.model.builder.impl.DisjunctionMaxQueryBuilder.FIELD_NAME_IS_GENERATED;
import static querqy.model.builder.impl.DisjunctionMaxQueryBuilder.FIELD_NAME_OCCUR;
import static querqy.model.builder.impl.DisjunctionMaxQueryBuilder.dmq;
import static querqy.model.builder.impl.TermBuilder.term;

public class DisjunctionMaxQueryBuilderTest extends AbstractBuilderTest {

    @Test
    public void testSetAttributesFromMap() {
        assertThat(new DisjunctionMaxQueryBuilder(
                map(
                        entry(DisjunctionMaxQueryBuilder.NAME_OF_QUERY_TYPE,
                                map(
                                        entry(FIELD_NAME_CLAUSES,
                                                list(
                                                        term("a").toMap(),
                                                        term("b").toMap())),
                                        entry(FIELD_NAME_OCCUR, Occur.MUST.typeName),
                                        entry(FIELD_NAME_IS_GENERATED, "true")))

                ))).isEqualTo(dmq(list(term("a"), term("b")), Occur.MUST, true));
    }

    @Test
    public void testBuilderToMap() {
        DisjunctionMaxQueryBuilder dmqBuilder = dmq(list(term("a"), term("b")), Occur.MUST, true);

        assertThat(dmqBuilder.toMap())
                .isEqualTo(
                        map(
                                entry(DisjunctionMaxQueryBuilder.NAME_OF_QUERY_TYPE,
                                        map(
                                                entry(FIELD_NAME_CLAUSES,
                                                        list(
                                                                term("a").toMap(),
                                                                term("b").toMap())
                                                ),
                                                entry(FIELD_NAME_OCCUR, Occur.MUST.typeName),
                                                entry(FIELD_NAME_IS_GENERATED, true)))
                        )
                );
    }

    @Test
    public void testSetAttributesFromObject() {
        DisjunctionMaxQueryBuilder dmqBuilder = dmq(list(term("a"), term("b")), Occur.MUST, true);

        DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(null, Clause.Occur.MUST, true);
        Term term1 = new Term(dmq, "a");
        Term term2 = new Term(dmq, "b");
        dmq.addClause(term1);
        dmq.addClause(term2);

        assertThat(new DisjunctionMaxQueryBuilder(dmq)).isEqualTo(dmqBuilder);
    }

    @Test
    public void testBuild() {
        DisjunctionMaxQueryBuilder dmqBuilder = dmq(list(term("a"), term("b")), Occur.MUST, true);

        DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(null, Clause.Occur.MUST, true);
        Term term1 = new Term(dmq, "a");
        Term term2 = new Term(dmq, "b");
        dmq.addClause(term1);
        dmq.addClause(term2);

        assertThat(dmqBuilder.build()).isEqualTo(dmq);
    }
}
