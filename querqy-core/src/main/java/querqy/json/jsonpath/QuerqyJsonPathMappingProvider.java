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

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

/**
 * A {@code com.jayway.jsonpath} (JsonPath) {@code MappingProvider} with no third-party JSON library
 * dependency of its own (see {@link QuerqyJsonPathProvider}).
 * <p>
 * querqy-core never calls a typed {@code DocumentContext.read(path, Class)}/{@code TypeRef} form -
 * {@code InstructionsProperties.matches(String)} only ever uses the untyped
 * {@code documentContext.read(jsonPath)}, confirmed by grepping this whole module - so {@link #map}
 * is only ever reached with a source that's already assignable to the requested type (the identity case
 * below). It throws for anything else rather than guessing at a conversion, exactly like json-path's own
 * {@code JsonSmartMappingProvider} already throws unconditionally for the {@link TypeRef} overload.
 */
public class QuerqyJsonPathMappingProvider implements MappingProvider {

    @Override
    public <T> T map(final Object source, final Class<T> targetType, final Configuration configuration) {
        if (source == null) {
            return null;
        }
        if (targetType.isInstance(source)) {
            return targetType.cast(source);
        }
        throw new UnsupportedOperationException(
                "QuerqyJsonPathMappingProvider cannot convert " + source.getClass().getName()
                        + " into " + targetType.getName() + " - only identity mapping is supported.");
    }

    @Override
    public <T> T map(final Object source, final TypeRef<T> targetType, final Configuration configuration) {
        throw new UnsupportedOperationException("QuerqyJsonPathMappingProvider does not support TypeRef targets.");
    }
}
