/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2026 Querqy Contributors
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
package querqy.rewriter.wordbreak;

/**
 * Expected-decompounding fixture shared by {@link GermanMorphologyDecompoundingTableTest} and
 * {@link DutchMorphologyDecompoundingTableTest}.
 */
record ExpectedWordBreak(String originalLeft, String originalRight, String suggestion) {

    static ExpectedWordBreak wb(final String left, final String right, final String expectedWordBreak) {
        return new ExpectedWordBreak(left, right, expectedWordBreak);
    }
}
