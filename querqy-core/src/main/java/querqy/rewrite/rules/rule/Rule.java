package querqy.rewrite.rules.rule;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import querqy.model.Input;
import querqy.rewrite.commonrules.model.InstructionsSupplier;

@RequiredArgsConstructor(staticName = "of")
@Getter
@EqualsAndHashCode
@ToString(includeFieldNames = false)
public class Rule {

    private final Input.SimpleInput input;
    private final InstructionsSupplier instructionsSupplier;

}
