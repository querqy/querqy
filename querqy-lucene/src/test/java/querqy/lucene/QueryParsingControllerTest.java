/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2026 Querqy Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package querqy.lucene;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import querqy.json.JsonProvider;
import querqy.json.JsonSmartJsonProvider;
import querqy.json.ToyJsonProvider;
import querqy.lucene.LuceneSearchEngineRequestAdapter.SyntaxException;
import querqy.lucene.rewrite.infologging.InfoLoggingContext;
import querqy.model.ExpandedQuery;
import querqy.model.MatchAllQuery;
import querqy.rewrite.RewriteChain;
import querqy.rewrite.RewriteChainOutput;
import querqy.rewrite.logging.ActionLog;
import querqy.rewrite.logging.RewriteChainLog;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exercises {@link QueryParsingController}'s rewrite-chain debug/logging round-trip through
 * {@link JsonProvider} (the private {@code toGenericList}/{@code toGenericMap} methods) via its real public
 * API ({@code process()}/{@code getDebugInfo()}) - this path had no test coverage at all before a security
 * review of the JsonProvider SPI change (#1158) flagged the gap.
 * <p>
 * Only {@link JsonSmartJsonProvider} can actually serialize the {@link RewriteChainLog}/{@link ActionLog}
 * object graph involved (it falls back to reflective bean serialization for types it doesn't recognize);
 * {@link ToyJsonProvider} is deliberately minimal (Map/List/String/Boolean/Number only) and throws for this
 * path - that real, pre-existing asymmetry is documented below rather than hidden.
 */
public class QueryParsingControllerTest {

    @Test
    public void testThat_rewriteChainLoggingAndDebugInfo_roundTripThroughJsonSmartJsonProvider()
            throws SyntaxException {

        final InfoLoggingContext infoLoggingContext = mock(InfoLoggingContext.class);
        final RewriteChainLog rewriteChainLog = singleActionRewriteChainLog();

        final QueryParsingController controller = new QueryParsingController(
                requestAdapter(new JsonSmartJsonProvider(), infoLoggingContext, rewriteChainLog));
        controller.process();

        verify(infoLoggingContext).setRewriterId("my_rewriter");
        @SuppressWarnings("unchecked")
        final List<Object> loggedActions = (List<Object>) capturedLogArgument(infoLoggingContext);
        assertEquals(1, loggedActions.size());
        assertEquals("replaced term", asMap(loggedActions.get(0)).get("message"));

        final Map<String, Object> debugInfo = controller.getDebugInfo();
        final List<Object> rewriteChain = asList(asMap(debugInfo.get("rewrite")).get("rewriteChain"));
        assertEquals(1, rewriteChain.size());
        final Map<String, Object> rewriteEntry = asMap(rewriteChain.get(0));
        assertEquals("my_rewriter", rewriteEntry.get("rewriterId"));
        assertEquals("replaced term", asMap(asList(rewriteEntry.get("actions")).get(0)).get("message"));
    }

    @Test
    public void testThat_toyJsonProvider_cannotSerializeRewriteChainLogging() {
        final InfoLoggingContext infoLoggingContext = mock(InfoLoggingContext.class);
        final RewriteChainLog rewriteChainLog = singleActionRewriteChainLog();

        final QueryParsingController controller = new QueryParsingController(
                requestAdapter(new ToyJsonProvider(), infoLoggingContext, rewriteChainLog));

        assertThrows(UnsupportedOperationException.class, controller::process);
    }

    private static RewriteChainLog singleActionRewriteChainLog() {
        return RewriteChainLog.builder()
                .add("my_rewriter", List.of(ActionLog.builder().message("replaced term").build()))
                .build();
    }

    private static LuceneSearchEngineRequestAdapter requestAdapter(
            final JsonProvider jsonProvider, final InfoLoggingContext infoLoggingContext,
            final RewriteChainLog rewriteChainLog) {

        final LuceneSearchEngineRequestAdapter requestAdapter = mock(LuceneSearchEngineRequestAdapter.class);
        when(requestAdapter.getJsonProvider()).thenReturn(jsonProvider);
        when(requestAdapter.getQueryString()).thenReturn("*");
        when(requestAdapter.isMatchAllQuery("*")).thenReturn(true);
        when(requestAdapter.getQueryAnalyzer()).thenReturn(new KeywordAnalyzer());
        when(requestAdapter.isDebugQuery()).thenReturn(true);
        when(requestAdapter.getInfoLoggingContext()).thenReturn(Optional.of(infoLoggingContext));

        final RewriteChain rewriteChain = mock(RewriteChain.class);
        when(requestAdapter.getRewriteChain()).thenReturn(rewriteChain);
        when(rewriteChain.rewrite(any(), any())).thenReturn(
                RewriteChainOutput.builder()
                        .expandedQuery(new ExpandedQuery(new MatchAllQuery()))
                        .rewriteLog(rewriteChainLog)
                        .build());

        return requestAdapter;
    }

    private static Object capturedLogArgument(final InfoLoggingContext infoLoggingContext) {
        final ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(infoLoggingContext).log(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(final Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(final Object value) {
        return (List<Object>) value;
    }
}
