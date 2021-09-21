package querqy.rewrite.commonrules;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import querqy.rewrite.commonrules.PropertiesBuilder;
import querqy.rewrite.commonrules.RuleParseException;
import querqy.rewrite.commonrules.model.InstructionsProperties;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Deprecated
public class PropertiesBuilderTest {

    PropertiesBuilder builder;

    @Before
    public void setUp() {
        builder = new PropertiesBuilder();
    }

    @Test
    public void testAddSimpleIntegerProperty() throws RuleParseException {

        assertFalse(builder.nextLine("@\"prop1\":011").isPresent());
        assertFalse(builder.nextLine("@\"prop2\":22").isPresent());

        final InstructionsProperties properties = builder.build();

        assertEquals(Optional.of(11), properties.getProperty("prop1"));
        assertEquals(Optional.of(22), properties.getProperty("prop2"));

    }


    @Test
    public void testAddSimpleStringProperty() throws RuleParseException {

        assertFalse(builder.nextLine("@\"prop1\":\"aa\"").isPresent());

        final InstructionsProperties properties = builder.build();

        assertEquals(Optional.of("aa"), properties.getProperty("prop1"));

    }

    @Test
    public void testAddSimpleBooleanProperty() throws RuleParseException {

        assertFalse(builder.nextLine("@\"prop1\": true").isPresent());
        assertFalse(builder.nextLine("@\"prop2\": false").isPresent());

        final InstructionsProperties properties = builder.build();

        assertEquals(Optional.of(true), properties.getProperty("prop1"));
        assertEquals(Optional.of(false), properties.getProperty("prop2"));

    }

    @Test
    public void testAddSimpleDecimalProperty() throws RuleParseException {

        assertFalse(builder.nextLine("@\"prop1\": 0.8349").isPresent());
        assertFalse(builder.nextLine("@\"prop2\": 01913902225.9894").isPresent());
        assertFalse(builder.nextLine("@\"prop3\": 3.9139022259894E9").isPresent());
        assertFalse(builder.nextLine("@\"prop4\": -1.8").isPresent());


        final InstructionsProperties properties = builder.build();

        assertEquals(Optional.of(0.8349), properties.getProperty("prop1"));
        assertEquals(Optional.of(1913902225.9894), properties.getProperty("prop2"));
        assertEquals(Optional.of(3913902225.9894), properties.getProperty("prop3"));
        assertEquals(Optional.of(-1.8), properties.getProperty("prop4"));

    }

    @Test
    public void testAddSimpleArray() throws RuleParseException {

        assertFalse(builder.nextLine("@\"prop1\": [\"1a\", \"1b\"]").isPresent());

        final InstructionsProperties properties = builder.build();

        assertEquals(Optional.of(Arrays.asList("1a", "1b")), properties.getProperty("prop1"));


    }

    @Test
    public void testPropertyCannotBeAddedRepeatedly() throws RuleParseException {

        assertFalse(builder.nextLine("@\"prop1\": 0.8349").isPresent());
        assertFalse(builder.nextLine("@\"prop2\": \"2a\"").isPresent());
        assertTrue(builder.nextLine("@\"prop1\": 0.8349").isPresent());

    }

    @Test
    public void testAddWithoutQuotes() throws RuleParseException {

        assertFalse(builder.nextLine("@prop1: 0.8349").isPresent());
        assertFalse(builder.nextLine("@prop2: 2a").isPresent());

        final InstructionsProperties properties = builder.build();

        assertEquals(Optional.of(0.8349), properties.getProperty("prop1"));
        assertEquals(Optional.of("2a"), properties.getProperty("prop2"));

    }

    @Test
    public void testAddWithSingleQuotes() throws RuleParseException {

        assertFalse(builder.nextLine("@'prop2': '2a'").isPresent());

        final InstructionsProperties properties = builder.build();

        assertEquals(Optional.of("2a"), properties.getProperty("prop2"));

    }

    @Test
    public void testAddAsObject() throws RuleParseException {

        assertFalse(builder.nextLine("@{ ").isPresent());
        assertFalse(builder.nextLine("'prop1': 0.8349,").isPresent());
        assertFalse(builder.nextLine("'prop2': '2a'").isPresent());
        assertFalse(builder.nextLine("}@").isPresent());

        final InstructionsProperties properties = builder.build();

        assertEquals(Optional.of(0.8349), properties.getProperty("prop1"));
        assertEquals(Optional.of("2a"), properties.getProperty("prop2"));

    }

    @Test
    public void testAddAsObjectOnSingleLine() throws RuleParseException {

        assertFalse(builder.nextLine("@{ 'prop1': 0.8349, 'prop2': '2a'}@").isPresent());

        final InstructionsProperties properties = builder.build();

        assertEquals(Optional.of(0.8349), properties.getProperty("prop1"));
        assertEquals(Optional.of("2a"), properties.getProperty("prop2"));

    }

    @Test
    public void testAddAsObjectStartingOnFirstLine() throws RuleParseException {

        assertFalse(builder.nextLine("@{'prop1': 0.8349,").isPresent());
        assertFalse(builder.nextLine("'prop2': '2a'").isPresent());
        assertFalse(builder.nextLine("}@").isPresent());

        final InstructionsProperties properties = builder.build();

        assertEquals(Optional.of(0.8349), properties.getProperty("prop1"));
        assertEquals(Optional.of("2a"), properties.getProperty("prop2"));

    }

    @Test(expected = RuleParseException.class)
    public void testThatPropertyRepeatedInObjectThrowsException() throws RuleParseException {

        assertFalse(builder.nextLine("@{ ").isPresent());
        assertFalse(builder.nextLine("'prop1': 0.8349,").isPresent());
        assertFalse(builder.nextLine("'prop2': '2a',").isPresent());
        assertFalse(builder.nextLine("'prop1': 1.8349,").isPresent());
        assertFalse(builder.nextLine("'prop2': '2b'").isPresent());
        assertFalse(builder.nextLine("}@").isPresent());

        builder.build();

    }

    @Test
    public void testThatPropertyRepeatedBetweenSimplePropsAndObjectThrowsException() throws RuleParseException {

        assertFalse(builder.nextLine("@{ ").isPresent());
        assertFalse(builder.nextLine("'prop1': 0.8349,").isPresent());
        assertFalse(builder.nextLine("'prop2': '2a'").isPresent());
        assertFalse(builder.nextLine("}@").isPresent());

        assertTrue(builder.nextLine("@prop2: '2b'").isPresent());

    }

    @Test
    public void testThatOnlyOneObjectIsAccepted() throws RuleParseException {

        assertFalse(builder.nextLine("@{ ").isPresent());
        assertFalse(builder.nextLine("'prop1': 0.8349,").isPresent());
        assertFalse(builder.nextLine("'prop2': '2a'").isPresent());
        assertFalse(builder.nextLine("}@").isPresent());
        assertTrue(builder.nextLine("@{ ").isPresent());

    }

    @Test
    public void testCombineObjectAndSimpleProps() throws RuleParseException {

        assertFalse(builder.nextLine("@prop0:'p0'").isPresent());
        assertFalse(builder.nextLine("@{ ").isPresent());
        assertFalse(builder.nextLine("'prop1': 0.8349,").isPresent());
        assertFalse(builder.nextLine("'prop2': '2a'").isPresent());
        assertFalse(builder.nextLine("}@").isPresent());
        assertFalse(builder.nextLine("@prop3:42").isPresent());

        final InstructionsProperties properties = builder.build();

        assertEquals(Optional.of("p0"), properties.getProperty("prop0"));
        assertEquals(Optional.of(0.8349), properties.getProperty("prop1"));
        assertEquals(Optional.of("2a"), properties.getProperty("prop2"));
        assertEquals(Optional.of(42), properties.getProperty("prop3"));

    }

    @Test
    public void testCommentInObjectIsIgnored() throws RuleParseException {

        assertFalse(builder.nextLine("@{ ").isPresent());
        assertFalse(builder.nextLine("#'prop1': 0.8349,").isPresent());
        assertFalse(builder.nextLine("'prop2': '2a'#,xx:{prob3:33}").isPresent());
        assertFalse(builder.nextLine("}@").isPresent());

        final InstructionsProperties properties = builder.build();

        assertFalse(properties.getProperty("prop1").isPresent());
        assertFalse(properties.getProperty("xx").isPresent());
        assertEquals(Optional.of("2a"), properties.getProperty("prop2"));

    }

    @Test
    public void testNestedObject() throws RuleParseException {

        assertFalse(builder.nextLine("@{ ").isPresent());
        assertFalse(builder.nextLine("'prop': [").isPresent());
        assertFalse(builder.nextLine("          {'id' : 'id1', ").isPresent());
        assertFalse(builder.nextLine("           'name': 'n1'").isPresent());
        assertFalse(builder.nextLine("          }, ").isPresent());
        assertFalse(builder.nextLine("{'id' : 'id2', ").isPresent());
        assertFalse(builder.nextLine("'name': 'n2'").isPresent());
        assertFalse(builder.nextLine("}]").isPresent());
        assertFalse(builder.nextLine("}@").isPresent());

        final InstructionsProperties properties = builder.build();
        final Optional<Object> propOpt = properties.getProperty("prop");
        assertTrue(propOpt.isPresent());
        propOpt.ifPresent(prop -> {
            assertTrue(prop instanceof List);
            List objects = (List) prop;
            assertEquals(2, objects.size());

            Object object1 = objects.get(0);

            assertTrue(object1 instanceof Map);
            Map map1 = (Map) object1;
            assertEquals("id1", map1.get("id"));
            assertEquals("n1", map1.get("name"));


            Object object2 = objects.get(1);

            assertTrue(object2 instanceof Map);
            Map map2 = (Map) object2;
            assertEquals("id2", map2.get("id"));
            assertEquals("n2", map2.get("name"));

        });


    }
}