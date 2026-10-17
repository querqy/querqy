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

import static org.junit.Assert.*;

import com.jayway.jsonpath.Configuration;
import org.junit.Test;
import querqy.json.jsonpath.QuerqyJsonPathMappingProvider;
import querqy.json.jsonpath.QuerqyJsonPathProvider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InstructionsPropertiesTest {

    @Test
    public void testGetProperty() {

        final Map<String, Object> props = new HashMap<>();
        props.put("p1", 1);
        props.put("p2", "2");
        props.put("p3", Arrays.asList("3", 33));
        final InstructionsProperties instructionsProperties = new InstructionsProperties(props);
        assertEquals(Optional.of(1), instructionsProperties.getProperty("p1"));
        assertEquals(Optional.of("2"), instructionsProperties.getProperty("p2"));
        assertEquals(Optional.of(Arrays.asList("3", 33)), instructionsProperties.getProperty("p3"));
        assertFalse(instructionsProperties.getProperty("p4").isPresent());

    }

    @Test
    public void testGetPropertyMap() {

        final Map<String, Object> props = new HashMap<>();
        props.put("p1", 1);
        props.put("p2", "2");
        final InstructionsProperties instructionsProperties = new InstructionsProperties(props);

        assertEquals(props, instructionsProperties.getPropertyMap());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetPropertyMapIsUnmodifiable() {

        final Map<String, Object> props = new HashMap<>();
        props.put("p1", 1);
        final InstructionsProperties instructionsProperties = new InstructionsProperties(props);

        instructionsProperties.getPropertyMap().put("p2", "2");
    }

    @Test
    public void testMatches() {

        final Map<String, Object> props = new HashMap<>();
        props.put("p1", 1);
        props.put("p2", "2");
        props.put("p3", Arrays.asList("3", 33));

        final Map<String, Object> p4 = new HashMap<>();
        p4.put("p41", "41");
        p4.put("p42", 42);
        props.put("p4", p4);

        final InstructionsProperties instructionsProperties = new InstructionsProperties(props, Configuration.builder()
                .jsonProvider(new QuerqyJsonPathProvider()).mappingProvider(new QuerqyJsonPathMappingProvider()).build());

        assertTrue(instructionsProperties.matches("$[?(@.p2 == '2')]"));
        assertFalse(instructionsProperties.matches("$.[?(@.p2 == 1)]"));

        assertTrue(instructionsProperties.matches("$.p3[?(@ == '3')]"));
        assertTrue(instructionsProperties.matches("$.p4[?(@.p42 == 42)]"));
        assertTrue(instructionsProperties.matches("$.p4[?(@.p42 > 5)]"));


    }

    @Test
    public void testMatches_withDefaultConfiguration() {
        // Exercises the class's own default Configuration field (the no-arg constructor), rather than one
        // explicitly passed in as testMatches()/testInOperatorMatches() do above (both now also backed by
        // QuerqyJsonPathProvider/QuerqyJsonPathMappingProvider, but via the explicit two-arg constructor) -
        // this is the test proving the actual default path (used in production via PropertyParser) correctly
        // backs filter/predicate matching.
        final Map<String, Object> props = new HashMap<>();
        props.put("p1", 1);
        props.put("p2", "2");
        props.put("p3", Arrays.asList("3", 33));

        final Map<String, Object> p4 = new HashMap<>();
        p4.put("p41", "41");
        p4.put("p42", 42);
        props.put("p4", p4);

        final InstructionsProperties instructionsProperties = new InstructionsProperties(props);

        assertTrue(instructionsProperties.matches("$[?(@.p2 == '2')]"));
        assertFalse(instructionsProperties.matches("$.[?(@.p2 == 1)]"));

        assertTrue(instructionsProperties.matches("$.p3[?(@ == '3')]"));
        assertTrue(instructionsProperties.matches("$.p4[?(@.p42 == 42)]"));
        assertTrue(instructionsProperties.matches("$.p4[?(@.p42 > 5)]"));

        assertTrue(instructionsProperties.matches("$[?(@.p2 in [\"2\",\"11\"])]"));
    }

    @Test
    public void testInOperatorMatches() {
        // addresses #491
        final Map<String, Object> props = new HashMap<>();
        props.put("p1", 1);
        props.put("p2", "2");
        props.put("p3", Arrays.asList("3", 33));

        final Map<String, Object> p4 = new HashMap<>();
        p4.put("p41", "41");
        p4.put("p42", 42);
        props.put("p4", p4);

        final InstructionsProperties instructionsProperties = new InstructionsProperties(props, Configuration.builder()
                .jsonProvider(new QuerqyJsonPathProvider()).mappingProvider(new QuerqyJsonPathMappingProvider()).build());

        assertTrue(instructionsProperties.matches("$[?(@.p2 in [\"2\",\"11\"])]"));

    }
}