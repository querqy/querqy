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
package querqy.trie.model;

import java.util.Objects;

public class SuffixMatch<T> {
    public final T match;
    public final int startSubstring;
    public final CharSequence wildcardMatch;
    private int lookupOffset;

    public SuffixMatch(final int startSubstring, final T match) {
        this(startSubstring, "", match);
    }

    public SuffixMatch(final int startSubstring, final CharSequence wildcardMatch, final T match) {
        this.startSubstring = startSubstring;
        this.wildcardMatch = wildcardMatch;
        this.match = match;
    }

    public int getLookupOffset() {
        return lookupOffset;
    }

    public SuffixMatch<T> setLookupOffset(final int lookupOffset) {
        this.lookupOffset = lookupOffset;
        return this;
    }

    @Override
    public String toString() {
        return "SuffixMatch{" +
                "match=" + match +
                ", startSubstring=" + startSubstring +
                ", lookupOffset=" + lookupOffset +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final SuffixMatch<?> that = (SuffixMatch<?>) o;
        return startSubstring == that.startSubstring &&
                lookupOffset == that.lookupOffset &&
                Objects.equals(match, that.match);
    }

    @Override
    public int hashCode() {
        return Objects.hash(match, startSubstring, lookupOffset);
    }
}
