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
package querqy.rewriter.commonrules.rules.property;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import lombok.NoArgsConstructor;
import querqy.rewriter.commonrules.model.InstructionsProperties;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor(staticName = "create")
public class PropertyParser {

    private static final Configuration JACKSON_CONFIGURATION = Configuration.builder()
            .jsonProvider(new JacksonJsonProvider())
            .mappingProvider(new JacksonMappingProvider())
            .build()
            .addOptions(Option.ALWAYS_RETURN_LIST);

    public static final String ID = "_id";
    public static final String LOG_MESSAGE = "_log";

    public InstructionsProperties parse(final Map<String, Object> properties,
                                        final String defaultId) {

        final Map<String, Object> propertiesWithDefaults = new HashMap<>(properties);
        propertiesWithDefaults.putIfAbsent(ID, defaultId);
        propertiesWithDefaults.putIfAbsent(LOG_MESSAGE, propertiesWithDefaults.get(ID));

        return new InstructionsProperties(propertiesWithDefaults, JACKSON_CONFIGURATION);
    }
}
