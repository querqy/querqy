package querqy.rewrite;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import querqy.model.EmptySearchEngineRequestAdapter;
import querqy.model.ExpandedQuery;
import querqy.rewrite.logging.ActionLog;
import querqy.rewrite.logging.RewriteChainLog;
import querqy.rewrite.logging.RewriterLog;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class RewriteChainTest {

    @Mock RewriterFactory rewriterFactory1;
    @Mock RewriterFactory rewriterFactory2;

    @Mock QueryRewriter queryRewriter1;
    @Mock QueryRewriter queryRewriter2;

    @Mock ExpandedQuery expandedQuery1;
    @Mock ExpandedQuery expandedQuery2;

    @Mock
    ActionLog actionLogging1;
    @Mock
    ActionLog actionLogging2;

    @Mock SearchEngineRequestAdapter searchEngineRequestAdapter;

    @Test
    public void testThat_rewriterAreCreatedAndExecutedInCorrectOrder_forExecutingInRewriteChain() {
        setupRewriterFactories();
        setupRewriter();

        final RewriteChain rewriteChain = new RewriteChain(List.of(rewriterFactory1, rewriterFactory2));
        rewriteChain.rewrite(expandedQuery1, new EmptySearchEngineRequestAdapter());

        final InOrder inOrder = Mockito.inOrder(queryRewriter1, queryRewriter2);

        verify(rewriterFactory1).createRewriter(any(), any());
        verify(rewriterFactory2).createRewriter(any(), any());

        inOrder.verify(queryRewriter1).rewrite(any(), any());
        inOrder.verify(queryRewriter2).rewrite(any(), any());
    }

    @Test
    public void testThat_expandedQueryFromLastRewriterOutputIsReturned_forExecutingInRewriteChain() {
        setupRewriterFactories();
        setupRewriter();

        final RewriteChain rewriteChain = new RewriteChain(List.of(rewriterFactory1, rewriterFactory2));
        final ExpandedQuery expandedQuery = rewriteChain.rewrite(expandedQuery1, new EmptySearchEngineRequestAdapter())
                .getExpandedQuery();

        assert expandedQuery == expandedQuery2;
    }

    @Test
    public void testThat_actionLoggingsAreCollected_forActivatedActionLoggingAndActivatedDetails() {
        setupRewriterFactories();
        setupRewriter();
        activateRewriteLogging(true);

        final RewriteChain rewriteChain = new RewriteChain(List.of(rewriterFactory1, rewriterFactory2));
        final Optional<RewriteChainLog> rewriteChainLogging = rewriteChain.rewrite(expandedQuery1, searchEngineRequestAdapter)
                .getRewriteLog();

        assertThat(rewriteChainLogging).isPresent();
        assertThat(rewriteChainLogging.get()).isEqualTo(
                RewriteChainLog.builder()
                        .add("1", List.of(actionLogging1))
                        .add("2", List.of(actionLogging2))
                        .build()
        );
    }

    @Test
    public void testThat_onlyIdsAreCollected_forActivatedActionLoggingAndDeactivatedDetails() {
        setupRewriterFactories();
        setupRewriter();
        activateRewriteLogging(false);

        final RewriteChain rewriteChain = new RewriteChain(List.of(rewriterFactory1, rewriterFactory2));
        final Optional<RewriteChainLog> rewriteChainLogging = rewriteChain.rewrite(expandedQuery1, searchEngineRequestAdapter)
                .getRewriteLog();

        assertThat(rewriteChainLogging).isPresent();
        assertThat(rewriteChainLogging.get()).isEqualTo(
                RewriteChainLog.builder()
                        .add("1", List.of())
                        .add("2", List.of())
                        .build()
        );
    }

    @Test
    public void testThat_actionLoggingsAreCollectedForCertainRewriters_forDeactivatedDetailsAndGivenRewriterIds() {
        setupRewriterFactories();
        setupRewriter();
        activateRewriteLogging(true, "1");

        final RewriteChain rewriteChain = new RewriteChain(List.of(rewriterFactory1, rewriterFactory2));
        final Optional<RewriteChainLog> rewriteChainLogging = rewriteChain.rewrite(expandedQuery1, searchEngineRequestAdapter)
                .getRewriteLog();

        assertThat(rewriteChainLogging).isPresent();
        assertThat(rewriteChainLogging.get()).isEqualTo(
                RewriteChainLog.builder()
                        .add("1", List.of(actionLogging1))
                        .build()
        );
    }

    @Test
    public void testThat_onlyIdsAreCollectedForCertainRewriters_forDeactivatedDetailsAndGivenRewriterIds() {
        setupRewriterFactories();
        setupRewriter();
        activateRewriteLogging(false, "1");

        final RewriteChain rewriteChain = new RewriteChain(List.of(rewriterFactory1, rewriterFactory2));
        final Optional<RewriteChainLog> rewriteChainLogging = rewriteChain.rewrite(expandedQuery1, searchEngineRequestAdapter)
                .getRewriteLog();

        assertThat(rewriteChainLogging).isPresent();
        assertThat(rewriteChainLogging.get()).isEqualTo(
                RewriteChainLog.builder()
                        .add("1", List.of())
                        .build()
        );
    }

    @Test
    public void testThat_noLoggingIsCollected_forActivatedLoggingButNoRewriting() {
        setupRewriterFactories();
        setupRewriter(false);
        activateRewriteLogging(false, "1");

        final RewriteChain rewriteChain = new RewriteChain(List.of(rewriterFactory1, rewriterFactory2));
        final Optional<RewriteChainLog> rewriteChainLogging = rewriteChain.rewrite(expandedQuery1, searchEngineRequestAdapter)
                .getRewriteLog();

        assertThat(rewriteChainLogging).isPresent();
        assertThat(rewriteChainLogging.get()).isEqualTo(RewriteChainLog.builder().build());
    }

    @Test
    public void testThat_creationOfRewriteChainFails_forDuplicateRewriterId() {
        setupRewriterFactories("1", "1");
        assertThatThrownBy(() -> new RewriteChain(List.of(rewriterFactory1, rewriterFactory2)));
    }

    @Test
    public void testThat_creationOfRewriteChainFails_forBlankRewriterId() {
        setupRewriterFactories("    ", "1");
        assertThatThrownBy(() -> new RewriteChain(List.of(rewriterFactory1, rewriterFactory2)));
    }

    private void setupRewriterFactories() {
        setupRewriterFactories("1", "2");
    }

    private void setupRewriterFactories(final String id1, final String id2) {
        when(rewriterFactory1.getRewriterId()).thenReturn(id1);
        when(rewriterFactory2.getRewriterId()).thenReturn(id2);

        when(rewriterFactory1.createRewriter(any(), any())).thenReturn(queryRewriter1);
        when(rewriterFactory2.createRewriter(any(), any())).thenReturn(queryRewriter2);
    }

    private void setupRewriter() {
        setupRewriter(true);
    }

    private void setupRewriter(final boolean hasAppliedRewriting) {
        when(queryRewriter1.rewrite(any(), any())).thenReturn(
                RewriterOutput.builder()
                        .expandedQuery(expandedQuery1)
                        .rewriterLog(createRewriteLogging(hasAppliedRewriting, actionLogging1))
                        .build()
        );
        when(queryRewriter2.rewrite(any(), any())).thenReturn(
                RewriterOutput.builder()
                        .expandedQuery(expandedQuery2)
                        .rewriterLog(createRewriteLogging(hasAppliedRewriting, actionLogging2))
                        .build()
        );
    }

    private void activateRewriteLogging(final boolean hasDetails, final String... includedIds) {
        when(searchEngineRequestAdapter.getRewriteLoggingConfig())
                .thenReturn(RewriteLoggingConfig.builder()
                        .isActive(true)
                        .hasDetails(hasDetails)
                        .includedRewriters(Set.of(includedIds))
                        .build());
    }

    private RewriterLog createRewriteLogging(final boolean hasAppliedRewriting, final ActionLog actionLogging) {
        return RewriterLog.builder()
                .hasAppliedRewriting(hasAppliedRewriting)
                .addActionLogs(actionLogging)
                .build();
    }
}
