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
package querqy.model.convert.converter;

import org.junit.Test;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class MapConverterConfigTest {

    @Test
    public void testParseBooleanToString() {
        Map<String, Object> map = new HashMap<>();
        MapConverterConfig.builder().parseBooleanToString(true).build()
                .convertAndPut(map, "field", false, MapConverterConfig.DEFAULT_MV_CONVERTER);
        assertThat(map).containsExactly(new AbstractMap.SimpleEntry<>("field", "false"));
    }

    @Test
    public void testInclusionOfNullValues() {
        Map<String, Object> map = new HashMap<>();
        MapConverterConfig.builder().includeNullValues(true).build()
                .convertAndPut(map, "field", null, MapConverterConfig.DEFAULT_MV_CONVERTER);
        assertThat(map).containsExactly(new AbstractMap.SimpleEntry<>("field", null));
    }

    @Test
    public void testInclusionOfNullValuesAndBooleanString() {
        Map<String, Object> map = new HashMap<>();
        final MapConverterConfig mapConverterConfig = MapConverterConfig.builder()
                .includeNullValues(true)
                .parseBooleanToString(true)
                .build();

        mapConverterConfig.convertAndPut(map, "field1", null, MapConverterConfig.DEFAULT_MV_CONVERTER);
        mapConverterConfig.convertAndPut(map, "field2", true, MapConverterConfig.DEFAULT_MV_CONVERTER);
        assertThat(map).containsExactly(new AbstractMap.SimpleEntry<>("field1", null),
                new AbstractMap.SimpleEntry<>("field2", "true"));
    }


}
