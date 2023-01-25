package querqy.rewrite.lookup.preprocessing;

import java.util.Arrays;
import java.util.List;

public class PipelinePreprocessor implements LookupPreprocessor {

    private final List<LookupPreprocessor> preprocessors;

    private PipelinePreprocessor(final List<LookupPreprocessor> preprocessors) {
        this.preprocessors = preprocessors;
    }

    @Override
    public CharSequence process(final CharSequence charSequence) {

        CharSequence processedCharSequence = charSequence;
        for (final LookupPreprocessor preprocessor : preprocessors) {
            processedCharSequence = preprocessor.process(processedCharSequence);
        }

        return processedCharSequence;
    }

    public static PipelinePreprocessor of(final List<LookupPreprocessor> preprocessors) {
        return new PipelinePreprocessor(preprocessors);
    }

    public static PipelinePreprocessor of(final LookupPreprocessor... preprocessors) {
        return of(Arrays.asList(preprocessors));
    }


}
