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

public class LookupPreprocessorFactory {

    private static final LookupPreprocessor IDENTITY_PREPROCESSOR = charSequence -> charSequence;

    private static final LookupPreprocessor LOWERCASE_PREPROCESSOR = LowerCasePreprocessor.create();

    public static LookupPreprocessor identity() {
        return IDENTITY_PREPROCESSOR;
    }

    public static LookupPreprocessor lowercase() {
        return LOWERCASE_PREPROCESSOR;
    }

    public static LookupPreprocessor fromType(final LookupPreprocessorType type) {

        switch (type) {
            case NONE:
                return IDENTITY_PREPROCESSOR;

            case GERMAN:
                return GermanPreprocessorOnDemandClassHolder.GERMAN_PREPROCESSOR;

            case LOWERCASE:
                return LOWERCASE_PREPROCESSOR;

            default:
                throw new IllegalArgumentException("Preprocessor of type " + " is currently not supported");
        }

    }

    // This wrapper makes sure the maps in the GermanNounNormalizer are loaded lazily (and in a synchronized way)
    private static class GermanPreprocessorOnDemandClassHolder {
        private static final LookupPreprocessor GERMAN_PREPROCESSOR = PipelinePreprocessor.of(
                LowerCasePreprocessor.create(),
                GermanUmlautPreprocessor.create(),
                GermanNounNormalizer.create()
        );
    }
}
