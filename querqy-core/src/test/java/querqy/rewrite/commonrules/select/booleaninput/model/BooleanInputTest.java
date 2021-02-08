package querqy.rewrite.commonrules.select.booleaninput.model;

import org.junit.Test;

import querqy.rewrite.commonrules.AbstractCommonRulesTest;
import querqy.rewrite.commonrules.select.booleaninput.BooleanInputQueryHandler;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BooleanInputTest extends AbstractCommonRulesTest {

    @Test
    public void testEvaluationOfSingleBooleanInput() {
        final List<BooleanInputLiteral> literals = literals(4);
        booleanInput(literals);

        final BooleanInputQueryHandler handler = new BooleanInputQueryHandler();

        handler.notifyLiteral(literals.get(0));
        assertThat(handler.evaluate()).isEmpty();

        handler.notifyLiteral(literals.get(1));
        assertThat(handler.evaluate()).isEmpty();

        handler.notifyLiteral(literals.get(2));
        assertThat(handler.evaluate()).isEmpty();

        handler.notifyLiteral(literals.get(3));
        assertThat(handler.evaluate()).isNotEmpty();
    }

    @Test
    public void testEvaluationOfMultipleBooleanInput() {

        final List<BooleanInputLiteral> literals = literals(6);

        booleanInput(literals.subList(0, 3));
        booleanInput(literals.subList(1, 4));
        booleanInput(literals.subList(4, 6));

        final BooleanInputQueryHandler handler = new BooleanInputQueryHandler();

        handler.notifyLiteral(literals.get(0));
        assertThat(handler.evaluate()).isEmpty();

        handler.notifyLiteral(literals.get(1));
        assertThat(handler.evaluate()).isEmpty();

        handler.notifyLiteral(literals.get(2));
        assertThat(handler.evaluate()).hasSize(1);

        handler.notifyLiteral(literals.get(3));
        assertThat(handler.evaluate()).hasSize(2);

        handler.notifyLiteral(literals.get(4));
        assertThat(handler.evaluate()).hasSize(2);

        handler.notifyLiteral(literals.get(5));
        assertThat(handler.evaluate()).hasSize(3);

    }

}
