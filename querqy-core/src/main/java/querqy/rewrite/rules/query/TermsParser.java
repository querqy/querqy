package querqy.rewrite.rules.query;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import querqy.rewrite.commonrules.model.PrefixTerm;
import querqy.rewrite.commonrules.model.Term;
import querqy.rewrite.rules.RuleParseException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static querqy.rewrite.commonrules.EscapeUtil.endsWithSpecialChar;
import static querqy.rewrite.commonrules.EscapeUtil.unescape;

@RequiredArgsConstructor(staticName = "of", access = AccessLevel.PRIVATE)
public class TermsParser {

    private static final char WILDCARD = '*';

    private static final String PATTERN_TO_SPLIT_TERMS = "\\s+";
    private static final String FIELD_SEPARATOR = ":";
    private static final String MULTI_FIELD_DEFINITION_OPENER = "{";
    private static final String MULTI_FIELD_DEFINITION_CLOSER = "}";
    private static final String MULTI_FIELD_DEFINITION_SEPARATOR = ",";

    private final List<Term> terms = new ArrayList<>();

    private final String value;

    private TermDefinition termDef;

    public static TermsParser createPrototype() {
        return of(null);
    }

    public TermsParser with(final String value) {
        return TermsParser.of(value);
    }

    public List<Term> parse() {
        assertThatThisIsNotPrototype();

        final String[] rawTerms = value.trim().split(PATTERN_TO_SPLIT_TERMS, -1);
        for (final String rawTerm : rawTerms) {
            termDef = TermDefinition.create().value(rawTerm);
            parseTerm();
        }

        return terms;
    }

    private void assertThatThisIsNotPrototype() {
        if (value == null) {
            throw new UnsupportedOperationException("Methods cannot be used on prototype");
        }
    }

    private void parseTerm() {
        checkForWildcard();
        parseFields();
        termDef.parseTermValue();
        addTerm();
    }

    private void checkForWildcard() {
        if (endsWithSpecialChar(termDef.value, WILDCARD)) {
            termDef.hasWildcard(true);
        }
    }

    private void parseFields() {
        final int positionOfFieldSeparator = termDef.value.indexOf(FIELD_SEPARATOR);

        termDef.termStartsAt(positionOfFieldSeparator + 1);

        if (positionOfFieldSeparator > 0) {
            parseFieldDefinition();
        }
    }

    private void parseFieldDefinition() {
        final String fieldDefinition = termDef.value.substring(0, termDef.termStartsAt - 1);

        if (isMultiFieldDefinition(fieldDefinition)) {
            parseMultiFieldDefinition(fieldDefinition);

        } else {
            termDef.fields(Collections.singletonList(fieldDefinition));
        }
    }

    protected boolean isMultiFieldDefinition(final String fieldDefinition) {
        if (fieldDefinition.startsWith(MULTI_FIELD_DEFINITION_OPENER) &&
                fieldDefinition.endsWith(MULTI_FIELD_DEFINITION_CLOSER)) {
            return true;

        } else if (fieldDefinition.startsWith(MULTI_FIELD_DEFINITION_OPENER) ||
                fieldDefinition.endsWith(MULTI_FIELD_DEFINITION_CLOSER)) {

            throw new RuleParseException(
                    String.format("Error parsing value %s. " +
                            "The definition of multiple fields for a term must be opened and closed by curly brackets",
                            value));

        } else {
            return false;
        }

    }

    private void parseMultiFieldDefinition(final String fieldDefinition) {
        final String definitionWithoutBrackets = fieldDefinition.substring(1, fieldDefinition.length() - 1);

        final List<String> fields = Arrays.asList(
                definitionWithoutBrackets.split(MULTI_FIELD_DEFINITION_SEPARATOR));

        termDef.fields(fields);
    }

    private void addTerm() {
        final Term term = createTerm();
        validateTerm(term);
        terms.add(term);
    }

    private Term createTerm() {
        if (termDef.hasWildcard) {
            return new PrefixTerm(termDef.value.toCharArray(), 0, termDef.getLength(), termDef.fields());

        } else {
            return new Term(termDef.value.toCharArray(), 0, termDef.getLength(), termDef.fields());
        }
    }

    private void validateTerm(final Term term) {
        if (term.getValue().length == 0) {
            throw new RuleParseException(String.format("Error parsing %s: Empty values are not allowed", value));
        }

        if (term.getPlaceHolders() != null && term.getPlaceHolders().size() > 1) {
            throw new RuleParseException("Max. wild card reference is 1: " + value);
        }
    }

    @NoArgsConstructor(staticName = "create")
    @Getter
    @Setter
    @Accessors(fluent = true)
    private static class TermDefinition {
        private String value;

        private boolean hasWildcard;
        private int termStartsAt = 0;

        private List<String> fields;

        public void parseTermValue() {
            value = value.substring(termStartsAt);
            removeWildcard();
            unescapeTerm();
            trim();
        }

        private void removeWildcard() {
            if (hasWildcard) {
                value = value.substring(0, value.length() - 1);
            }
        }

        private void unescapeTerm() {
            value = unescape(value);
        }

        private void trim() {
            value = value.trim();
        }

        public int getLength() {
            return value.length();
        }
    }
}
