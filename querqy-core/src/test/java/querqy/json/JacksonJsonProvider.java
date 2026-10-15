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
package querqy.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

/**
 * Test-only reference implementation of {@link JsonProvider}, backed by plain (non-lenient, non-polymorphic)
 * Jackson data-binding - exercises querqy-core's refactored parsers with a real, known-good JSON
 * implementation without reintroducing a Jackson dependency into querqy-core's own production code:
 * Maven never includes {@code src/test/java} classes, nor {@code test}-scope dependencies (which is how
 * {@code jackson-databind}/{@code jackson-core} are now declared in this module's {@code pom.xml}), in the
 * packaged {@code querqy-core} jar or in anything that depends on it.
 */
public class JacksonJsonProvider implements JsonProvider {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Map<String, Object> readMap(final String json) throws IOException {
        return mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    }

    @Override
    public <T> T readValue(final String json, final Class<T> type) throws IOException {
        return mapper.readValue(json, type);
    }

    @Override
    public String writeValueAsString(final Object value) throws IOException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
    }
}
