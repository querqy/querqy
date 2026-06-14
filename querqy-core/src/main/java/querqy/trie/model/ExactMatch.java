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

public class ExactMatch<T> {

    public final int lookupStart;
    public final int lookupExclusiveEnd;
    public final int termSize;

    public final T value;

    public ExactMatch(final int lookupStart, final int lookupExclusiveEnd, final T value) {
        this.lookupStart = lookupStart;
        this.lookupExclusiveEnd = lookupExclusiveEnd;
        this.termSize = lookupExclusiveEnd - lookupStart;
        this.value = value;
    }

    @Override
    public String toString() {
        return "ExactMatch{" +
                "lookupStart=" + lookupStart +
                ", lookupExclusiveEnd=" + lookupExclusiveEnd +
                ", value=" + value +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExactMatch<?> that = (ExactMatch<?>) o;
        return lookupStart == that.lookupStart &&
                lookupExclusiveEnd == that.lookupExclusiveEnd &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lookupStart, lookupExclusiveEnd, value);
    }
}
