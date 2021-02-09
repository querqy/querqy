package querqy.solr.rewriter.replace;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static querqy.solr.rewriter.replace.ReplaceRewriterFactory.CONF_IGNORE_CASE;
import static querqy.solr.rewriter.replace.ReplaceRewriterFactory.CONF_RHS_QUERY_PARSER;
import static querqy.solr.rewriter.replace.ReplaceRewriterFactory.CONF_RULES;

import org.junit.Test;
import querqy.rewrite.commonrules.WhiteSpaceQuerqyParserFactory;

import java.util.List;
import java.util.Map;

public class ReplaceConfigRequestBuilderTest {

    @Test
    public void testThatRulesMustBeSet() {
        try {
            new ReplaceConfigRequestBuilder().buildConfig();
            fail("rules==null must not be allowed");
        } catch (final Exception e) {
            assertTrue(e.getMessage().contains(CONF_RULES));
        }

        try {
            new ReplaceConfigRequestBuilder().rules((String) null);
            fail("rules==null must not be allowed");
        } catch (final Exception e) {
            assertTrue(e.getMessage().contains(CONF_RULES));
        }
    }

    @Test
    public void testThatRulesCanBeEmpty() {
        final Map<String, Object> config = new ReplaceConfigRequestBuilder().rules("").buildConfig();
        assertThat(config, hasEntry(CONF_RULES, ""));

        final List<String> errors = new ReplaceRewriterFactory("id").validateConfiguration(config);
        assertTrue(errors == null || errors.isEmpty());
    }

    @Test
    public void testSetAllProperties() {

        final Map<String, Object> config = new ReplaceConfigRequestBuilder().inputDelimiter(";")
                .rules("ab;cd => xy")
                .ignoreCase(false)
                .rhsParser(WhiteSpaceQuerqyParserFactory.class).buildConfig();

        final List<String> errors = new ReplaceRewriterFactory("id").validateConfiguration(config);
        assertTrue(errors == null || errors.isEmpty());

        assertThat(config, hasEntry(CONF_RULES, "ab;cd => xy"));
        assertThat(config, hasEntry(CONF_IGNORE_CASE, Boolean.FALSE));
        assertThat(config, hasEntry(CONF_RHS_QUERY_PARSER, WhiteSpaceQuerqyParserFactory.class.getName()));

    }

}