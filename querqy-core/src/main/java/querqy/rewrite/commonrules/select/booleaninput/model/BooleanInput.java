package querqy.rewrite.commonrules.select.booleaninput.model;

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BooleanInput that = (BooleanInput) o;
        return Objects.equals(booleanInputString, that.booleanInputString) &&
                Objects.equals(instructions, that.instructions);
    }

    @Override
    public int hashCode() {
        return cachedHashCode;
    }

    public static BooleanInputBuilder builder() {
        return new BooleanInputBuilder();
    }

    public static class BooleanInputBuilder {
        private final List<Reference> references = new ArrayList<>();
        private String booleanInputString;
        private Instructions instructions;
        private Predicate<boolean[]> predicate;

        private BooleanInputBuilder() {}

        public BooleanInputBuilder setBooleanInputString(final String booleanInputString) {
            this.booleanInputString = booleanInputString;
            return this;
        }

        public String getBooleanInputString() {
            return this.booleanInputString;
        }

        public int addLiteralAndCreateReferenceId(final BooleanInputLiteral literal) {
            final int referenceId = this.references.size();
            this.references.add(new Reference().setLiteral(literal).setReferenceId(referenceId));
            return referenceId;
        }

        public BooleanInputBuilder setPredicate(final Predicate<boolean[]> predicate) {
            this.predicate = predicate;
            return this;
        }

        public BooleanInputBuilder linkToInstructions(final Instructions instructions) {
            this.instructions = instructions;
            return this;
        }

        public List<BooleanInputLiteral> getLiterals() {
            return this.references.stream().map(Reference::getLiteral).collect(Collectors.toList());
        }

        public BooleanInput build() {
            final BooleanInput booleanInput =
                    new BooleanInput(this.booleanInputString, getLiterals(), this.predicate, this.instructions);

            this.references.forEach(reference -> reference.getLiteral().addReference(
                    reference.setBooleanInput(booleanInput)));

            return booleanInput;
        }
    }

    public static class Reference {
        private BooleanInput booleanInput;
        private BooleanInputLiteral literal;
        private int referenceId;

        public BooleanInput getBooleanInput() {
            return booleanInput;
        }

        public Reference setBooleanInput(BooleanInput booleanInput) {
            this.booleanInput = booleanInput;
            return this;
        }

        public BooleanInputLiteral getLiteral() {
            return literal;
        }

        public Reference setLiteral(BooleanInputLiteral literal) {
            this.literal = literal;
            return this;
        }

        public int getReferenceId() {
            return referenceId;
        }

        public Reference setReferenceId(int referenceId) {
            this.referenceId = referenceId;
            return this;
        }
    }
}
