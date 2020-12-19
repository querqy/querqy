package querqy.rewrite;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class QuerqyTemplateEngineTest {

    @Test(expected = TemplateParseException.class)
    public void testErrorForReferenceOnMultilineTemplate() throws TemplateParseException, IOException {
        new QuerqyTemplateEngine(resource("templating/error/embedded-reference-on-multiline.txt"));
    }

    @Test(expected = TemplateParseException.class)
    public void testErrorForReferenceOnMultilineTemplate2() throws TemplateParseException, IOException {
        new QuerqyTemplateEngine(resource("templating/error/embedded-reference-on-multiline-2.txt"));
    }

    @Test(expected = TemplateParseException.class)
    public void testErrorForMissingTemplateBody() throws TemplateParseException, IOException {
        new QuerqyTemplateEngine(resource("templating/error/missing-body-for-template.txt"));
    }

    @Test(expected = TemplateParseException.class)
    public void testErrorForMissingTemplateDefinition() throws TemplateParseException, IOException {
        new QuerqyTemplateEngine(resource("templating/error/missing-template-definition.txt"));
    }

    @Test(expected = TemplateParseException.class)
    public void testErrorForNonMatchingParams() throws TemplateParseException, IOException {
        new QuerqyTemplateEngine(resource("templating/error/non-matching-params.txt"));
    }

    @Test(expected = TemplateParseException.class)
    public void testErrorForNonMatchingParams2() throws TemplateParseException, IOException {
        new QuerqyTemplateEngine(resource("templating/error/non-matching-params-2.txt"));
    }

    @Test
    public void testParseParameters() throws TemplateParseException, IOException {
        QuerqyTemplateEngine querqyTemplateEngine = new QuerqyTemplateEngine(new StringReader(""));

        Map<String, String> parameters;

        parameters = querqyTemplateEngine.parseParameters(" a = 1 ");
        assertThat(parameters).containsExactly(
                new AbstractMap.SimpleEntry<>("a", "1"));

        parameters = querqyTemplateEngine.parseParameters("a = 1 || b=2");
        assertThat(parameters).containsExactly(
                new AbstractMap.SimpleEntry<>("a", "1"),
                new AbstractMap.SimpleEntry<>("b", "2"));
    }

    @Test
    public void testRendering() throws IOException, TemplateParseException {
        QuerqyTemplateEngine querqyTemplateEngine = new QuerqyTemplateEngine(
                resource("templating/input-rendering.txt"));

        assertThat(list(querqyTemplateEngine.renderedRules.reader))
                .isEqualTo(list(resource("templating/expected-rendering.txt")));
    }

    @Test
    public void testLineNumberMapping() throws IOException, TemplateParseException {
        List<String> linesOfInput = list(resource("templating/input-line-number-mapping.txt"));
        Map<Integer, String> numberedLinesOfInput = numberedLines(linesOfInput);

        QuerqyTemplateEngine querqyTemplateEngine = new QuerqyTemplateEngine(
                resource("templating/input-line-number-mapping.txt"));

        List<String> linesOfOutput = list(querqyTemplateEngine.renderedRules.reader);
        Map<Integer, String> numberedLinesOfOutput = numberedLines(linesOfOutput);

        Map<Integer, Integer> lineNumberMapping = querqyTemplateEngine.renderedRules.lineNumberMapping;

        assertThat(numberedLinesOfOutput.get(3)).isEqualTo(numberedLinesOfInput.get(lineNumberMapping.get(3)));
        assertThat(numberedLinesOfOutput.get(7)).isEqualTo(numberedLinesOfInput.get(lineNumberMapping.get(7)));
        assertThat(numberedLinesOfOutput.get(12)).isEqualTo(numberedLinesOfInput.get(lineNumberMapping.get(12)));
        assertThat(numberedLinesOfOutput.get(19)).isEqualTo(numberedLinesOfInput.get(lineNumberMapping.get(19)));
    }

    private Reader resource(String resourceName) {
        return new InputStreamReader(getClass().getClassLoader().getResourceAsStream(resourceName));
    }

    private List<String> list(Reader reader) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(reader);

        List<String> lines = new ArrayList<>();

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            lines.add(line);
        }

        return lines;
    }

    private Map<Integer, String> numberedLines(List<String> lines) {
        Map<Integer, String> numberedLines = new HashMap<>();

        for (String line : lines) {
            numberedLines.put(numberedLines.size() + 1, line);
        }

        return numberedLines;
    }

}
