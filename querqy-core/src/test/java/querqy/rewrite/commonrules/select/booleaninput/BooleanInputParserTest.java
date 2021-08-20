package querqy.rewrite.commonrules.select.booleaninput;

import org.junit.Test;
import querqy.rewrite.commonrules.RuleParseException;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInputElement;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BooleanInputParserTest {

    @Test
    public void testSeparateParentheses() {
        BooleanInputParser parser = new BooleanInputParser();

        List<String> elements;

        elements = parser.separateParenthesesFromElements("a(b)c").collect(Collectors.toList());
        assertThat(elements).isEqualTo(list("a", "(", "b", ")", "c"));

        elements = parser.separateParenthesesFromElements("(a)").collect(Collectors.toList());
        assertThat(elements).isEqualTo(list("(", "a", ")"));

        elements = parser.separateParenthesesFromElements("a\\(b\\)c").collect(Collectors.toList());
        assertThat(elements).isEqualTo(list("a\\(b\\)c"));

        elements = parser.separateParenthesesFromElements("\\(a\\)").collect(Collectors.toList());
        assertThat(elements).isEqualTo(list("\\(a\\)"));
    }

    @Test
    public void testValidateInput() {
        BooleanInputParser parser = new BooleanInputParser();

        assertThatCode(() -> parser.validateBooleanInput(elements("a b"), ""))
                .doesNotThrowAnyException();

        assertThatCode(() -> parser.validateBooleanInput(elements("a OR b"), ""))
                .doesNotThrowAnyException();

        assertThatCode(() -> parser.validateBooleanInput(elements("( a b OR c )"), ""))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> parser.validateBooleanInput(elements("AND a OR b"), ""))
                .isInstanceOf(RuleParseException.class);

        assertThatThrownBy(() -> parser.validateBooleanInput(elements("a OR b AND"), ""))
                .isInstanceOf(RuleParseException.class);

        assertThatThrownBy(() -> parser.validateBooleanInput(elements("a OR b NOT"), ""))
                .isInstanceOf(RuleParseException.class);

        assertThatThrownBy(() -> parser.validateBooleanInput(elements("a NOT b"), ""))
                .isInstanceOf(RuleParseException.class);

        assertThatThrownBy(() -> parser.validateBooleanInput(elements("a ( b )"), ""))
                .isInstanceOf(RuleParseException.class);

        assertThatThrownBy(() -> parser.validateBooleanInput(elements("a AND ( )"), ""))
                .isInstanceOf(RuleParseException.class);

        assertThatThrownBy(() -> parser.validateBooleanInput(elements("a ) AND ( b"), ""))
                .isInstanceOf(RuleParseException.class);

        assertThatThrownBy(() -> parser.validateBooleanInput(elements("a AND b )"), ""))
                .isInstanceOf(RuleParseException.class);

        assertThatThrownBy(() -> parser.validateBooleanInput(elements("( a AND b"), ""))
                .isInstanceOf(RuleParseException.class);

        assertThatThrownBy(() -> parser.validateBooleanInput(elements("( a AND ) b"), ""))
                .isInstanceOf(RuleParseException.class);

        assertThatThrownBy(() -> parser.validateBooleanInput(elements("a ( AND b )"), ""))
                .isInstanceOf(RuleParseException.class);
    }

    @Test
    public void testParseInputStringToElements() {
        BooleanInputParser parser = new BooleanInputParser();

        assertThat(parser.parseInputStringToElements("a AND b"))
                .isEqualTo(list(term("a"), and(), term("b")));

        assertThat(parser.parseInputStringToElements("a \\AND b"))
                .isEqualTo(list(term("a"), term("AND"), term("b")));

        assertThat(parser.parseInputStringToElements("a AND (b)"))
                .isEqualTo(list(term("a"), and(), leftP(), term("b"), rightP()));

        assertThat(parser.parseInputStringToElements("a AND \\(b\\)"))
                .isEqualTo(list(term("a"), and(), term("(b)")));
    }

    @Test
    public void handlesVerbatimWildcardQuoteAndHashAsLiterals() {
        BooleanInputParser parser = new BooleanInputParser();

        assertThat(parser.parseInputStringToElements("12\" AND 1*1 OR #9"))
                .isEqualTo(list(term("12\""), and(), term("1*1"), or(), term("#9")));
    }

    @Test
    public void unescapesWildcardQuoteAndHashAsLiterals() {
        BooleanInputParser parser = new BooleanInputParser();

        assertThat(parser.parseInputStringToElements("12\\\" AND 1\\*1 OR \\#9"))
                .isEqualTo(list(term("12\""), and(), term("1*1"), or(), term("#9")));
    }

    private BooleanInputElement term(String term) {
        return new BooleanInputElement(term, BooleanInputElement.Type.TERM);
    }

    private BooleanInputElement and() {
        return new BooleanInputElement("AND", BooleanInputElement.Type.AND);
    }

    private BooleanInputElement or() {
        return new BooleanInputElement("OR", BooleanInputElement.Type.OR);
    }

    private BooleanInputElement leftP() {
        return new BooleanInputElement("(", BooleanInputElement.Type.LEFT_PARENTHESIS);
    }

    private BooleanInputElement rightP() {
        return new BooleanInputElement(")", BooleanInputElement.Type.RIGHT_PARENTHESIS);
    }

    private List<BooleanInputElement> elements(String booleanInput) {
        return Arrays.stream(booleanInput.split("\\s+"))
                .map(element -> new BooleanInputElement(
                        element, BooleanInputElement.Type.getType(element.toUpperCase())))
                .collect(Collectors.toList());
    }

    private static List<String> list(final String... elems) {
        return Arrays.asList(elems);
    }

    private static List<BooleanInputElement> list(final BooleanInputElement... elems) {
        return Arrays.asList(elems);
    }


}
