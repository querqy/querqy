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
package querqy.rewriter.wordbreak;

public class Compound implements Comparable<Compound> {
    public final CharSequence[] terms;
    public final CharSequence compound;
    public final float probability;

    public Compound(final CharSequence[] terms, final CharSequence compound, final float probability) {
        this.terms = terms;
        this.compound = compound;
        this.probability = probability;
    }

    @Override
    public int compareTo(final Compound other) {
        if (other == this) {
            return 0;
        }
        final int c = Float.compare(probability, other.probability); // greater is better
        if (c != 0) {
            return c;
        }
        final int lenCmp = Integer.compare(compound.length(), other.compound.length()); // shorter is better
        if (lenCmp != 0) {
            return lenCmp;
        }
        for (int i = 0, len = compound.length(); i < len; i++) {
            final int charCmp = Character.compare(compound.charAt(i), other.compound.charAt(i));
            if (charCmp != 0) {
                return charCmp;
            }
        }
        return 0;
    }
}
