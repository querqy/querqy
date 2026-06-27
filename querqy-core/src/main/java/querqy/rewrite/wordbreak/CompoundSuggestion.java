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

import querqy.CharSequenceUtil;

public class CompoundSuggestion implements Comparable<CompoundSuggestion> {

    final CharSequence[] suggestions;
    final float score;

    public CompoundSuggestion(final CharSequence[] suggestions, final float score) {
        this.suggestions = suggestions;
        this.score = score;
    }


    @Override
    public int compareTo(final CompoundSuggestion other) {

        if (other == this) {
            return 0;
        }
        int c = Float.compare(score, other.score); // greater is better
        if (c == 0) {
            c = Integer.compare(suggestions.length, other.suggestions.length); // shorter is better
            if (c == 0) {
                for (int i = 0; i < suggestions.length && c == 0; i++) {
                    c = CharSequenceUtil.compare(suggestions[i], other.suggestions[i]);
                }
            }
        }

        return c;
    }

}
