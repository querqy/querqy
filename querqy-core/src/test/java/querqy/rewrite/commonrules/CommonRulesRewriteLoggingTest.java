package querqy.rewrite.commonrules;

import org.junit.Test;
import querqy.model.ExpandedQuery;
import querqy.model.RewritingOutput;

import static org.assertj.core.api.Assertions.assertThat;
import static querqy.model.convert.builder.BooleanQueryBuilder.bq;
import static querqy.model.convert.builder.ExpandedQueryBuilder.expanded;

public class CommonRulesRewriteLoggingTest extends AbstractCommonRulesTest {

    @Test(expected = AssertionError.class)
    public void testThat_rewriteLoggingIsNotEmpty_forActivatedRewriteLoggingAndAppliedRule() {
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("iphone"),
                        synonym("apple")
                )
        );

        final ExpandedQuery expandedQuery = expanded(bq("iphone")).build();
        final RewritingOutput rewritingOutput = rewriter.rewrite(expandedQuery, emptyAdapter());

        assertThat(rewritingOutput.getActionLoggings()).isNotEmpty();
    }
}
