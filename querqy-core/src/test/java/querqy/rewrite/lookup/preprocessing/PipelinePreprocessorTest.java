package querqy.rewrite.lookup.preprocessing;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class PipelinePreprocessorTest {

    @Mock
    LookupPreprocessor preprocessor1;
    @Mock
    LookupPreprocessor preprocessor2;

    @Test
    public void testThat_preprocessorsAreAppliedInOrder_forTwoGivenPreprocessors() {
        when(preprocessor1.process("a")).thenReturn("b");
        when(preprocessor2.process("b")).thenReturn("c");

        final LookupPreprocessor pipeline = PipelinePreprocessor.of(preprocessor1, preprocessor2);
        assertThat(pipeline.process("a")).isEqualTo("c");
    }
}
