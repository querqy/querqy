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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A fully dependency-free, test-only {@link JsonProvider} implementation - a minimal hand-rolled JSON parser,
 * with no third-party JSON library of its own. Used alongside {@link JsonSmartJsonProvider} (library-backed)
 * to prove that querqy-core's refactored parsers really do work against the {@link JsonProvider} abstraction
 * and don't implicitly assume one particular JSON library's behavior - if the exact same tests pass against
 * both, the SPI boundary is real.
 * <p>
 * Deliberately minimal: only supports what the refactored parsers actually need (objects, arrays, strings,
 * numbers, booleans, null). POJO binding (nested POJOs, {@code List<SomeType>} fields, package-private
 * targets) is delegated to {@link ReflectiveBeanBinder}, shared with {@link JsonSmartJsonProvider}.
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
            return ReflectiveBeanBinder.bind(parsed, type);
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
