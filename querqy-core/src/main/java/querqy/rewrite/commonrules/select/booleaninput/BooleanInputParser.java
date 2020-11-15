package querqy.rewrite.commonrules.select.booleaninput;

import querqy.rewrite.commonrules.RuleParseException;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInput;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInput.BooleanInputBuilder;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInputElement;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInputElement.Type;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInputLiteral;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BooleanInputParser {

    /*
        TODO:
          Implement a check that the input must have at least one clause that is not a 'MUST NOT' clause.
          Currently, the parser will not fail and create a BooleanInput e. g. for an input like 'NOT a'. However,
          this input will never be applied at query time, as a BooleanInput is only evaluated if at least one literal
          matches (which means in this case that the query must contain 'a' in order that the BooleanInput is
          evaluated). Furthermore, a BooleanInput like 'NOT a OR NOT b' will be treated as an exclusive OR as at least
          one literal must be contained in the query to activate the evaluation of the BooleanInput. The other literal
          must not be included in the query in order that the input evaluation will return 'true' for the BooleanInput.
     */

    private final Map<List<String>, BooleanInputLiteral> literalRegister = new HashMap<>();
    private final Map<String, String> mappingToUnescapeElements = Arrays.stream(Type.values())
            .map(Type::getName)
            .collect(Collectors.toMap(typeName -> "\\" + typeName, typeName -> typeName));

    public BooleanInputBuilder parseBooleanInput(final BooleanInputString booleanInputString) throws RuleParseException {
        return this.parseBooleanInput(booleanInputString.booleanInputString);
    }

    public BooleanInputBuilder parseBooleanInput(final String booleanInputString) throws RuleParseException {
        final List<BooleanInputElement> elements = parseInputStringToElements(booleanInputString);

        validateBooleanInput(elements, booleanInputString);

        final BooleanInputBuilder booleanInputBuilder = BooleanInput.builder();
        booleanInputBuilder.setBooleanInputString(booleanInputString);

        final ToIntFunction<List<String>> createReferenceIdFunction = literalTerms -> {
            final BooleanInputLiteral literal = literalRegister.computeIfAbsent(
                    literalTerms, key -> new BooleanInputLiteral(literalTerms));

            return booleanInputBuilder.addLiteralAndCreateReferenceId(literal);
        };

        final PredicateCreator predicateCreator = new PredicateCreator(elements, createReferenceIdFunction);

        booleanInputBuilder.setPredicate(predicateCreator.build());

        return booleanInputBuilder;
    }

    protected List<BooleanInputElement> parseInputStringToElements(final String booleanInputString) {
        return Arrays.stream(booleanInputString.split("\\s+"))
                .flatMap(this::separateParenthesesFromElements)
                .map(element -> new BooleanInputElement(
                        unescapeElement(element),
                        Type.getType(element.toUpperCase())))
                .collect(Collectors.toList());
    }

    protected String unescapeElement(final String element) {
        return this.mappingToUnescapeElements.getOrDefault(element, element)
                .replace("\\(", "(")
                .replace("\\)", ")");
    }

    protected Stream<String> separateParenthesesFromElements(String element) {
        return Arrays.stream(
                element.split("(?<=(?<!\\\\)\\()|(?=(?<!\\\\)\\()|(?<=(?<!\\\\)\\))|(?=(?<!\\\\)\\))"));
    }

    public void validateBooleanInput(final List<BooleanInputElement> elements, final String booleanInput) throws RuleParseException {
        final String baseMessage = String.format("Cannot parse boolean expression \'%s\'. ", booleanInput);

        if (elements.isEmpty()) {
            throw new RuleParseException(baseMessage + "Expression is empty.");
        }

        BooleanInputElement.Type priorType = null;
        int numberOpenGroups = 0;

        for (final BooleanInputElement element : elements) {

            switch (element.type) {
                case LEFT_PARENTHESIS:
                    if (priorType == Type.TERM || priorType == Type.RIGHT_PARENTHESIS) {
                        throw new RuleParseException(
                                baseMessage + "A left parenthesis opening a boolean group must be defined at the " +
                                        "beginning of a statement or prepended by an operator, e. g. " +
                                        "(term OR term) AND (term OR term) AND NOT (term OR term).");
                    }
                    numberOpenGroups++;
                    break;

                case RIGHT_PARENTHESIS:
                    if (priorType != Type.TERM && priorType != Type.RIGHT_PARENTHESIS) {
                        throw new RuleParseException(
                                baseMessage + "A right parenthesis closing a boolean group must be prepended " +
                                        "by a term, e. g. (term OR term)");
                    }

                    numberOpenGroups--;

                    if (numberOpenGroups < 0) {
                        throw new RuleParseException(baseMessage + "The boolean expression misses at least one " +
                                "left parenthesis to open a group.");
                    }

                    break;

                case OR:
                case AND:
                    if (priorType != Type.TERM && priorType != Type.RIGHT_PARENTHESIS) {
                        throw new RuleParseException(
                                baseMessage + "The operators AND and OR are expected to be prepended by a " +
                                        "term or a right parenthesis, e. g. (term OR term) AND term");
                    }
                    break;

                case NOT:
                    if (priorType == Type.NOT || priorType == Type.RIGHT_PARENTHESIS || priorType == Type.TERM) {
                        throw new RuleParseException(
                                baseMessage + "The operator NOT must be defined at the beginning of a statement or prepended " +
                                        "by a left parenthesis, AND or OR, e. g. NOT term AND NOT (NOT term)");
                    }
                    break;

                case TERM:
                    if (priorType == Type.RIGHT_PARENTHESIS) {
                        throw new RuleParseException(
                                baseMessage + "A term must not be prepended by a right parenthesis closing a group.");
                    }
            }

            priorType = element.type;
        }

        final BooleanInputElement lastElement = elements.get(elements.size() - 1);
        if (lastElement.type != Type.TERM && lastElement.type != Type.RIGHT_PARENTHESIS) {
            throw new RuleParseException(
                    baseMessage + "The last element in a boolean expression must be a term or a right parenthesis " +
                            "closing a group.");
        }

        if (numberOpenGroups > 0) {
            throw new RuleParseException("The boolean expression \'%s\' misses at least one right parenthesis to " +
                    "close a group.");
        }

        if (numberOpenGroups < 0) {
            throw new RuleParseException("The boolean expression \'%s\' misses at least one left parenthesis to open " +
                    "a group.");
        }

    }

    public Map<List<String>, BooleanInputLiteral> getLiteralRegister() {
        return this.literalRegister;
    }

    public static class BooleanInputString {
        public final String booleanInputString;
        public BooleanInputString(final String booleanInputString) {
            this.booleanInputString = booleanInputString;
        }
    }


}
