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

import java.util.Arrays;
import java.util.Objects;

public class Suggestion implements Comparable<Suggestion> {

    public final CharSequence[] sequence;
    public final float score;

    public Suggestion(final CharSequence[] sequence, final float score) {
        this.sequence = sequence;
        this.score = score;
    }


    @Override
    public int compareTo(final Suggestion other) {

        if (other == this) {
            return 0;
        }
        int c = Float.compare(score, other.score); // greater is better
        if (c == 0) {
            c = Integer.compare(sequence.length, other.sequence.length); // shorter is better
            if (c == 0) {
                for (int i = 0; i < sequence.length && c == 0; i++) {
                    c = CharSequenceUtil.compare(sequence[i], other.sequence[i]);
                }
            }
        }

        return c;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Suggestion that = (Suggestion) o;
        return Float.compare(that.score, score) == 0 && Arrays.equals(sequence, that.sequence);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(score);
        result = 31 * result + Arrays.hashCode(sequence);
        return result;
    }
}
