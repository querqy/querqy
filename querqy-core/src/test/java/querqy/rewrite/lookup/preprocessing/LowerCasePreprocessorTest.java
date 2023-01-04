package querqy.rewrite.lookup.preprocessing;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LowerCasePreprocessorTest {

    @Test
    public void testThat_sharpSIsReplaced_atTheBeginning() {
        final LowerCasePreprocessor preprocessor = LowerCasePreprocessor.create();
        assertThat(preprocessor.process("AaB"))
                .isEqualTo("aab");
    }

}
