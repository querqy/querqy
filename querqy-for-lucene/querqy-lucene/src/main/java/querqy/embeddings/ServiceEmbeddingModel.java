package querqy.embeddings;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import querqy.utils.JsonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * An example {@link EmbeddingModel} that gets the embeddings for a text value from an external (HTTP) service.
 * The class makes some assumptions on the external service:
 * <ul>
 *     <li>The service accepts HTTP POSTed JSON objects.</li>
 *     <li>The service returns the embeddings in a HTTP response body using a JSON numbers array.</li>
 * </ul>
 * Other options can be configured passing config values to the {@link #configure(Map, EmbeddingCache)} method.
 * <p>
 * The request's key/values must be configured by setting a <code>request_template</code> config option. For a service
 * that expects the text in a <code>sentence</code> property and which supports a vector normalization flag
 * <code>norm</code> which you want to set to <code>true</code> to correspond with your indexed vectors you would
 * configure <code>request_template</code> to <code>Map.of("sentence", "{{text}}", "norm", true);</code>
 * </p>
 * <p>
 * The location of the numbers array in the service's response body must be configured using JSONPath in the
 * <code>response_path</code> option. If the service returns the embedding in a nested array
 * <code>{ "embedding": [[0.4, 1, 2.4]] }</code> the <code>response_path</code> would need to be set to
 * <code>"$.embedding[0]"</code>
 * </p>
 */
public class ServiceEmbeddingModel implements EmbeddingModel {

    private static final String CONTENT_TYPE_JSON = "application/json";

    private URL url;

    private Map<String, Object> requestTemplate;

    private JsonPath responsePath;

    private EmbeddingCache<String> embeddingsCache;

    private static final Configuration jsonPathConfiguration = Configuration.builder()
            .jsonProvider(new JacksonJsonProvider())
            .mappingProvider(new JacksonMappingProvider())
            .build();

    @Override
    public void configure(final Map<String, Object> config, final EmbeddingCache<String> embeddingsCache) {
        try {
            this.url = new URL((String) config.get("url"));
            if (Boolean.TRUE.equals(config.get("test_on_configure"))) {
                getEmbedding("hello world");
            }
            this.requestTemplate = (Map<String, Object>) config.get("request_template");
            this.responsePath = JsonPath.compile((String) config.get("response_path"));
            this.embeddingsCache = embeddingsCache;

        } catch (final MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Embedding getEmbedding(final String text) {
        try {
            final String json = buildJsonRequestBodyString(requestTemplate, text);
            final String cacheKey = url.toString() + "|" + json;
            Embedding embedding = embeddingsCache.getEmbedding(cacheKey);
            if (embedding == null) {
                InputStream response = doRequest(json);
                embedding = readEmbedding(response, responsePath);
                embeddingsCache.putEmbedding(cacheKey, embedding);
            }
            return embedding;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected InputStream doRequest(String requestBody) throws IOException {
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", CONTENT_TYPE_JSON);
        con.setRequestProperty("Accept", CONTENT_TYPE_JSON);
        con.setDoOutput(true);
        try (final OutputStream os = con.getOutputStream()) {
            final byte[] input = requestBody.getBytes(UTF_8);
            os.write(input, 0, input.length);
        }
        return con.getInputStream();
    }

    protected static String buildJsonRequestBodyString(final Map<String, Object> requestTemplate, final String text) {
        final Map<String, Object> parameters = Map.of("text", text);

        // Sorting the keys so that we get a stable key for the cache lookup.
        final TreeMap<String, Object> requestBody = new TreeMap<>();
        requestTemplate.forEach((requestTemplateKey, requestTemplateValue) -> {
            Object value = requestTemplateValue instanceof String
                    // could be a template value
                    ? replaceParameterValue((String) requestTemplateValue, parameters)
                    : requestTemplateValue;
            requestBody.put(requestTemplateKey, value);
        });
        return JsonUtil.toJson(requestBody);
    }

    protected static Embedding readEmbedding(final InputStream responseStream, final JsonPath jsonPath) {
        Object parsed = JsonUtil.readJson(responseStream, Object.class);
        List<Number> embedding = jsonPath.read(parsed, jsonPathConfiguration);
        return Embedding.of(embedding);
    }

    // Replaces well-known "{{value}}" placeholders. Returns input verbatim if it's not surrounded by curly braces or
    // there is no value for this key in replacements.
    private static Object replaceParameterValue(final String value, final Map<String, Object> replacements) {
        int len = value.length();
        if (len > 4 && value.charAt(0) == '{' && value.charAt(1) == '{'
                && value.charAt(len - 2) == '}' && value.charAt(len - 1) == '}') {
            String placeholder = value.substring(2, len - 2);
            Object replacement = replacements.get(placeholder);
            // for unknown parameters we just return the {{placeholder}} as it came in
            return replacement == null ? value : replacement;
        } else {
            return value;
        }
    }

}
