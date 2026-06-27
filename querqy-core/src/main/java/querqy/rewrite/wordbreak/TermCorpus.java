/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020 Querqy Contributors
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
package querqy.rewrite.wordbreak;

/**
 * Provides term lookup operations against a corpus (e.g. a Lucene index field).
 * Implementations are responsible for any field-name and case-normalization details.
 */
public interface TermCorpus {

    boolean exists(CharSequence term);

    int docFreq(CharSequence term);

    int numDocs();

    /**
     * Returns {@code true} if this corpus can answer co-occurrence queries via {@link #coExist}.
     * Implementations that do not carry co-occurrence data must return {@code false} and rely on
     * the default {@link #coExist} implementation, which throws {@link UnsupportedOperationException}.
     */
    boolean isCollationSupported();

    /**
     * Returns {@code true} if {@code term1} and {@code term2} co-occur in at least one document.
     * Only valid when {@link #isCollationSupported()} returns {@code true}.
     *
     * @throws UnsupportedOperationException if this corpus does not support co-occurrence lookup
     */
    default boolean coExist(CharSequence term1, CharSequence term2) {
        throw new UnsupportedOperationException(
                "This TermCorpus implementation does not support co-occurrence lookup");
    }
}
