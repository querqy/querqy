package querqy.rewrite.commonrules.select.booleaninput;

import querqy.rewrite.commonrules.model.Instructions;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInput;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInputEvaluator;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInputLiteral;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class BooleanInputQueryHandler {

    private Map<BooleanInput, BooleanInputEvaluator> evaluatorMap = null;

    public void notifyLiteral(final BooleanInputLiteral literal) {
        if (this.evaluatorMap == null) {
            this.evaluatorMap = new HashMap<>();
        }

        literal.getReferences().forEach(
                reference -> evaluatorMap
                        .computeIfAbsent(reference.getBooleanInput(),
                                key -> reference.getBooleanInput().createEvaluator())
                        .notify(reference.getReferenceId()));
    }

    public Stream<Instructions> evaluate() {
        if (evaluatorMap == null) {
            return Stream.empty();
        } else {
            return evaluatorMap.values().stream()
                    .map(BooleanInputEvaluator::evaluate)
                    .filter(Optional::isPresent)
                    .map(Optional::get);
        }
    }
}
