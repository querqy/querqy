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
package querqy.rewrite.lookup.triemap.suffix;

import java.util.Collections;
import java.util.List;

/**
 * A rule whose input contains a leading-wildcard term (e.g. {@code abc *hemd def}). The fixed terms before the
 * wildcard (if any) are matched via a separate, dedicated forward trie (see {@link SuffixWildcardRules}) since they
 * are purely literal; this object only needs to carry the fixed terms that follow the wildcard, which are checked
 * directly, term by term, against the query once a candidate wildcard match is in flight.
 *
 * @param <T> The value type associated with a rule (e.g. {@code InstructionsSupplier}).
 */
public class SuffixWildcardRule<T> {

    private final List<CharSequence> rightTermKeys;
    private final T value;

    public SuffixWildcardRule(final List<CharSequence> rightTermKeys, final T value) {
        this.rightTermKeys = rightTermKeys == null ? Collections.emptyList() : rightTermKeys;
        this.value = value;
    }

    public List<CharSequence> getRightTermKeys() {
        return rightTermKeys;
    }

    public T getValue() {
        return value;
    }
}
