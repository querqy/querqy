package querqy.rewrite;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class QuerqyTemplateEngine {

    private final Map<String, Template> templates = new HashMap<>();
    public final RenderedRules renderedRules;

    public QuerqyTemplateEngine(final Reader rules) throws TemplateParseException, IOException {
        final RenderedRules rulesWithoutTemplates = extractTemplatesFromRules(rules);

        this.renderedRules = templates.isEmpty()
                ? rulesWithoutTemplates
                : renderUsingTemplates(rulesWithoutTemplates.reader, rulesWithoutTemplates.lineNumberMapping);
    }

    private static final byte[] LINE_BREAK = "\n".getBytes();

    private RenderedRules renderUsingTemplates(final Reader reader, final Map<Integer, Integer> originalLineNumberMapping)
            throws TemplateParseException, IOException {

        final Pattern identifyReferenceOnTemplate = Pattern.compile("<<\\s*(\\w+)(?:\\s*|:((?:(?!>>).)*))>>");

        try (final BufferedReader bufferedReader = new BufferedReader(reader);
             final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            final Map<Integer, Integer> lineNumberMapping = new HashMap<>();
            int lineNumber = 0;

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                lineNumber++;

                final Matcher templateReferenceMatcher = identifyReferenceOnTemplate.matcher(line);

                final List<MatchResult> templateReferenceMatchResults = new ArrayList<>();
                while (templateReferenceMatcher.find()) {
                    templateReferenceMatchResults.add(templateReferenceMatcher.toMatchResult());
                }

                if (templateReferenceMatchResults.isEmpty()) {
                    writeLine(outputStream, line, originalLineNumberMapping.get(lineNumber), lineNumberMapping);

                } else if (templateReferenceMatchResults.size() == 1) {
                    final MatchResult matchResult = templateReferenceMatchResults.get(0);

                    final Template template = getTemplateForReferenceName(matchResult.group(1).trim());

                    final Map<String, String> referenceParameters = parseParameters(matchResult.group(2));
                    validateReferenceParameters(referenceParameters, template);

                    if (template.body.size() == 1) {
                        final String renderedTemplate = renderTemplateLine(referenceParameters, template.body.get(0));
                        writeLine(outputStream, renderLine(line, renderedTemplate, matchResult),
                                originalLineNumberMapping.get(lineNumber), lineNumberMapping);

                    } else {
                        final boolean referenceComprisesFullLine = (line.substring(0, matchResult.start()) +
                                line.substring(matchResult.end())).trim().isEmpty();

                        if (!referenceComprisesFullLine) {
                            throw new TemplateParseException("References for multi-line templates must comprise " +
                                    "a full line and must not be embedded in a statement.");
                        }

                        writeLines(outputStream, renderTemplateLines(referenceParameters, template.body),
                                originalLineNumberMapping.get(lineNumber), lineNumberMapping);
                    }
                } else {
                    for (int i = templateReferenceMatchResults.size() - 1; i >= 0; i--) {
                        final MatchResult matchResult = templateReferenceMatchResults.get(i);

                        final Template template = getTemplateForReferenceName(matchResult.group(1).trim());

                        if (template.body.size() > 1) {
                            throw new TemplateParseException("References for multi-line templates must comprise " +
                                    "a full line and must not be nested in a statement.");
                        }

                        final Map<String, String> referenceParameters = parseParameters(matchResult.group(2));
                        validateReferenceParameters(referenceParameters, template);

                        final String renderedTemplate = renderTemplateLine(referenceParameters, template.body.get(0));

                        line = renderLine(line, renderedTemplate, matchResult);

                    }
                    writeLine(outputStream, line, originalLineNumberMapping.get(lineNumber), lineNumberMapping);
                }
            }
            return new RenderedRules(new InputStreamReader(new ByteArrayInputStream(outputStream.toByteArray()), UTF_8),
                    lineNumberMapping);
        }
    }

    private void writeLines(final OutputStream outputStream,
                            final List<String> lines,
                            final int originalLineNumber,
                            final Map<Integer, Integer> lineNumberMapping) throws IOException {
        for (final String line : lines) {
            writeLine(outputStream, line, originalLineNumber, lineNumberMapping);
        }
    }

    private void writeLine(final OutputStream outputStream,
                           final String line,
                           final int originalLineNumber,
                           final Map<Integer, Integer> lineNumberMapping) throws IOException {
        outputStream.write(line.getBytes(UTF_8));
        outputStream.write(LINE_BREAK);
        lineNumberMapping.put(lineNumberMapping.size() + 1, originalLineNumber);
    }

    private List<String> renderTemplateLines(final Map<String, String> referenceParameters, final List<String> templateLines) {
        return templateLines.stream()
                .map(templateLine -> renderTemplateLine(referenceParameters, templateLine))
                .collect(Collectors.toList());
    }

    private String renderTemplateLine(final Map<String, String> referenceParameters, String templateLine) {
        for (final Map.Entry<String, String> param : referenceParameters.entrySet()) {
            templateLine = templateLine.replace("$" + param.getKey(), param.getValue());
        }

        return templateLine;
    }

    private String renderLine(final String line, final String renderedTemplate, final MatchResult matchResult) {
        return line.substring(0, matchResult.start()) + renderedTemplate + line.substring(matchResult.end());
    }

    private Template getTemplateForReferenceName(final String referenceName) throws TemplateParseException {
        final Template template = templates.get(referenceName);

        if (template == null) {
            throw new TemplateParseException(String.format("No template definition found for template " +
                    "reference \'%s\'", referenceName));
        }

        return template;
    }

    private void validateReferenceParameters(final Map<String, String> referenceParameters, final Template template)
            throws TemplateParseException {

        if (!(template.parameters.containsAll(referenceParameters.keySet()) &&
                template.parameters.size() == referenceParameters.size())) {
            throw new TemplateParseException(String.format("Parameters of template reference " +
                    "\'%s\' are defined incorrectly or do not match to parameters defined in the " +
                    "template", template.name));
        }
    }

    protected Map<String, String> parseParameters(final String parameterDefinition)
            throws TemplateParseException {

        if (isBlank(parameterDefinition)) {
            return Collections.emptyMap();
        }

        final List<String> rawParams = Arrays.stream(parameterDefinition.split("\\|\\|"))
                .map(String::trim)
                .filter(rawParam -> !rawParam.isEmpty())
                .collect(Collectors.toList());

        Map<String, String> params = new HashMap<>();

        for (final String rawParam : rawParams) {
            final String[] paramSplit = rawParam.split("=", 2);

            if (paramSplit.length != 2) {
                throw new TemplateParseException(String.format("Error parsing parameter definition \'%s\'. " +
                        "Parameters for template references must be defined by a key-value " +
                        "pair, separated by the character '='. Multiple parameters are separated " +
                        "by double pipe, e. g. << template_name: param1 = value1 || param2 = value2 >>", rawParam));
            }

            params.put(paramSplit[0].trim(), paramSplit[1].trim());
        }

        return params;
    }

    private RenderedRules extractTemplatesFromRules(final Reader reader) throws TemplateParseException, IOException {

        final Pattern identifyTemplateHeader = Pattern.compile("^\\s*def\\s+(\\w+)\\s*\\(([\\w, ]*)\\):\\s*$");

        final Map<Integer, Integer> lineNumberMapping = new HashMap<>();
        int lineNumber = 0;

        try (final BufferedReader bufferedReader = new BufferedReader(reader);
             final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                lineNumber++;

                final Matcher templateHeaderCandidate = identifyTemplateHeader.matcher(line);
                if (templateHeaderCandidate.matches()) {
                    final String templateName = templateHeaderCandidate.group(1);
                    final List<String> parameters = Arrays.stream(templateHeaderCandidate.group(2).split(","))
                            .map(String::trim)
                            .filter(parameter -> !parameter.isEmpty())
                            .collect(Collectors.toList());

                    final List<String> templateBody = new ArrayList<>();

                    while (!isBlank((line = bufferedReader.readLine()))) {
                        lineNumber++;
                        templateBody.add(line);
                    }
                    lineNumber++;

                    if (templateBody.isEmpty()) {
                        throw new TemplateParseException(String.format("Body of template \'%s\' is empty", templateName));
                    }

                    this.templates.put(templateName, new Template(templateName, parameters, templateBody));
                } else {
                    outputStream.write(line.getBytes(UTF_8));
                    outputStream.write(LINE_BREAK);
                    lineNumberMapping.put(lineNumberMapping.size() + 1, lineNumber);
                }
            }

            return new RenderedRules(new InputStreamReader(new ByteArrayInputStream(outputStream.toByteArray()), UTF_8),
                    lineNumberMapping);
        }
    }

    public class Template {
        public final String name;
        public final List<String> parameters;
        public final List<String> body;

        public Template(String name, List<String> parameters, List<String> body) {
            this.name = name;
            this.parameters = parameters;
            this.body = body;
        }

        @Override
        public String toString() {
            return "Template{" +
                    "name='" + name + '\'' +
                    ", parameters=" + parameters +
                    ", body=" + body +
                    '}';
        }
    }

    public class RenderedRules {
        public final Reader reader;
        public final Map<Integer, Integer> lineNumberMapping;

        public RenderedRules(Reader reader, Map<Integer, Integer> lineNumberMapping) {
            this.reader = reader;
            this.lineNumberMapping = lineNumberMapping;
        }
    }

    public boolean isBlank(final String str) {
        return str == null || str.trim().isEmpty();
    }
}
