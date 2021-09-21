package querqy.rewrite.rules.rule.skeleton;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import querqy.rewrite.rules.RuleParseException;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TextRuleSkeletonParserTest {

    @Mock LineParser lineParser;
    @Captor ArgumentCaptor<String> linesPassedToLineParser;

    @Test
    public void testThat_exceptionThrownByLineParserShowsCorrectLineNumber_forGivenLineNumberMappings() {
        final String errorLine = "error";

        doThrow(new RuleParseException("Test Exception")).when(lineParser).parse(errorLine);

        final Map<Integer, Integer> lineNumberMappings = new HashMap<>();
        lineNumberMappings.put(2, 4);
        lineNumberMappings.put(3, 5);
        lineNumberMappings.put(4, 6);

        final TextRuleSkeletonParser parser = createParser(
                content(
                        "a", " ", errorLine, "b"
                ),
                lineNumberMappings
        );

        final IOException ioException = assertThrows(IOException.class, parser::parse);
        assertThat(ioException.getMessage()).contains("5");
    }

    @Test
    public void testThat_linesArePassedToLineParser_ifNotBlank() {
        assertThatLinesArePassedToLineParser(
                content(" a ", "", " ", "b  ", " \t "),
                "a", "b");
    }

    @Test
    public void testThat_commentIsStripped_atNumberSignWithinLine() {
        assertThatLinesArePassedToLineParser(
                content("abc # def"),
                "abc");
    }

    @Test
    public void testThat_lineIsNotPassedToLineParser_forOnlyContainingComment() {
        assertThatLinesArePassedToLineParser(
                content("# def"));
    }

    @Test
    public void testThat_commentsAreNotStripped_forEscapedNumberSign() {
        assertThatLinesArePassedToLineParser(
                content("abc \\# def"),
                "abc \\# def");
    }

    @Test
    public void testThat_commentIsStripped_atNumberSignAfterEscapedNumberSign() {
        assertThatLinesArePassedToLineParser(
                content("abc \\## def"),
                "abc \\#");
    }

    @Test
    public void testThat_commentIsStripped_atFirstNumberSign() {
        assertThatLinesArePassedToLineParser(
                content("abc # d # ef"),
                "abc");
    }

    private Reader content(final String... lines) {
        return new StringReader(String.join("\n", lines));
    }

    private void assertThatLinesArePassedToLineParser(final Reader input, final String... expectedLines) {
        parse(input);

        verify(lineParser, times(expectedLines.length)).parse(linesPassedToLineParser.capture());

        assertThat(linesPassedToLineParser.getAllValues()).containsExactly(expectedLines);
    }

    private void parse(final Reader input) {
        try {
            createParser(input).parse();
        } catch (IOException ignored) {}
    }

    private TextRuleSkeletonParser createParser(final Reader input) {
        return TextRuleSkeletonParser.builder()
                .rulesContentReader(input)
                .lineParser(lineParser)
                .build();

    }

    private TextRuleSkeletonParser createParser(final Reader input, Map<Integer, Integer> lineNumberMappings) {
        return TextRuleSkeletonParser.builder()
                .rulesContentReader(input)
                .lineParser(lineParser)
                .lineNumberMappings(lineNumberMappings)
                .build();

    }
}
