/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2021 Querqy Contributors
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
package querqy.model.convert;

import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractBuilderTest {

    public Map<String, Object> map(final Entry... entries) {
        return Arrays.stream(entries).collect(Collectors.toMap(entry -> entry.key, entry -> entry.value));
    }

    public Entry entry(final String key, final Object value) {
        return new Entry(key, value);
    }

    @Data
    public static class Entry {
        final String key;
        final Object value;
    }

    @SafeVarargs
    public final <T> List<T> list(final T... elements) {
        return Arrays.asList(elements);
    }
}
