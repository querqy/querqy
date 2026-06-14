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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

final class CaptureEvents {

    final Map<Integer, Integer> start = new HashMap<>();
    final Map<Integer, Integer> end   = new HashMap<>();

    CaptureEvents copy() {
        final CaptureEvents c = new CaptureEvents();
        c.start.putAll(this.start);
        c.end.putAll(this.end);
        return c;
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof CaptureEvents other)) return false;
        return this.start.equals(other.start) && this.end.equals(other.end);

    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }
}

