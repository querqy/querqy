package querqy.rewrite;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import querqy.model.EmptySearchEngineRequestAdapter;
import querqy.model.rewriting.RewriterOutput;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static querqy.model.convert.builder.BooleanQueryBuilder.bq;
import static querqy.model.convert.builder.ExpandedQueryBuilder.expanded;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class RewriteChainTest {

    @Mock RewriterFactory rewriterFactory1;
    @Mock RewriterFactory rewriterFactory2;
    @Mock QueryRewriter queryRewriter1;
    @Mock QueryRewriter queryRewriter2;

    @Test
    public void testThat_rewriterAreCreatedAndExecuted_forExecutingRewriterChain() {
        setupRewriterFactories();
        setupRewriter();

        final RewriteChain rewriteChain = new RewriteChain(List.of(rewriterFactory1, rewriterFactory2));
        rewriteChain.rewrite(expanded(bq("a")).build(), new EmptySearchEngineRequestAdapter());

        verify(rewriterFactory1).createRewriter(any(), any());
        verify(rewriterFactory2).createRewriter(any(), any());
        verify(queryRewriter1).rewrite(any(), any());
        verify(queryRewriter2).rewrite(any(), any());
    }

    private void setupRewriterFactories() {
        when(rewriterFactory1.getRewriterId()).thenReturn("1");
        when(rewriterFactory2.getRewriterId()).thenReturn("2");

        when(rewriterFactory1.createRewriter(any(), any())).thenReturn(queryRewriter1);
        when(rewriterFactory2.createRewriter(any(), any())).thenReturn(queryRewriter2);
    }

    private void setupRewriter() {
        when(queryRewriter1.rewrite(any(), any())).thenReturn(RewriterOutput.builder().build());
        when(queryRewriter2.rewrite(any(), any())).thenReturn(RewriterOutput.builder().build());
    }

}
