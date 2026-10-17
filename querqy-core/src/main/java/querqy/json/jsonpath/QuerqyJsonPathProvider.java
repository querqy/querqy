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
package querqy.json.jsonpath;

import com.jayway.jsonpath.spi.json.AbstractJsonProvider;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * A {@code com.jayway.jsonpath} (JsonPath) {@code JsonProvider} with no third-party JSON library
 * dependency of its own - this is what lets querqy-core use JsonPath's filter/predicate expression engine
 * (see {@code querqy.rewriter.commonrules.model.InstructionsProperties}) without depending on Jackson or
 * json-smart.
 * <p>
 * {@link AbstractJsonProvider} already implements every {@code Map}/{@code List}-walking method
 * ({@code isMap}, {@code isArray}, {@code getMapValue}, {@code setProperty}, {@code getArrayIndex},
 * {@code setArrayIndex}, {@code removeProperty}, {@code getPropertyKeys}, {@code length},
 * {@code toIterable}, {@code unwrap}) purely in terms of plain {@code java.util.Map}/{@code java.util.List}
 * - this class only needs to supply the document-model constructors.
 * <p>
 * {@link #parse(String)}/{@link #parse(InputStream, String)}/{@link #toJson(Object)} are intentionally
 * unimplemented: querqy-core only ever uses JsonPath via {@code JsonPath.using(config).parse(Map)} - the
 * {@code Object} overload, parsing an <em>already-built</em> property map - never raw JSON text, and never
 * serializes a JsonPath document back to text. If that ever changes, these should delegate to a
 * {@code querqy.json.JsonProvider} instance instead of throwing.
 */
public class QuerqyJsonPathProvider extends AbstractJsonProvider {

    private static final String NOT_SUPPORTED_MESSAGE =
            "QuerqyJsonPathProvider only supports querying already-built Map/List property trees via " +
            "JsonPath.using(...).parse(Object) - it does not parse or serialize raw JSON text.";

    @Override
    public Object createArray() {
        return new ArrayList<>();
    }

    @Override
    public Object createMap() {
        return new LinkedHashMap<>();
    }

    @Override
    public Object parse(final String json) {
        throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
    }

    @Override
    public Object parse(final InputStream jsonStream, final String charset) {
        throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
    }

    @Override
    public String toJson(final Object obj) {
        throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
    }
}
