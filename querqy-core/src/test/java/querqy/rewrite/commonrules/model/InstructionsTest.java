package querqy.rewrite.commonrules.model;

import org.junit.Test;
import querqy.rewrite.commonrules.AbstractCommonRulesTest;

import static org.assertj.core.api.Assertions.assertThat;

public class InstructionsTest extends AbstractCommonRulesTest {

    @Test
    public void testThatDeleteInstructionsAreAlwaysTheLast() {

        Instructions instructions = new Instructions(
                0,
                0,
                list(
                        synonym(""),
                        delete(""),
                        synonym(""),
                        synonym(""),
                        delete("")
                )
        );

        assertThat(instructions).hasSize(5);

        assertThat(instructions.pollFirst()).isInstanceOf(SynonymInstruction.class);
        assertThat(instructions.pollFirst()).isInstanceOf(SynonymInstruction.class);
        assertThat(instructions.pollFirst()).isInstanceOf(SynonymInstruction.class);
        assertThat(instructions.pollFirst()).isInstanceOf(DeleteInstruction.class);
        assertThat(instructions.pollFirst()).isInstanceOf(DeleteInstruction.class);
    }
}
