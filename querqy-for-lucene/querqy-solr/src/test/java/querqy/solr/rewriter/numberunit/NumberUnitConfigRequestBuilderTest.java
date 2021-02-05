package querqy.solr.rewriter.numberunit;

import static org.junit.Assert.*;
import static querqy.solr.rewriter.numberunit.NumberUnitRewriterFactory.CONF_PROPERTY;

import org.junit.Test;
import querqy.solr.utils.JsonUtil;

import java.util.Map;

public class NumberUnitConfigRequestBuilderTest {

    @Test
    public void testThatConfigObjectMustBeSet() {
        try {
            new NumberUnitConfigRequestBuilder().buildConfig();
            fail("configObject==null must not be allowed");
        } catch (final Exception e) {
            assertTrue(e.getMessage().contains(CONF_PROPERTY));
        }

        try {
            new NumberUnitConfigRequestBuilder().numberUnitConfig(null);
        } catch (final Exception e) {
            assertTrue(e.getMessage().contains("numberUnitConfig"));
        }

    }

    @Test
    public void testConfigObject() {
        final NumberUnitConfigObject numberUnitConfigObject = JsonUtil.readJson(
                getClass().getClassLoader().getResourceAsStream("configs/numberunit/number-unit-minimal-config.json"),
                NumberUnitConfigObject.class);

        final Map<String, Object> config = new NumberUnitConfigRequestBuilder().numberUnitConfig(numberUnitConfigObject)
                .buildConfig();


        assertNotNull(config);
        final String numberUnitConfigString = (String) config.get(CONF_PROPERTY);
        assertNotNull(numberUnitConfigString);

        assertEquals(numberUnitConfigObject, JsonUtil.readJson(numberUnitConfigString, NumberUnitConfigObject.class));



    }



}