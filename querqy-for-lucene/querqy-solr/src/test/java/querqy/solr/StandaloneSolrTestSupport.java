package querqy.solr;

import static java.nio.charset.StandardCharsets.UTF_8;
import static querqy.solr.QuerqyRewriterRequestHandler.ActionParam.*;

import org.apache.solr.common.util.ContentStreamBase;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.request.SolrRequestInfo;
import org.apache.solr.response.SolrQueryResponse;
import querqy.rewrite.commonrules.model.BoostInstruction;
import querqy.rewrite.commonrules.select.SelectionStrategyFactory;
import querqy.rewrite.lookup.preprocessing.LookupPreprocessorType;
import querqy.solr.rewriter.commonrules.CommonRulesConfigRequestBuilder;
import querqy.solr.rewriter.replace.ReplaceConfigRequestBuilder;
import querqy.solr.utils.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public interface StandaloneSolrTestSupport {

    static void withCommonRulesRewriter(final SolrCore core, final String rewriterId, final String rulesName,
                                        final String ... sinks) {
        withCommonRulesRewriter(core, rewriterId, rulesName, Collections.emptyMap(), null, sinks);
    }

    static void withCommonRulesRewriter(final SolrCore core, final String rewriterId, final String rulesName,
                                        final LookupPreprocessorType lookupPreprocessorType
                                        ) {
        withCommonRulesRewriter(core, rewriterId, rulesName, Collections.emptyMap(), null, lookupPreprocessorType);
    }

    static void withCommonRulesRewriter(final SolrCore core, final String rewriterId, final String rulesName,
                                        final BoostInstruction.BoostMethod boostMethod,
                                        final String ... sinks) {
        withCommonRulesRewriter(core, rewriterId, rulesName, Collections.emptyMap(), boostMethod, sinks);
    }

    static void withCommonRulesRewriter(final SolrCore core, final String rewriterId, final String rulesName,
                                        final Map<String, Class<? extends FactoryAdapter<SelectionStrategyFactory>>>
                                                ruleSelectionStrategies,
                                        final String ... sinks) {
        withCommonRulesRewriter(core, rewriterId, rulesName, ruleSelectionStrategies, null, sinks);
    }

    static void withCommonRulesRewriter(final SolrCore core, final String rewriterId, final String rulesName,
                                        final Map<String, Class<? extends FactoryAdapter<SelectionStrategyFactory>>>
                                                ruleSelectionStrategies,
                                        final BoostInstruction.BoostMethod boostMethod, final String ... sinks
                                        ) {
        withCommonRulesRewriter(core, rewriterId, rulesName, ruleSelectionStrategies, boostMethod, null, sinks);
    }

    static void withCommonRulesRewriter(final SolrCore core, final String rewriterId, final String rulesName,
                                        final Map<String, Class<? extends FactoryAdapter<SelectionStrategyFactory>>>
                                                ruleSelectionStrategies,
                                        final BoostInstruction.BoostMethod boostMethod,
                                        final LookupPreprocessorType lookupPreprocessorType,
                                        final String ... sinks
                                        ) {

        try {
            final CommonRulesConfigRequestBuilder builder = new CommonRulesConfigRequestBuilder()
                    .rules(StandaloneSolrTestSupport.class.getClassLoader().getResourceAsStream(rulesName))
                    .loggingToSinks(sinks);
            ruleSelectionStrategies.forEach(builder::ruleSelectionStrategy);
            if (lookupPreprocessorType != null) {
                builder.lookupPreprocessorType(lookupPreprocessorType);
            }

            if (boostMethod != null) {
                builder.boostMethod(boostMethod);
            }
            withCommonRulesRewriter(core, rewriterId, builder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void withCommonRulesRewriter(final SolrCore core, final String rewriterId,
                                        final CommonRulesConfigRequestBuilder builder) {
        SolrRequestHandler handler = core.getRequestHandler("/querqy/rewriter/" + rewriterId);

        final LocalSolrQueryRequest req = new LocalSolrQueryRequest(core, SAVE.params());
        req.setContentStreams(Collections.singletonList(new ContentStreamBase.StringStream(builder.buildJson())));
        req.getContext().put("httpMethod", "POST");

        final SolrQueryResponse rsp = new SolrQueryResponse();
        SolrRequestInfo.setRequestInfo(new SolrRequestInfo(req, rsp));
        try {
            core.execute(handler, req, rsp);
        } finally {
            SolrRequestInfo.clearRequestInfo();
            req.close();
        }
    }

    static void withReplaceRewriter(final SolrCore core, final String rewriterId, final String rulesName,
                                    final String... sinks) {
        try {
            final ReplaceConfigRequestBuilder builder = new ReplaceConfigRequestBuilder()
                    .rules(StandaloneSolrTestSupport.class.getClassLoader().getResourceAsStream(rulesName))
                    .loggingToSinks(sinks);
            withReplaceRewriter(core, rewriterId, builder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void withReplaceRewriter(final SolrCore core, final String rewriterId,
                                        final ReplaceConfigRequestBuilder builder) {
        SolrRequestHandler handler = core.getRequestHandler("/querqy/rewriter/" + rewriterId);

        final LocalSolrQueryRequest req = new LocalSolrQueryRequest(core, SAVE.params());
        req.setContentStreams(Collections.singletonList(new ContentStreamBase.StringStream(builder.buildJson())));
        req.getContext().put("httpMethod", "POST");

        final SolrQueryResponse rsp = new SolrQueryResponse();
        SolrRequestInfo.setRequestInfo(new SolrRequestInfo(req, rsp));
        try {
            core.execute(handler, req, rsp);
        } finally {
            SolrRequestInfo.clearRequestInfo();
            req.close();
        }
    }

    static void withRewriter(final SolrCore core, final String rewriterId,
                             final Class<? extends SolrRewriterFactoryAdapter> rewriterClass) {
        withRewriter(core, rewriterId, rewriterClass, Collections.emptyMap());
    }

    static void withRewriter(final SolrCore core, final String rewriterId,
                             final Class<? extends SolrRewriterFactoryAdapter> rewriterClass,
                                        final Map<String, Object> config) {

        SolrRequestHandler handler = core.getRequestHandler("/querqy/rewriter/" + rewriterId);

        final LocalSolrQueryRequest req = new LocalSolrQueryRequest(core, SAVE.params());

        final Map<String, Object> request = new HashMap<>();
        request.put("class", rewriterClass.getName());
        request.put("config", config);

        req.setContentStreams(Collections.singletonList(new ContentStreamBase.StringStream(JsonUtil.toJson(request))));
        req.getContext().put("httpMethod", "POST");

        final SolrQueryResponse rsp = new SolrQueryResponse();
        SolrRequestInfo.setRequestInfo(new SolrRequestInfo(req, rsp));
        try {
            core.execute( handler, req, rsp );
        } finally {
            SolrRequestInfo.clearRequestInfo();
            req.close();
        }
    }

    static void deleteRewriter(final SolrCore core, final String rewriterId) {

        SolrRequestHandler handler = core.getRequestHandler("/querqy/rewriter/" + rewriterId);

        final LocalSolrQueryRequest req = new LocalSolrQueryRequest(core, DELETE.params());

        req.getContext().put("httpMethod", "POST");

        final SolrQueryResponse rsp = new SolrQueryResponse();
        SolrRequestInfo.setRequestInfo(new SolrRequestInfo(req, rsp));
        try {
            core.execute( handler, req, rsp );
        } finally {
            SolrRequestInfo.clearRequestInfo();
            req.close();
        }
    }

    static String resourceToString(final String resourceName) throws IOException {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(StandaloneSolrTestSupport.class.getClassLoader()
                        .getResourceAsStream(resourceName)), UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
