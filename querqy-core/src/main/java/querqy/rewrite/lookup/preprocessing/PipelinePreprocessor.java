package querqy.rewrite.lookup.preprocessing;

import java.util.Arrays;
import java.util.List;

public class PipelinePreprocessor implements Preprocessor {

    private final List<Preprocessor> preprocessors;

    private PipelinePreprocessor(final List<Preprocessor> preprocessors) {
        this.preprocessors = preprocessors;
    }

    @Override
    public CharSequence process(final CharSequence charSequence) {

        CharSequence processedCharSequence = charSequence;
        for (final Preprocessor preprocessor : preprocessors) {
            processedCharSequence = preprocessor.process(processedCharSequence);
        }

        return processedCharSequence;
    }

    public static PipelinePreprocessor of(final List<Preprocessor> preprocessors) {
        return new PipelinePreprocessor(preprocessors);
    }

    public static PipelinePreprocessor of(final Preprocessor... preprocessors) {
        return of(Arrays.asList(preprocessors));
    }


}
