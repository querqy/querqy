package querqy.model;

import lombok.Getter;
import querqy.ComparableCharSequence;
import querqy.CompoundCharSequence;
import querqy.rewrite.commonrules.LineParser;
import querqy.rewrite.commonrules.RuleParseException;
import querqy.rewrite.commonrules.model.Instructions;
import querqy.rewrite.commonrules.model.RulesCollectionBuilder;
import querqy.rewrite.commonrules.model.Term;
import querqy.rewrite.commonrules.select.booleaninput.BooleanInputParser;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInputElement;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public abstract class Input {

    protected final String inputString;

    public Input(final String inputString) {
        this.inputString = inputString;
    }
    public abstract List<Term> getInputTerms();

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
            return new BooleanInput(elements, booleanInputParser, inputString);
        }

    }

    public static Object parseSimpleInput(final String inputString) {
        final Object parseResult = LineParser.parseInput(inputString);
        return (parseResult instanceof SimpleInput) ? (SimpleInput) parseResult : parseResult;
    }

    /**
     * A simple input that accepts terms, boundary markers and a wildcard but no boolean expressions
     */
    public static class SimpleInput extends Input {

        @Getter
        protected final List<Term> inputTerms;
        @Getter
        protected final boolean requiresLeftBoundary;
        @Getter
        protected final boolean requiresRightBoundary;

        /**
         * Same as {@link #SimpleInput(List, boolean, boolean, String)} with both boundaries not required
         * (set to false)
         * @param inputTerms The sequence of terms to match
         * @param inputString The raw input string
         */
        public SimpleInput(final List<Term> inputTerms, final String inputString) {
            this(inputTerms, false, false, inputString);
        }

        /**
         *
         * @param inputTerms The sequence of terms to match
         * @param requiresLeftBoundary true iff the first input term must be the first term in the query
         * @param requiresRightBoundary true iff the last input term must be the last term in the query
         * @param inputString The raw input string
         */
        public SimpleInput(final List<Term> inputTerms, final boolean requiresLeftBoundary,
                           final boolean requiresRightBoundary, final String inputString) {
            super(inputString);
            if ((inputTerms == null) || inputTerms.isEmpty()) {
                if (!(requiresLeftBoundary && requiresRightBoundary)) {
                    throw new IllegalArgumentException("input required");
                }
                this.inputTerms = Collections.emptyList();
            } else {
                this.inputTerms = inputTerms;
            }
            this.requiresLeftBoundary = requiresLeftBoundary;
            this.requiresRightBoundary = requiresRightBoundary;

        }

        public void applyInstructions(final Instructions instructions, final RulesCollectionBuilder builder) {
            builder.addRule(this, instructions);
        }

        public List<ComparableCharSequence> getInputSequences(final boolean lowerCaseValues) {

            if (inputTerms.size() == 1) {
                return inputTerms.get(0).getCharSequences(lowerCaseValues);
            }

            LinkedList<List<ComparableCharSequence>> slots = new LinkedList<>();

            for (final Term inputTerm : inputTerms) {
                slots.add(inputTerm.getCharSequences(lowerCaseValues));
            }

            final List<ComparableCharSequence> seqs = new LinkedList<>();
            collectTails(new LinkedList<>(), slots, seqs);
            return seqs;

        }

        void collectTails(final List<ComparableCharSequence> prefix, List<List<ComparableCharSequence>> tailSlots,
                          final List<ComparableCharSequence> result) {
            if (tailSlots.size() == 1) {
                for (final ComparableCharSequence sequence : tailSlots.get(0)) {
                    final List<ComparableCharSequence> combined = new LinkedList<>(prefix);
                    combined.add(sequence);
                    result.add(new CompoundCharSequence(" ", combined));
                }
            } else {

                final List<List<ComparableCharSequence>> newTail = tailSlots.subList(1, tailSlots.size());
                for (final ComparableCharSequence sequence : tailSlots.get(0)) {
                    final List<ComparableCharSequence> newPrefix = new LinkedList<>(prefix);
                    newPrefix.add(sequence);
                    collectTails(newPrefix, newTail, result);
                }
            }
        }

    }

    public static class BooleanInput extends Input {

        private final List<BooleanInputElement> elements;
        private final BooleanInputParser booleanInputParser;

        public BooleanInput(final List<BooleanInputElement> elements, final BooleanInputParser booleanInputParser,
                            final String inputString) {
            super(inputString);
            this.elements = elements;
            this.booleanInputParser = booleanInputParser;

        }

        public List<Term> getInputTerms() {
            return Collections.emptyList();
        }

        public void applyInstructions(final Instructions instructions, final RulesCollectionBuilder builder)
                throws RuleParseException {
            booleanInputParser.createInputBuilder(elements, inputString).withInstructions(instructions).build();
        }


    }
}
