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

import net.minidev.json.JSONValue;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import java.io.IOException;
import java.util.Map;

/**
 * A third, library-backed test-only {@link JsonProvider} implementation, backed by
 * {@code net.minidev:json-smart} - a library this module cannot avoid depending on regardless, since it's a
 * mandatory, non-optional transitive runtime dependency of {@code com.jayway.jsonpath:json-path} (see
 * querqy-core's {@code pom.xml}). Gives a second, independently-maintained-library cross-check on the
 * {@link JsonProvider} abstraction's JSON parsing/writing (as opposed to {@link ToyJsonProvider}'s hand-rolled
 * one) without adding any dependency this module doesn't already carry.
 * <p>
 * Uses {@link JSONParser#MODE_RFC4627} (strict JSON) rather than json-smart's default lenient parsing, since
 * the lenient CommonRules property dialect is already normalized to strict JSON upstream (by
 * {@code LenientJsonNormalizer}) before it ever reaches a {@code JsonProvider}.
 * <p>
 * {@code readValue} parses into json-smart's own generic {@code JSONObject}/{@code JSONArray} structure and
 * then binds that into the target class via the shared {@link ReflectiveBeanBinder}, rather than using
 * json-smart's own ASM-based bean binder ({@code net.minidev:accessors-smart}): that binder generates its
 * accessor class in a different {@link ClassLoader} instance than the one that loaded the target class, which
 * fails with {@code IllegalAccessError} against querqy-core's package-private binding targets (e.g.
 * {@code NumberUnitConfigObject}) - the JVM's package-private access check requires the same defining
 * classloader, not just the same package name.
 */
public class JsonSmartJsonProvider implements JsonProvider {

    @Override
    public Map<String, Object> readMap(final String json) throws IOException {
        final Object parsed = parse(json);
        if (!(parsed instanceof Map)) {
            throw new IOException("Expected a JSON object at the root, got: " + parsed);
        }
        @SuppressWarnings("unchecked")
        final Map<String, Object> map = (Map<String, Object>) parsed;
        return map;
    }

    @Override
    public <T> T readValue(final String json, final Class<T> type) throws IOException {
        final Object parsed = parse(json);
        try {
            return ReflectiveBeanBinder.bind(parsed, type);
        } catch (final ReflectiveOperationException e) {
            throw new IOException("Could not bind JSON into " + type.getName(), e);
        }
    }

    @Override
    public String writeValueAsString(final Object value) throws IOException {
        return JSONValue.toJSONString(value);
    }

    private Object parse(final String json) throws IOException {
        try {
            return new JSONParser(JSONParser.MODE_RFC4627).parse(json);
        } catch (final ParseException e) {
            throw new IOException("Could not parse JSON: " + json, e);
        }
    }
}
