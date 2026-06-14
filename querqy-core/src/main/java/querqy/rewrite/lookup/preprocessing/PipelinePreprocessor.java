/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2023 Querqy Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
