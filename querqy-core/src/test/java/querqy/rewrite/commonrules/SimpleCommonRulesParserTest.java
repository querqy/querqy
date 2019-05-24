package querqy.rewrite.commonrules;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import querqy.rewrite.commonrules.model.Input;
import querqy.rewrite.commonrules.model.Instructions;
import querqy.rewrite.commonrules.model.RulesCollectionBuilder;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Optional;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class SimpleCommonRulesParserTest {

    QuerqyParserFactory parserFactory = new WhiteSpaceQuerqyParserFactory();

    @Mock
    RulesCollectionBuilder rulesCollectionBuilder;

    @Test
    public void testIdPropertyIsCreatedIfNotDefined() throws IOException, RuleParseException {

        final String rules = "input1 => \n" +
                "SYNONYM: syn1";

        final SimpleCommonRulesParser parser = new SimpleCommonRulesParser(new StringReader(rules), parserFactory,
                rulesCollectionBuilder);
        parser.parse();


        ArgumentCaptor<Instructions> captor = ArgumentCaptor.forClass(Instructions.class);
        verify(rulesCollectionBuilder).addRule(ArgumentMatchers.any(Input.class), captor.capture());

        final Instructions instructions = captor.getValue();
        assertEquals("input1#0", instructions.getId());

    }

    @Test
    public void testIdPropertyIsRead() throws IOException, RuleParseException {

        final String rules = "input1 => \n" +
                "SYNONYM: syn1\n" +
                "@_id:\"The Id\"";

        final SimpleCommonRulesParser parser = new SimpleCommonRulesParser(new StringReader(rules), parserFactory,
                rulesCollectionBuilder);
        parser.parse();


        ArgumentCaptor<Instructions> captor = ArgumentCaptor.forClass(Instructions.class);
        verify(rulesCollectionBuilder).addRule(ArgumentMatchers.any(Input.class), captor.capture());

        final Instructions instructions = captor.getValue();
        assertEquals("The Id", instructions.getId());

    }

    @Test
    public void testListPropertyIsRead() throws IOException, RuleParseException {

        final String rules = "input1 => \n" +
                "SYNONYM: syn1\n" +
                "@prop1:[\"v1\",\"v2\"]";

        final SimpleCommonRulesParser parser = new SimpleCommonRulesParser(new StringReader(rules), parserFactory,
                rulesCollectionBuilder);
        parser.parse();


        ArgumentCaptor<Instructions> captor = ArgumentCaptor.forClass(Instructions.class);
        verify(rulesCollectionBuilder).addRule(ArgumentMatchers.any(Input.class), captor.capture());

        final Instructions instructions = captor.getValue();
        assertEquals(Optional.of(Arrays.asList("v1", "v2")), instructions.getProperty("prop1"));

    }

    @Test
    public void testPropertyObjectIsRead() throws IOException, RuleParseException {

        final String rules = "input1 => \n" +
                "SYNONYM: syn1\n" +
                "@{" +
                "   prop1:[\"v1\",\"v2\"]," +
                "   prop2:true" +
                "}@";

        final SimpleCommonRulesParser parser = new SimpleCommonRulesParser(new StringReader(rules), parserFactory,
                rulesCollectionBuilder);
        parser.parse();


        ArgumentCaptor<Instructions> captor = ArgumentCaptor.forClass(Instructions.class);
        verify(rulesCollectionBuilder).addRule(ArgumentMatchers.any(Input.class), captor.capture());

        final Instructions instructions = captor.getValue();
        assertEquals(Optional.of(Arrays.asList("v1", "v2")), instructions.getProperty("prop1"));
        assertEquals(Optional.of(true), instructions.getProperty("prop2"));

    }

    @Test
    public void testLogPropertyCanBeUsedInObject() throws IOException, RuleParseException {

        final String rules = "input1 => \n" +
                "SYNONYM: syn1\n" +
                "@{" +
                "   _log:'some message'," +
                "   prop1:[\"v1\",\"v2\"]," +
                "   prop2:true" +
                "}@";

        final SimpleCommonRulesParser parser = new SimpleCommonRulesParser(new StringReader(rules), parserFactory,
                rulesCollectionBuilder);
        parser.parse();


        ArgumentCaptor<Instructions> captor = ArgumentCaptor.forClass(Instructions.class);
        verify(rulesCollectionBuilder).addRule(ArgumentMatchers.any(Input.class), captor.capture());

        final Instructions instructions = captor.getValue();
        assertEquals(Optional.of("some message"), instructions.getProperty("_log"));

    }

    @Test(expected = RuleParseException.class)
    public void testThatIdCanOnlyBeUsedOnce() throws IOException, RuleParseException {

        final String rules = "input1 => \n" +
                "SYNONYM: syn1\n" +
                "@_id:\"1\"" +
                "\n\n" +
                "input2 => \n" +
                "SYNONYM: syn2\n" +
                "@_id:\"1\"";


        new SimpleCommonRulesParser(new StringReader(rules), parserFactory, true).parse();

    }

    @Test(expected = RuleParseException.class)
    public void testThatIdArrayIsNotAccepted() throws IOException, RuleParseException {

        final String rules = "input1 => \n" +
                "SYNONYM: syn1\n" +
                "@_id:[\"1\"]";


        new SimpleCommonRulesParser(new StringReader(rules), parserFactory, true).parse();

    }




}