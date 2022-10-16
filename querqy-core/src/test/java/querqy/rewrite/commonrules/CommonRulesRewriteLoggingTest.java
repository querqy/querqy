package querqy.rewrite.commonrules;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import querqy.model.ExpandedQuery;
import querqy.model.RewritingOutput;
import querqy.model.logging.InstructionLogging;
import querqy.model.logging.MatchLogging;
import querqy.model.logging.RewriteLoggingConfig;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.commonrules.model.InstructionDescription;
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
    public void testThat_rewriteLoggingIsNotEmpty_forActivatedRewriteLogging() {
        activateRewriteLoggingConfigMock();

        final CommonRulesRewriter rewriter = rewriter(
                rule(input("iphone"), synonym("apple"))
        );

        final ExpandedQuery expandedQuery = expanded(bq("iphone")).build();
        final RewritingOutput rewritingOutput = rewriter.rewrite(expandedQuery, searchEngineRequestAdapter);

        assertThat(rewritingOutput.getActionLoggings()).isNotEmpty();
    }

    @Test
    public void testThat_rewriteLoggingIsEmpty_forDeactivatedRewriteLogging() {
        when(searchEngineRequestAdapter.getRewriteLoggingConfig())
                .thenReturn(RewriteLoggingConfig.builder().isActive(false).hasDetails(false).build());

        final CommonRulesRewriter rewriter = rewriter(
                rule(input("iphone"), synonym("apple"))
        );

        final ExpandedQuery expandedQuery = expanded(bq("iphone")).build();
        final RewritingOutput rewritingOutput = rewriter.rewrite(expandedQuery, searchEngineRequestAdapter);

        assertThat(rewritingOutput.getActionLoggings()).isEmpty();
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
        assertThat(rewritingOutput.getActionLoggings().get(0).getMatch().getType())
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

    @Test
    public void testThat_instructionLoggingIsNotEmpty_forActivatedRewriteLogging() {
        activateRewriteLoggingConfigMock();

        final CommonRulesRewriter rewriter = rewriter(
                rule(input("iphone"), synonym("apple"))
        );

        final ExpandedQuery expandedQuery = expanded(bq("iphone")).build();
        final RewritingOutput rewritingOutput = rewriter.rewrite(expandedQuery, searchEngineRequestAdapter);

        assertThat(rewritingOutput.getActionLoggings()).hasSize(1);
        assertThat(rewritingOutput.getActionLoggings().get(0).getInstructions()).isNotEmpty();
    }

    @Test
    public void testThat_instructionLoggingIsReturned_appliedRule() {
        activateRewriteLoggingConfigMock();

        final InstructionDescription instructionDescription = InstructionDescription.builder()
                .typeName("synonym")
                .param(1.0f)
                .value("apple")
                .build();

        final CommonRulesRewriter rewriter = rewriter(
                rule(input("iphone"), synonym("apple", instructionDescription))
        );

        final ExpandedQuery expandedQuery = expanded(bq("iphone")).build();
        final RewritingOutput rewritingOutput = rewriter.rewrite(expandedQuery, searchEngineRequestAdapter);

        assertThat(rewritingOutput.getActionLoggings()).hasSize(1);
        assertThat(rewritingOutput.getActionLoggings().get(0).getInstructions()).hasSize(1);

        final InstructionLogging instructionLogging = rewritingOutput.getActionLoggings().get(0).getInstructions().get(0);
        assertThat(instructionLogging.getType()).isEqualTo("synonym");
        assertThat(instructionLogging.getParam()).isEqualTo("1.0");
        assertThat(instructionLogging.getValue()).isEqualTo("apple");
    }

    private void activateRewriteLoggingConfigMock() {
        when(searchEngineRequestAdapter.getRewriteLoggingConfig())
                .thenReturn(RewriteLoggingConfig.builder().isActive(true).hasDetails(true).build());
    }
}
