package querqy.rewrite.commonrules.model;

import org.junit.Test;
import querqy.model.EmptySearchEngineRequestAdapter;
import querqy.model.ExpandedQuery;
import querqy.rewrite.commonrules.AbstractCommonRulesTest;
import querqy.rewrite.commonrules.CommonRulesRewriter;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInputLiteral;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static querqy.model.convert.builder.BooleanQueryBuilder.bq;

public class CommonRulesRewriterBooleanInputTest extends AbstractCommonRulesTest {

    @Test
    public void testRewritingForSingleBooleanInput() {
        final List<BooleanInputLiteral> literals = literals("a", "b");
        booleanInput(literals, filter("f"));

        CommonRulesRewriter rewriter = rewriter(literals);

        ExpandedQuery expandedQuery = rewriter.rewrite(new ExpandedQuery(bq("a", "b", "c").buildQuerqyQuery()),
                new EmptySearchEngineRequestAdapter()).getExpandedQuery();

        assertThat(expandedQuery.getFilterQueries()).isNotNull();
        assertThat(expandedQuery.getFilterQueries()).hasSize(1);
    }

    @Test
    public void testRewritingForMultipleBooleanInput() {
        final List<BooleanInputLiteral> literals = literals("a", "b", "c", "d");
        booleanInput(literals.subList(0, 2), filter("f"));
        booleanInput(literals.subList(1, 3), filter("g"));
        booleanInput(literals.subList(3, 4), filter("h"));

        CommonRulesRewriter rewriter = rewriter(literals);

        ExpandedQuery expandedQuery;

        expandedQuery= rewriter.rewrite(
                new ExpandedQuery(bq("a", "b").buildQuerqyQuery()), new EmptySearchEngineRequestAdapter()).getExpandedQuery();

        assertThat(expandedQuery.getFilterQueries()).isNotNull();
        assertThat(expandedQuery.getFilterQueries()).hasSize(1);

        expandedQuery= rewriter.rewrite(
                new ExpandedQuery(bq("a", "b", "c").buildQuerqyQuery()), new EmptySearchEngineRequestAdapter()).getExpandedQuery();

        assertThat(expandedQuery.getFilterQueries()).isNotNull();
        assertThat(expandedQuery.getFilterQueries()).hasSize(2);

        expandedQuery= rewriter.rewrite(
                new ExpandedQuery(bq("b", "c").buildQuerqyQuery()), new EmptySearchEngineRequestAdapter()).getExpandedQuery();

        assertThat(expandedQuery.getFilterQueries()).isNotNull();
        assertThat(expandedQuery.getFilterQueries()).hasSize(1);

        expandedQuery= rewriter.rewrite(
                new ExpandedQuery(bq("b", "c", "d").buildQuerqyQuery()), new EmptySearchEngineRequestAdapter()).getExpandedQuery();

        assertThat(expandedQuery.getFilterQueries()).isNotNull();
        assertThat(expandedQuery.getFilterQueries()).hasSize(2);

        expandedQuery= rewriter.rewrite(
                new ExpandedQuery(bq("a", "b", "c", "d").buildQuerqyQuery()), new EmptySearchEngineRequestAdapter()).getExpandedQuery();

        assertThat(expandedQuery.getFilterQueries()).isNotNull();
        assertThat(expandedQuery.getFilterQueries()).hasSize(3);

    }

}
