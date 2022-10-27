package querqy.rewrite.contrib;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import querqy.model.ExpandedQuery;
import querqy.model.logging.InstructionLogging;
import querqy.model.logging.MatchLogging;
import querqy.model.logging.RewriteLoggingConfig;
import querqy.model.rewriting.RewriterOutput;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.contrib.replace.ReplaceInstruction;
import querqy.rewrite.contrib.replace.TermsReplaceInstruction;
import querqy.trie.SequenceLookup;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static querqy.model.convert.builder.BooleanQueryBuilder.bq;
import static querqy.model.convert.builder.ExpandedQueryBuilder.expanded;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class ReplaceRewriterLoggingTest {

    @Mock
    SearchEngineRequestAdapter searchEngineRequestAdapter;

    @Test
    public void testThat_rewriteLoggingIsNotEmpty_forActivatedRewriteLogging() {
        activateRewriteLoggingConfigMock();

        final ReplaceRewriter rewriter = rewriter(
                rule(input("iphone"), termsInstruction("apple"))
        );

        final ExpandedQuery expandedQuery = expanded(bq("iphone")).build();
        final RewriterOutput rewritingOutput = rewriter.rewrite(expandedQuery, searchEngineRequestAdapter);

        assertThat(rewritingOutput.getRewriterLogging()).isPresent();
        assertThat(rewritingOutput.getRewriterLogging().get().getActionLoggings()).isNotEmpty();
    }

    @Test
    public void testThat_rewriteLoggingHasTwoLoggings_forTwoAppliedRules() {
        activateRewriteLoggingConfigMock();

        final ReplaceRewriter rewriter = rewriter(
                rule(input("iphone"), termsInstruction("apple")),
                rule(input("8"), termsInstruction("apple"))
        );

        final ExpandedQuery expandedQuery = expanded(bq("iphone", "8")).build();
        final RewriterOutput rewritingOutput = rewriter.rewrite(expandedQuery, searchEngineRequestAdapter);

        assertThat(rewritingOutput.getRewriterLogging()).isPresent();
        assertThat(rewritingOutput.getRewriterLogging().get().getActionLoggings()).hasSize(2);
    }

    @Test
    public void testThat_rewriteLoggingIsEmpty_forDeactivatedRewriteLogging() {
        when(searchEngineRequestAdapter.getRewriteLoggingConfig())
                .thenReturn(RewriteLoggingConfig.builder().isActive(false).hasDetails(false).build());

        final ReplaceRewriter rewriter = rewriter(
                rule(input("iphone"), termsInstruction("apple"))
        );

        final ExpandedQuery expandedQuery = expanded(bq("iphone")).build();
        final RewriterOutput rewritingOutput = rewriter.rewrite(expandedQuery, searchEngineRequestAdapter);

        assertThat(rewritingOutput.getRewriterLogging()).isPresent();
        assertThat(rewritingOutput.getRewriterLogging().get().getActionLoggings()).isEmpty();
        assertThat(rewritingOutput.getRewriterLogging().get().hasAppliedRewriting()).isTrue();
    }

    @Test
    public void testThat_rewriteLoggingIsEmpty_forActiveLoggingButNoMatch() {
        when(searchEngineRequestAdapter.getRewriteLoggingConfig())
                .thenReturn(RewriteLoggingConfig.builder().isActive(true).hasDetails(true).build());

        final ReplaceRewriter rewriter = rewriter(
                rule(input("iphone"), termsInstruction("apple"))
        );

        final ExpandedQuery expandedQuery = expanded(bq("iphones")).build();
        final RewriterOutput rewritingOutput = rewriter.rewrite(expandedQuery, searchEngineRequestAdapter);

        assertThat(rewritingOutput.getRewriterLogging()).isPresent();
        assertThat(rewritingOutput.getRewriterLogging().get().getActionLoggings()).isEmpty();
        assertThat(rewritingOutput.getRewriterLogging().get().hasAppliedRewriting()).isFalse();
    }

    @Test
    public void testThat_matchInformationIsReturned_forAppliedRule() {
        activateRewriteLoggingConfigMock();

        final ReplaceRewriter rewriter = rewriter(
                rule(input("iphone"), termsInstruction("apple"))
        );

        final ExpandedQuery expandedQuery = expanded(bq("iphone")).build();
        final RewriterOutput rewritingOutput = rewriter.rewrite(expandedQuery, searchEngineRequestAdapter);

        assertThat(rewritingOutput.getRewriterLogging()).isPresent();
        assertThat(rewritingOutput.getRewriterLogging().get().getActionLoggings()).hasSize(1);
        assertThat(rewritingOutput.getRewriterLogging().get().getActionLoggings().get(0).getMatch().getTerm())
                .isEqualTo("iphone");
        assertThat(rewritingOutput.getRewriterLogging().get().getActionLoggings().get(0).getMatch().getType())
                .isEqualTo(MatchLogging.MatchType.EXACT.getTypeName());
    }

    @Test
    public void testThat_messageIsReturned_forAppliedRule() {
        activateRewriteLoggingConfigMock();

        final ReplaceRewriter rewriter = rewriter(
                rule(input("iphone"), termsInstruction("apple"))
        );

        final ExpandedQuery expandedQuery = expanded(bq("iphone")).build();
        final RewriterOutput rewritingOutput = rewriter.rewrite(expandedQuery, searchEngineRequestAdapter);

        assertThat(rewritingOutput.getRewriterLogging()).isPresent();
        assertThat(rewritingOutput.getRewriterLogging().get().getActionLoggings()).hasSize(1);
        assertThat(rewritingOutput.getRewriterLogging().get().getActionLoggings().get(0).getMessage())
                .isEqualTo("iphone => apple");
    }

    @Test
    public void testThat_instructionLoggingIsNotEmpty_forActivatedRewriteLogging() {
        activateRewriteLoggingConfigMock();

        final ReplaceRewriter rewriter = rewriter(
                rule(input("iphone"), termsInstruction("apple"))
        );

        final ExpandedQuery expandedQuery = expanded(bq("iphone")).build();
        final RewriterOutput rewritingOutput = rewriter.rewrite(expandedQuery, searchEngineRequestAdapter);

        assertThat(rewritingOutput.getRewriterLogging()).isPresent();
        assertThat(rewritingOutput.getRewriterLogging().get().getActionLoggings()).hasSize(1);
        assertThat(rewritingOutput.getRewriterLogging().get().getActionLoggings().get(0).getInstructions()).isNotEmpty();
    }

    @Test
    public void testThat_instructionLoggingIsReturned_forAppliedRule() {
        activateRewriteLoggingConfigMock();

        final ReplaceRewriter rewriter = rewriter(
                rule(input("iphone"), termsInstruction("apple"))
        );

        final ExpandedQuery expandedQuery = expanded(bq("iphone")).build();
        final RewriterOutput rewritingOutput = rewriter.rewrite(expandedQuery, searchEngineRequestAdapter);

        assertThat(rewritingOutput.getRewriterLogging()).isPresent();
        assertThat(rewritingOutput.getRewriterLogging().get().getActionLoggings()).hasSize(1);
        assertThat(rewritingOutput.getRewriterLogging().get().getActionLoggings().get(0).getInstructions()).hasSize(1);

        final InstructionLogging instructionLogging = rewritingOutput.getRewriterLogging().get().getActionLoggings().get(0).getInstructions().get(0);
        assertThat(instructionLogging.getType()).isEqualTo("replace");
        assertThat(instructionLogging.getValue()).isEqualTo("apple");
    }

    private void activateRewriteLoggingConfigMock() {
        when(searchEngineRequestAdapter.getRewriteLoggingConfig())
                .thenReturn(RewriteLoggingConfig.builder().isActive(true).hasDetails(true).build());
    }

    private ReplaceRewriter rewriter(final Rule... rules) {
        final SequenceLookup<ReplaceInstruction> sequenceLookup = new SequenceLookup<>();

        Arrays.stream(rules).forEach(
                rule -> sequenceLookup.put(rule.getInput(), rule.getReplaceInstruction())
        );

        return new ReplaceRewriter(sequenceLookup);
    }

    private Rule rule(final List<CharSequence> input, final ReplaceInstruction replaceInstruction) {
        return new Rule(input, replaceInstruction);
    }

    private List<CharSequence> input(final String... terms) {
        return Arrays.asList(terms);
    }

    private ReplaceInstruction termsInstruction(final String... terms) {
        return new TermsReplaceInstruction(Arrays.asList(terms));
    }

    private static class Rule {
        final List<CharSequence> input;
        final ReplaceInstruction replaceInstruction;

        public Rule(List<CharSequence> input, ReplaceInstruction replaceInstruction) {
            this.input = input;
            this.replaceInstruction = replaceInstruction;
        }

        public List<CharSequence> getInput() {
            return input;
        }

        public ReplaceInstruction getReplaceInstruction() {
            return replaceInstruction;
        }
    }

}
