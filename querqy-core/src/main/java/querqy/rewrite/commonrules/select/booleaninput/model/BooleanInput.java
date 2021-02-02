package querqy.rewrite.commonrules.select.booleaninput.model;

import querqy.CharSequenceUtil;
import querqy.rewrite.commonrules.model.Instructions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BooleanInput {

    private final String booleanInputString;

    private final List<BooleanInputLiteral> literals;
    private final Predicate<boolean[]> predicate;
    private final Instructions instructions;

    private final int cachedHashCode;

    private BooleanInput(
            final String booleanInputString,
            final List<BooleanInputLiteral> literals,
            final Predicate<boolean[]> predicate,
            final Instructions instructions) {

        // booleanInputString works like it is part of an ID -> let's not allow null so that users do think about it
        if (booleanInputString == null) {
            throw new IllegalArgumentException("booleanInputString must not be null");
        }
        this.booleanInputString = booleanInputString;
        this.literals = literals;
        this.predicate = predicate;
        this.instructions = instructions;

        this.cachedHashCode = Objects.hash(this.booleanInputString, this.instructions);
    }

    public BooleanInputEvaluator createEvaluator() {
        return new BooleanInputEvaluator(this.literals.size(), this.predicate, this.instructions);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final BooleanInput that = (BooleanInput) o;
        return CharSequenceUtil.equals(booleanInputString, that.booleanInputString) &&
                Objects.equals(instructions, that.instructions);
    }

    @Override
    public int hashCode() {
        return cachedHashCode;
    }

    public static BooleanInputBuilder builder(final String booleanInputString) {
        return new BooleanInputBuilder(booleanInputString);
    }

    public static class BooleanInputBuilder {
        private final List<Reference> references = new ArrayList<>();
        private String booleanInputString;
        private Instructions instructions;
        private Predicate<boolean[]> predicate;

        private BooleanInputBuilder(final String booleanInputString) {
            if (booleanInputString == null) {
                throw new IllegalArgumentException("booleanInputString must not be null");
            }
            this.booleanInputString = booleanInputString;
        }

        public String getBooleanInputString() {
            return this.booleanInputString;
        }

        public int addLiteralAndCreateReferenceId(final BooleanInputLiteral literal) {
            final int referenceId = this.references.size();
            this.references.add(new Reference(literal, referenceId));
            return referenceId;
        }

        public BooleanInputBuilder withPredicate(final Predicate<boolean[]> predicate) {
            this.predicate = predicate;
            return this;
        }

        public BooleanInputBuilder withInstructions(final Instructions instructions) {
            this.instructions = instructions;
            return this;
        }

        public BooleanInput build() {
            final BooleanInput booleanInput = new BooleanInput(booleanInputString, getLiterals(), predicate,
                    instructions);

            references.forEach(reference -> reference.setBooleanInput(booleanInput));

            return booleanInput;
        }

        private List<BooleanInputLiteral> getLiterals() {
            return references.stream().map(Reference::getLiteral).collect(Collectors.toList());
        }
    }

}
