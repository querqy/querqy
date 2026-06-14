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
package querqy.regex;

import java.util.Set;

public class NFAFragment<T> {

    final NFAState<T> start;
    final Set<NFAState<T>> accepts;

    public NFAFragment(final NFAState<T> start, final Set<NFAState<T>> accepts) {
        this.start = start;
        this.accepts = accepts;
    }
    public static <T> NFAFragment<T> single(final NFAState<T> start, final NFAState<T> accept) {
        return new NFAFragment<T>(start, Set.of(accept));
    }

    public static <T> NFAFragment<T> empty() {
        final NFAState<T> state = new NFAState<T>();
        return single(state, state);
    }
}

