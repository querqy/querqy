/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Querqy Contributors
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
package querqy.rewriter.commonrules.model;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class InstructionsProperties {

    private static final Configuration JACKSON_CONFIGURATION = Configuration.builder()
            .jsonProvider(new JacksonJsonProvider())
            .mappingProvider(new JacksonMappingProvider())
            .build()
            .addOptions(Option.ALWAYS_RETURN_LIST);

    @EqualsAndHashCode.Include private final Map<String, Object> propertyMap;
    private final DocumentContext documentContext;

    public InstructionsProperties(final Map<String, Object> propertyMap, final Configuration jsonPathConfig) {
        this.propertyMap = propertyMap;
        documentContext = JsonPath.using(jsonPathConfig).parse(propertyMap);
    }

    public InstructionsProperties(final Map<String, Object> propertyMap) {
        this(propertyMap, JACKSON_CONFIGURATION);
    }


    public Optional<Object> getProperty(final String name) {
        return Optional.ofNullable(propertyMap.get(name));
    }

    public boolean matches(final String jsonPath) {
        final List read = documentContext.read(jsonPath);
        return read.size() > 0;
    }
}
