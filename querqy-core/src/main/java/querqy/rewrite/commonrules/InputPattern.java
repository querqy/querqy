package querqy.rewrite.commonrules;

import querqy.rewrite.commonrules.model.Input;
import querqy.rewrite.commonrules.model.Instructions;
import querqy.rewrite.commonrules.model.RulesCollectionBuilder;
import querqy.rewrite.commonrules.model.Term;
import querqy.rewrite.commonrules.select.booleaninput.BooleanInputParser;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInputElement;

import java.util.List;
import java.util.Optional;

public abstract class InputPattern {

    protected final String inputString;

    public InputPattern(final String inputString) {
        this.inputString = inputString;
    }
    public abstract Optional<List<Term>> getInputTerms();

    public abstract void applyInstructions(final Instructions instructions, final RulesCollectionBuilder builder) throws RuleParseException;

    public String getIdPrefix() {
        return inputString;
    }

    public static Object fromString(final String inputString, final BooleanInputParser booleanInputParser) {

        if (booleanInputParser == null) {
            return parseSimpleInput(inputString);
        }

        final List<BooleanInputElement> elements = booleanInputParser.parseInputStringToElements(inputString);
        // not all instructions can have boolean input -> use boolean input only if we have boolean predicates.
        if (elements.stream().noneMatch(element -> (element.type == BooleanInputElement.Type.OR)
                || (element.type == BooleanInputElement.Type.AND)
                || (element.type == BooleanInputElement.Type.NOT))) {
            return parseSimpleInput(inputString);
        } else {
            return new BooleanInputPattern(elements, booleanInputParser, inputString);
        }

    }

    public static Object parseSimpleInput(final String inputString) {
        final Object parseResult = LineParser.parseInput(inputString);
        return (parseResult instanceof Input) ? new SimpleInputPattern((Input) parseResult, inputString) : parseResult;
    }

    public static class SimpleInputPattern extends InputPattern {

        private final Input input;

        public SimpleInputPattern(final Input input, final String inputString) {
            super(inputString);
            if (input == null) {
                throw new IllegalArgumentException("Input must not be null");
            }
            this.input = input;
        }

        public Optional<List<Term>> getInputTerms() {
            return Optional.of(input.getInputTerms());
        }

        public void applyInstructions(final Instructions instructions, final RulesCollectionBuilder builder) {
            builder.addRule(input, instructions);
        }

    }

    public static class BooleanInputPattern extends InputPattern {

        private final List<BooleanInputElement> elements;
        private final BooleanInputParser booleanInputParser;

        public BooleanInputPattern(final List<BooleanInputElement> elements,
                                   final BooleanInputParser booleanInputParser, final String inputString) {
            super(inputString);
            this.elements = elements;
            this.booleanInputParser = booleanInputParser;

        }

        public Optional<List<Term>> getInputTerms() {
            return Optional.empty();
        }

        public void applyInstructions(final Instructions instructions, final RulesCollectionBuilder builder)
                throws RuleParseException {
            booleanInputParser.createInputBuilder(elements, inputString).withInstructions(instructions).build();
        }


    }
}
