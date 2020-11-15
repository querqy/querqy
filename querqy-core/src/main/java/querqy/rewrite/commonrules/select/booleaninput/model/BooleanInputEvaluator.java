package querqy.rewrite.commonrules.select.booleaninput.model;

import querqy.rewrite.commonrules.model.Instructions;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

public class BooleanInputEvaluator {

    private final boolean[] booleans;
    private final Predicate<boolean[]> predicate;
    private final Instructions instructions;

    public BooleanInputEvaluator(final int numberOfLiterals,
                                 final Predicate<boolean[]> predicate,
                                 final Instructions instructions) {

        booleans = new boolean[numberOfLiterals];
        Arrays.fill(booleans, false);
        this.predicate = predicate;
        this.instructions = instructions;
    }

    public void notify(final int referenceId) {
        this.booleans[referenceId] = true;
    }

    public Optional<Instructions> evaluate() {
        return predicate.test(this.booleans) ? Optional.of(this.instructions) : Optional.empty();
    }
}
