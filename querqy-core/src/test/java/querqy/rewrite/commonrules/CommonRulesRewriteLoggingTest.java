package querqy.rewrite.commonrules;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import querqy.model.ExpandedQuery;
import querqy.rewrite.RewriterOutput;
import querqy.rewrite.logging.InstructionLog;
import querqy.rewrite.logging.MatchLog;
import querqy.rewrite.RewriteLoggingConfig;
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
        final RewriterOutput rewritingOutput = rewriter.rewrite(expandedQuery, searchEngineRequestAdapter);

        assertThat(rewritingOutput.getRewriterLog()).isPresent();
        assertThat(rewritingOutput.getRewriterLog().get().getActionLogs()).isNotEmpty();
    }

    @Test
    public void testThat_rewriteLoggingIsEmpty_forDeactivatedRewriteLogging() {
        when(searchEngineRequestAdapter.getRewriteLoggingConfig())
                .thenReturn(RewriteLoggingConfig.builder().isActive(false).hasDetails(false).build());

        final CommonRulesRewriter rewriter = rewriter(
                rule(input("iphone"), synonym("apple"))
        );

        final ExpandedQuery expandedQuery = expanded(bq("iphone")).build();
        final RewriterOutput rewritingOutput = rewriter.rewrite(expandedQuery, searchEngineRequestAdapter);

        assertThat(rewritingOutput.getRewriterLog()).isPresent();
        assertThat(rewritingOutput.getRewriterLog().get().getActionLogs()).isEmpty();
    }

    @Test
    public void testThat_rewriteLoggingIsEmpty_forActiveLoggingButNoMatch() {
        final CommonRulesRewriter rewriter = rewriter(
                rule(input("iphone"), synonym("apple"))
        );

        final ExpandedQuery expandedQuery = expanded(bq("iphones")).build();
        final RewriterOutput rewritingOutput = rewriter.rewrite(expandedQuery, searchEngineRequestAdapter);

        assertThat(rewritingOutput.getRewriterLog()).isPresent();
        assertThat(rewritingOutput.getRewriterLog().get().getActionLogs()).isEmpty();
        assertThat(rewritingOutput.getRewriterLog().get().hasAppliedRewriting()).isFalse();
    }

    @Test
    public void testThat_matchInformationIsReturned_forAppliedRule() {
        activateRewriteLoggingConfigMock();

        final CommonRulesRewriter rewriter = rewriter(
                rule(input("iphone"), synonym("apple"))
        );

        final ExpandedQuery expandedQuery = expanded(bq("iphone")).build();
        final RewriterOutput rewritingOutput = rewriter.rewrite(expandedQuery, searchEngineRequestAdapter);

        assertThat(rewritingOutput.getRewriterLog()).isPresent();
        assertThat(rewritingOutput.getRewriterLog().get().getActionLogs()).hasSize(1);
        assertThat(rewritingOutput.getRewriterLog().get().getActionLogs().get(0).getMatch().getTerm())
                .isEqualTo("iphone");
        assertThat(rewritingOutput.getRewriterLog().get().getActionLogs().get(0).getMatch().getType())
                .isEqualTo(MatchLog.MatchType.EXACT.getTypeName());
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
        final RewriterOutput rewritingOutput = rewriter.rewrite(expandedQuery, searchEngineRequestAdapter);

        assertThat(rewritingOutput.getRewriterLog()).isPresent();
        assertThat(rewritingOutput.getRewriterLog().get().getActionLogs()).hasSize(1);
        assertThat(rewritingOutput.getRewriterLog().get().getActionLogs().get(0).getMessage())
                .isEqualTo("my message");
    }

    @Test
    public void testThat_instructionLoggingIsNotEmpty_forActivatedRewriteLogging() {
        activateRewriteLoggingConfigMock();

        final CommonRulesRewriter rewriter = rewriter(
                rule(input("iphone"), synonym("apple"))
        );

        final ExpandedQuery expandedQuery = expanded(bq("iphone")).build();
        final RewriterOutput rewritingOutput = rewriter.rewrite(expandedQuery, searchEngineRequestAdapter);

        assertThat(rewritingOutput.getRewriterLog()).isPresent();
        assertThat(rewritingOutput.getRewriterLog().get().getActionLogs()).hasSize(1);
        assertThat(rewritingOutput.getRewriterLog().get().getActionLogs().get(0).getInstructions()).isNotEmpty();
    }

    @Test
    public void testThat_instructionLoggingIsReturned_forAppliedRule() {
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
        final RewriterOutput rewritingOutput = rewriter.rewrite(expandedQuery, searchEngineRequestAdapter);

        assertThat(rewritingOutput.getRewriterLog()).isPresent();
        assertThat(rewritingOutput.getRewriterLog().get().getActionLogs()).hasSize(1);
        assertThat(rewritingOutput.getRewriterLog().get().getActionLogs().get(0).getInstructions()).hasSize(1);

        final InstructionLog instructionLogging = rewritingOutput.getRewriterLog().get().getActionLogs().get(0).getInstructions().get(0);
        assertThat(instructionLogging.getType()).isEqualTo("synonym");
        assertThat(instructionLogging.getParam()).isEqualTo("1.0");
        assertThat(instructionLogging.getValue()).isEqualTo("apple");
    }

    @Test
    public void testThat_instructionLoggingsAreReturned_forMultipleAppliedRules() {
        activateRewriteLoggingConfigMock();

        final InstructionDescription instructionDescription = InstructionDescription.builder()
                .typeName("synonym")
                .build();

        final CommonRulesRewriter rewriter = rewriter(
                rule(input("a"), synonym("b", instructionDescription)),
                rule(input("a"), synonym("d", instructionDescription))
        );

        final ExpandedQuery expandedQuery = expanded(bq("a")).build();
        final RewriterOutput rewritingOutput = rewriter.rewrite(expandedQuery, searchEngineRequestAdapter);

        assertThat(rewritingOutput.getRewriterLog()).isPresent();
        assertThat(rewritingOutput.getRewriterLog().get().getActionLogs()).hasSize(2);
    }

    private void activateRewriteLoggingConfigMock() {
        when(searchEngineRequestAdapter.getRewriteLoggingConfig())
                .thenReturn(RewriteLoggingConfig.builder().isActive(true).hasDetails(true).build());
    }
}
