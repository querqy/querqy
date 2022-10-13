package querqy.rewrite.commonrules;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import querqy.model.ExpandedQuery;
import querqy.model.RewritingOutput;
import querqy.model.logging.MatchLogging;
import querqy.model.logging.RewriteLoggingConfig;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.commonrules.model.Instructions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static querqy.model.convert.builder.BooleanQueryBuilder.bq;
import static querqy.model.convert.builder.ExpandedQueryBuilder.expanded;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class CommonRulesRewriteLoggingTest extends AbstractCommonRulesTest {

    @Mock
    SearchEngineRequestAdapter searchEngineRequestAdapter;

    @Test
    public void testThat_rewriteLoggingIsNotEmpty_forActivatedRewriteLoggingAndAppliedRule() {
        activateRewriteLoggingConfigMock();

        final CommonRulesRewriter rewriter = rewriter(
                rule(input("iphone"), synonym("apple"))
        );

        final ExpandedQuery expandedQuery = expanded(bq("iphone")).build();
        final RewritingOutput rewritingOutput = rewriter.rewrite(expandedQuery, searchEngineRequestAdapter);

        assertThat(rewritingOutput.getActionLoggings()).isNotEmpty();
    }

    @Test
    public void testThat_matchInformationIsReturned_forAppliedRule() {
        activateRewriteLoggingConfigMock();

        final CommonRulesRewriter rewriter = rewriter(
                rule(input("iphone"), synonym("apple"))
        );

        final ExpandedQuery expandedQuery = expanded(bq("iphone")).build();
        final RewritingOutput rewritingOutput = rewriter.rewrite(expandedQuery, searchEngineRequestAdapter);

        assertThat(rewritingOutput.getActionLoggings()).hasSize(1);
        assertThat(rewritingOutput.getActionLoggings().get(0).getMatch().getTerm())
                .isEqualTo("iphone");
        assertThat(rewritingOutput.getActionLoggings().get(0).getMatch().getType().getTypeName())
                .isEqualTo(MatchLogging.MatchType.EXACT.getTypeName());
    }

    @Test
    public void testThat_messageIsReturned_forAppliedRule() {
        activateRewriteLoggingConfigMock();

        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("iphone"),
                        synonym("apple"),
                        property(Instructions.StandardPropertyNames.LOG_MESSAGE, "my message")
                )
        );

        final ExpandedQuery expandedQuery = expanded(bq("iphone")).build();
        final RewritingOutput rewritingOutput = rewriter.rewrite(expandedQuery, searchEngineRequestAdapter);

        assertThat(rewritingOutput.getActionLoggings()).hasSize(1);
        assertThat(rewritingOutput.getActionLoggings().get(0).getMessage())
                .isEqualTo("my message");
    }

    private void activateRewriteLoggingConfigMock() {
        when(searchEngineRequestAdapter.getRewriteLoggingConfig())
                .thenReturn(new RewriteLoggingConfig(true, true));
    }
}
