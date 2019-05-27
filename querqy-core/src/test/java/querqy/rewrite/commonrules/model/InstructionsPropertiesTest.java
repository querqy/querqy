package querqy.rewrite.commonrules.model;

import static org.junit.Assert.*;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import org.junit.Test;

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
                .jsonProvider(new JacksonJsonProvider()).mappingProvider(new JacksonMappingProvider()).build());

        assertTrue(instructionsProperties.matches("$[?(@.p2 == '2')]"));
        assertFalse(instructionsProperties.matches("$.[?(@.p2 == 1)]"));

        assertTrue(instructionsProperties.matches("$.p3[?(@ == '3')]"));
        assertTrue(instructionsProperties.matches("$.p4[?(@.p42 == 42)]"));
        assertTrue(instructionsProperties.matches("$.p4[?(@.p42 > 5)]"));


    }
}