/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2022 Querqy Contributors
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
package querqy.rewrite.contrib.wordbreak;

import java.util.List;
import java.util.Objects;

public class WordBreak {
    public final CharSequence originalLeft;
    public final CharSequence originalRight;
    public final List<Suggestion> suggestions;

    WordBreak(final CharSequence originalLeft, final CharSequence originalRight, final List<Suggestion> suggestions) {
        this.originalLeft = originalLeft;
        this.originalRight = originalRight;
        this.suggestions = suggestions;
    }

    @Override
    public String toString() {
        return "WordBreak{" +
                "originalLeft=" + originalLeft +
                ", originalRight=" + originalRight +
                ", suggestions=" + suggestions +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final WordBreak wordBreak = (WordBreak) o;
        return Objects.equals(originalLeft, wordBreak.originalLeft)
                && Objects.equals(originalRight, wordBreak.originalRight)
                && Objects.equals(suggestions, wordBreak.suggestions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalLeft, originalRight, suggestions);
    }
}
