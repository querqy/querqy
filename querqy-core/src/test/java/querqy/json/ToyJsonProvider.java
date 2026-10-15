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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A second, genuinely non-Jackson, test-only {@link JsonProvider} implementation - a minimal hand-rolled
 * JSON parser plus a reflection-based POJO binder, with no third-party JSON library of its own. Used
 * purely to prove that querqy-core's refactored parsers really do work against the {@link JsonProvider}
 * abstraction and don't implicitly assume Jackson-specific behavior - if the exact same tests pass against
 * both this and {@link JacksonJsonProvider}, the SPI boundary is real.
 * <p>
 * Deliberately minimal: only supports what the two refactored parsers actually need (objects, arrays,
 * strings, numbers, booleans, null; binding into simple POJOs with public no-arg constructors and
 * getter/setter pairs, including nested POJOs and {@code List<SomeType>} fields).
 */
public class ToyJsonProvider implements JsonProvider {

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
            return bind(parsed, type);
        } catch (final ReflectiveOperationException e) {
            throw new IOException("Could not bind JSON into " + type.getName(), e);
        }
    }

    @Override
    public String writeValueAsString(final Object value) throws IOException {
        final StringBuilder sb = new StringBuilder();
        write(value, sb);
        return sb.toString();
    }

    private Object parse(final String json) throws IOException {
        final Parser parser = new Parser(json);
        final Object value = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.isAtEnd()) {
            throw new IOException("Trailing content after JSON value at position " + parser.pos);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private <T> T bind(final Object parsed, final Class<T> type) throws ReflectiveOperationException, IOException {
        if (parsed == null) {
            return null;
        }
        if (type.isInstance(parsed)) {
            return (T) parsed;
        }
        if (!(parsed instanceof Map)) {
            throw new IOException("Cannot bind " + parsed + " into " + type.getName());
        }
        final Map<?, ?> map = (Map<?, ?>) parsed;
        final Constructor<T> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true); // binding targets like NumberUnitConfigObject are package-private
        final T instance = constructor.newInstance();
        for (final Method method : type.getMethods()) {
            if (!method.getName().startsWith("set") || method.getParameterCount() != 1) {
                continue;
            }
            final String propertyName = decapitalize(method.getName().substring(3));
            if (!map.containsKey(propertyName)) {
                continue;
            }
            final Object coerced = coerce(map.get(propertyName), method.getGenericParameterTypes()[0]);
            method.setAccessible(true);
            method.invoke(instance, coerced);
        }
        return instance;
    }

    private Object coerce(final Object rawValue, final Type targetType)
            throws ReflectiveOperationException, IOException {
        if (rawValue == null) {
            return null;
        }
        if (targetType instanceof Class) {
            final Class<?> targetClass = (Class<?>) targetType;
            if (targetClass == String.class) {
                return rawValue.toString();
            }
            if ((targetClass == Integer.class || targetClass == int.class) && rawValue instanceof Number) {
                return ((Number) rawValue).intValue();
            }
            if ((targetClass == Float.class || targetClass == float.class) && rawValue instanceof Number) {
                return ((Number) rawValue).floatValue();
            }
            if ((targetClass == Double.class || targetClass == double.class) && rawValue instanceof Number) {
                return ((Number) rawValue).doubleValue();
            }
            if (targetClass.isInstance(rawValue)) {
                return rawValue;
            }
            return bind(rawValue, targetClass); // nested POJO
        }
        if (targetType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) targetType;
            if (parameterizedType.getRawType() == List.class && rawValue instanceof List) {
                final Type elementType = parameterizedType.getActualTypeArguments()[0];
                final List<Object> result = new ArrayList<>();
                for (final Object element : (List<?>) rawValue) {
                    result.add(coerce(element, elementType));
                }
                return result;
            }
        }
        return rawValue;
    }

    private static String decapitalize(final String s) {
        if (s.isEmpty()) {
            return s;
        }
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private void write(final Object value, final StringBuilder sb) throws IOException {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof Map) {
            sb.append('{');
            boolean first = true;
            for (final Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeString(String.valueOf(entry.getKey()), sb);
                sb.append(':');
                write(entry.getValue(), sb);
            }
            sb.append('}');
        } else if (value instanceof List) {
            sb.append('[');
            boolean first = true;
            for (final Object element : (List<?>) value) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                write(element, sb);
            }
            sb.append(']');
        } else if (value instanceof String) {
            writeString((String) value, sb);
        } else if (value instanceof Boolean || value instanceof Number) {
            sb.append(value);
        } else {
            throw new IOException("Cannot serialize value of type " + value.getClass());
        }
    }

    private void writeString(final String s, final StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    /** Minimal recursive-descent parser for strict JSON (the lenient CommonRules dialect is normalized to
     * strict JSON upstream, by {@code LenientJsonNormalizer}, before it ever reaches a {@link JsonProvider}). */
    private static final class Parser {
        private final String json;
        private int pos;

        Parser(final String json) {
            this.json = json;
        }

        boolean isAtEnd() {
            return pos >= json.length();
        }

        Object parseValue() throws IOException {
            skipWhitespace();
            if (isAtEnd()) {
                throw new IOException("Unexpected end of JSON input");
            }
            final char c = json.charAt(pos);
            switch (c) {
                case '{':
                    return parseObject();
                case '[':
                    return parseArray();
                case '"':
                    return parseString();
                case 't':
                    expect("true");
                    return Boolean.TRUE;
                case 'f':
                    expect("false");
                    return Boolean.FALSE;
                case 'n':
                    expect("null");
                    return null;
                default:
                    return parseNumber();
            }
        }

        private Map<String, Object> parseObject() throws IOException {
            expectChar('{');
            final Map<String, Object> result = new LinkedHashMap<>();
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return result;
            }
            while (true) {
                skipWhitespace();
                final String key = parseString();
                skipWhitespace();
                expectChar(':');
                final Object value = parseValue();
                result.put(key, value);
                skipWhitespace();
                final char next = peek();
                if (next == ',') {
                    pos++;
                } else if (next == '}') {
                    pos++;
                    break;
                } else {
                    throw new IOException("Expected ',' or '}' at position " + pos);
                }
            }
            return result;
        }

        private List<Object> parseArray() throws IOException {
            expectChar('[');
            final List<Object> result = new ArrayList<>();
            skipWhitespace();
            if (peek() == ']') {
                pos++;
                return result;
            }
            while (true) {
                result.add(parseValue());
                skipWhitespace();
                final char next = peek();
                if (next == ',') {
                    pos++;
                } else if (next == ']') {
                    pos++;
                    break;
                } else {
                    throw new IOException("Expected ',' or ']' at position " + pos);
                }
            }
            return result;
        }

        private String parseString() throws IOException {
            expectChar('"');
            final StringBuilder sb = new StringBuilder();
            while (true) {
                if (isAtEnd()) {
                    throw new IOException("Unterminated string at position " + pos);
                }
                final char c = json.charAt(pos++);
                if (c == '"') {
                    break;
                } else if (c == '\\') {
                    final char escaped = json.charAt(pos++);
                    switch (escaped) {
                        case '"':
                            sb.append('"');
                            break;
                        case '\\':
                            sb.append('\\');
                            break;
                        case '/':
                            sb.append('/');
                            break;
                        case 'n':
                            sb.append('\n');
                            break;
                        case 'r':
                            sb.append('\r');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case 'b':
                            sb.append('\b');
                            break;
                        case 'f':
                            sb.append('\f');
                            break;
                        case 'u':
                            sb.append((char) Integer.parseInt(json.substring(pos, pos + 4), 16));
                            pos += 4;
                            break;
                        default:
                            throw new IOException("Invalid escape sequence \\" + escaped + " at position " + pos);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        /** Returns a {@link Double} for every JSON number - callers coerce to the actual target type. */
        private Double parseNumber() throws IOException {
            final int start = pos;
            if (peek() == '-') {
                pos++;
            }
            while (!isAtEnd() && Character.isDigit(json.charAt(pos))) {
                pos++;
            }
            if (!isAtEnd() && json.charAt(pos) == '.') {
                pos++;
                while (!isAtEnd() && Character.isDigit(json.charAt(pos))) {
                    pos++;
                }
            }
            if (!isAtEnd() && (json.charAt(pos) == 'e' || json.charAt(pos) == 'E')) {
                pos++;
                if (!isAtEnd() && (json.charAt(pos) == '+' || json.charAt(pos) == '-')) {
                    pos++;
                }
                while (!isAtEnd() && Character.isDigit(json.charAt(pos))) {
                    pos++;
                }
            }
            final String numberText = json.substring(start, pos);
            if (numberText.isEmpty() || "-".equals(numberText)) {
                throw new IOException("Invalid number at position " + start);
            }
            try {
                return Double.parseDouble(numberText);
            } catch (final NumberFormatException e) {
                throw new IOException("Invalid number '" + numberText + "' at position " + start, e);
            }
        }

        private char peek() throws IOException {
            if (isAtEnd()) {
                throw new IOException("Unexpected end of JSON input");
            }
            return json.charAt(pos);
        }

        private void expectChar(final char expected) throws IOException {
            if (isAtEnd() || json.charAt(pos) != expected) {
                throw new IOException("Expected '" + expected + "' at position " + pos);
            }
            pos++;
        }

        private void expect(final String literal) throws IOException {
            if (pos + literal.length() > json.length() || !json.regionMatches(pos, literal, 0, literal.length())) {
                throw new IOException("Expected '" + literal + "' at position " + pos);
            }
            pos += literal.length();
        }

        void skipWhitespace() {
            while (!isAtEnd() && Character.isWhitespace(json.charAt(pos))) {
                pos++;
            }
        }
    }
}
