package querqy.embeddings;

import com.jayway.jsonpath.JsonPath;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static querqy.embeddings.ServiceEmbeddingModel.buildJsonRequestBodyString;
import static querqy.embeddings.ServiceEmbeddingModel.readEmbedding;

public class ServiceEmbeddingModelTest {

    @Test
    public void testJsonRequestBodyBuilding() {
        assertThat(buildJsonRequestBodyString(Map.of("text", "{{text}}"), "query"),
                is("{\"text\":\"query\"}"));
        assertThat(buildJsonRequestBodyString(Map.of("q", "{{text}}", "norm", true), "query"),
                is("{\"norm\":true,\"q\":\"query\"}"));
        assertThat(buildJsonRequestBodyString(Map.of(), "query"),
                is("{}"));
        assertThat(buildJsonRequestBodyString(Map.of("param", "{{placeholder}}"), "query"),
                is("{\"param\":\"{{placeholder}}\"}"));
    }

    @Test
    public void testJsonPathEmbeddingsParsing() {
        assertThat(readEmbedding(response("{\"embedding\": [0.1, 0.2, 0.3]}"), JsonPath.compile("$.embedding")).asVector(),
            is(new float[] { 0.1f, 0.2f, 0.3f }));
        assertThat(readEmbedding(response("{\"embedding\": [[0.1, 0.2, 0.3]]}"), JsonPath.compile("$.embedding[0]")).asVector(),
            is(new float[] { 0.1f, 0.2f, 0.3f }));
        assertThat(readEmbedding(response("[1, 2, 3]}"), JsonPath.compile("$.*")).asVector(),
            is(new float[] { 1f, 2f, 3f }));
    }

    private static InputStream response(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }
}
