package querqy.rewrite.rules.rule.skeleton;

import lombok.Builder;
import lombok.Singular;
import querqy.rewrite.rules.RuleParseException;
import querqy.rewrite.rules.RuleSkeletonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TextRuleSkeletonParser implements RuleSkeletonParser {

    private static final Pattern INLINE_COMMENTS_PATTERN = Pattern.compile("(?<!(?<!\\\\)\\\\)#");

    private final Reader rulesContentReader;
    private final LineParser lineParser;
    private final Map<Integer, Integer> lineNumberMappings;

    private String line;
    private int currentLineNumber = 0;

    @Builder
    protected TextRuleSkeletonParser(final Reader rulesContentReader,
                                     final LineParser lineParser,
                                     @Singular final Map<Integer, Integer> lineNumberMappings) {
        this.rulesContentReader = rulesContentReader;
        this.lineParser = lineParser;
        this.lineNumberMappings = lineNumberMappings;
    }

    @Override
    public List<RuleSkeleton> parse() throws IOException {
        try {
            final List<String> lines = ruleContentAsLines();
            parseLines(lines);
            return lineParser.finish();

        } catch (final RuleParseException e) {
            throw new IOException("An error occurred parsing line " + getCurrentLineNumber(), e);
        }
    }

    private List<String> ruleContentAsLines() throws IOException {
        try (final BufferedReader bufferedContentReader = new BufferedReader(rulesContentReader)) {
            return bufferedContentReader
                    .lines()
                    .collect(Collectors.toList());
        }
    }

    protected void parseLines(final List<String> lines) {
        for (final String newLine : lines) {
            line = newLine;
            incrementCurrentLineNumber();
            parseLine();
        }
    }

    private void incrementCurrentLineNumber() {
        currentLineNumber++;
    }

    private void parseLine() {
        stripLine();
        if (!line.isEmpty()) {
            lineParser.parse(line);
        }
    }

    private void stripLine() {
        stripComments();
        trim();
    }

    private void stripComments() {
        final Matcher matcher = INLINE_COMMENTS_PATTERN.matcher(line);

        if (matcher.find()) {
            line = line.substring(0, matcher.start());
        }
    }

    private void trim() {
        line = line.trim();
    }

    private int getCurrentLineNumber() {
        return lineNumberMappings.getOrDefault(currentLineNumber, currentLineNumber);
    }
}
