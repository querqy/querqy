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
import java.util.Map;

/**
 * SPI for JSON parsing/serialization, so host integrations (OpenSearch, Elasticsearch, Solr) can supply
 * their own JSON implementation instead of querqy-core depending on one itself.
 * <p>
 * Implementations MUST NOT support polymorphic/default-typing deserialization (no {@code @JsonTypeInfo}
 * equivalent, no binding into a class resolved from data in the JSON payload itself) - querqy-core only
 * ever binds into fixed, known POJO shapes declared by the caller. An implementation that adds
 * arbitrary-class deserialization reintroduces the classic deserialization-gadget-chain RCE class of
 * vulnerability that this restriction is partly designed to avoid.
 */
public interface JsonProvider {

    /**
     * Parses {@code json}, a JSON object, into a generic {@link Map}. Nested objects/arrays are
     * themselves represented as nested {@code Map}/{@code List} values.
     *
     * @throws IOException if {@code json} is not valid JSON, or its root value is not an object
     */
    Map<String, Object> readMap(String json) throws IOException;

    /**
     * Parses {@code json} directly into an instance of {@code type} - a fixed, known shape supplied by the
     * caller (never resolved from the JSON payload itself - see the class-level polymorphic-typing
     * restriction above). {@code type} is usually a bean class with a no-arg constructor, but may also be a
     * generic collection/map interface such as {@link java.util.List} or {@link Map} - querqy-lucene's
     * {@code QueryParsingController} binds rewrite-chain debug/logging data this way (e.g.
     * {@code readValue(json, List.class)}).
     *
     * @throws IOException if {@code json} is not valid JSON, or cannot be bound into {@code type}
     */
    <T> T readValue(String json, Class<T> type) throws IOException;

    /**
     * Serializes {@code value} to a pretty-printed JSON string. {@code value} is usually a {@link Map} or
     * {@link java.util.List}, but implementations must also support serializing an arbitrary bean-shaped
     * object graph: querqy-lucene's {@code QueryParsingController} serializes its own internal rewrite-chain
     * log objects (e.g. {@code RewriteChainLog}, {@code ActionLog}) through this method as part of producing
     * debug/logging output, not just plain {@code Map}/{@code List} structures.
     * <p>
     * This SPI makes no guarantee about how {@code null}/empty fields of such an object graph are
     * represented - whether they're included (e.g. {@code "field":null}) or omitted entirely is left to the
     * implementation. Before this SPI existed, {@code QueryParsingController}'s hardcoded Jackson
     * {@code ObjectMapper} explicitly configured {@code NON_NULL}/{@code NON_EMPTY} inclusion to omit them;
     * that was a deliberate configuration choice on that one {@code ObjectMapper}, not Jackson's own default
     * (Jackson's default is to include them), so it is NOT restored automatically just by an implementation
     * being Jackson-backed - any implementation wanting compact, null/empty-free debug/logging output must
     * configure that itself.
     *
     * @throws IOException if {@code value} cannot be serialized
     */
    String writeValueAsString(Object value) throws IOException;
}
