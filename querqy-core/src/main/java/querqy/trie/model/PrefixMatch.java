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

public class PrefixMatch<T> {
    public final T match;
    public final int exclusiveEnd;
    public final CharSequence wildcardMatch;
    private int lookupOffset;

    public PrefixMatch(final int exclusiveEnd, final T match) {
        this(exclusiveEnd, "", match);
    }

    public PrefixMatch(final int exclusiveEnd, final CharSequence wildcardMatch, final T match) {
        this.exclusiveEnd = exclusiveEnd;
        this.wildcardMatch = wildcardMatch;
        this.match = match;
    }

    public int getLookupOffset() {
        return lookupOffset;
    }

    public PrefixMatch<T> setLookupOffset(final int lookupOffset) {
        this.lookupOffset = lookupOffset;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final PrefixMatch<?> that = (PrefixMatch<?>) o;
        return exclusiveEnd == that.exclusiveEnd &&
                lookupOffset == that.lookupOffset &&
                Objects.equals(match, that.match);
    }

    @Override
    public int hashCode() {
        return Objects.hash(match, exclusiveEnd, lookupOffset);
    }

    @Override
    public String toString() {
        return "PrefixMatch{" +
                "match=" + match +
                ", exclusiveEnd=" + exclusiveEnd +
                ", lookupOffset=" + lookupOffset +
                '}';
    }
}
