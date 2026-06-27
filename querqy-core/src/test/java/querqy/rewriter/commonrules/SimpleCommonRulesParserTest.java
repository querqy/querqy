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
package querqy.rewriter.commonrules;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static querqy.rewriter.commonrules.model.BoostInstruction.BoostMethod.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import querqy.model.Input;
import querqy.rewriter.commonrules.model.BoostInstruction;
import querqy.rewriter.commonrules.model.Instructions;
import querqy.rewriter.commonrules.model.RulesCollectionBuilder;

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

        final SimpleCommonRulesParser parser = new SimpleCommonRulesParser(new StringReader(rules), true, parserFactory,
                rulesCollectionBuilder, ADDITIVE);
        parser.parse();


        ArgumentCaptor<Instructions> captor = ArgumentCaptor.forClass(Instructions.class);
        verify(rulesCollectionBuilder).addRule(ArgumentMatchers.any(Input.SimpleInput.class), captor.capture());

        final Instructions instructions = captor.getValue();
        assertEquals("input1#0", instructions.getId());

    }

    @Test
    public void testIdPropertyIsRead() throws IOException, RuleParseException {

        final String rules = "input1 => \n" +
                "SYNONYM: syn1\n" +
                "@_id:\"The Id\"";

        final SimpleCommonRulesParser parser = new SimpleCommonRulesParser(new StringReader(rules), true, parserFactory,
                rulesCollectionBuilder, ADDITIVE);
        parser.parse();


        ArgumentCaptor<Instructions> captor = ArgumentCaptor.forClass(Instructions.class);
        verify(rulesCollectionBuilder).addRule(ArgumentMatchers.any(Input.SimpleInput.class), captor.capture());

        final Instructions instructions = captor.getValue();
        assertEquals("The Id", instructions.getId());

    }

    @Test
    public void testListPropertyIsRead() throws IOException, RuleParseException {

        final String rules = "input1 => \n" +
                "SYNONYM: syn1\n" +
                "@prop1:[\"v1\",\"v2\"]";

        final SimpleCommonRulesParser parser = new SimpleCommonRulesParser(new StringReader(rules), true, parserFactory,
                rulesCollectionBuilder, ADDITIVE);
        parser.parse();


        ArgumentCaptor<Instructions> captor = ArgumentCaptor.forClass(Instructions.class);
        verify(rulesCollectionBuilder).addRule(ArgumentMatchers.any(Input.SimpleInput.class), captor.capture());

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

        final SimpleCommonRulesParser parser = new SimpleCommonRulesParser(new StringReader(rules), true, parserFactory,
                rulesCollectionBuilder, ADDITIVE);
        parser.parse();


        ArgumentCaptor<Instructions> captor = ArgumentCaptor.forClass(Instructions.class);
        verify(rulesCollectionBuilder).addRule(ArgumentMatchers.any(Input.SimpleInput.class), captor.capture());

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

        final SimpleCommonRulesParser parser = new SimpleCommonRulesParser(new StringReader(rules), true, parserFactory,
                rulesCollectionBuilder, ADDITIVE);
        parser.parse();


        ArgumentCaptor<Instructions> captor = ArgumentCaptor.forClass(Instructions.class);
        verify(rulesCollectionBuilder).addRule(ArgumentMatchers.any(Input.SimpleInput.class), captor.capture());

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


        new SimpleCommonRulesParser(new StringReader(rules), true, parserFactory, true, ADDITIVE).parse();

    }

    @Test(expected = RuleParseException.class)
    public void testThatIdArrayIsNotAccepted() throws IOException, RuleParseException {

        final String rules = "input1 => \n" +
                "SYNONYM: syn1\n" +
                "@_id:[\"1\"]";


        new SimpleCommonRulesParser(new StringReader(rules), true, parserFactory, true, ADDITIVE).parse();

    }

    @Test(expected = RuleParseException.class)
    public void testThatInvalidBooleanInputIsRefusedIfBooleanIsEnabled() throws IOException, RuleParseException {

        final String rules = "input1 AND OR => \n" +
                "UP: juu";

        new SimpleCommonRulesParser(new StringReader(rules), true, parserFactory, true, ADDITIVE).parse();

    }

    @Test
    public void testThatInvalidBooleanInputIsAllowedIfBooleanIsDisabled() throws IOException, RuleParseException {

        final String rules = "input1 AND OR => \n" +
                "UP: juu";

        new SimpleCommonRulesParser(new StringReader(rules), false, parserFactory, true, ADDITIVE).parse();

    }

    @Test
    public void testThatNonBooleanInputIsParsedIfBooleanIsEnabled() throws IOException, RuleParseException {

        new SimpleCommonRulesParser(new StringReader("input1 AND_THEN y => \nUP: juu"), true, parserFactory, true,
                ADDITIVE).parse();

        new SimpleCommonRulesParser(new StringReader("input1 (c) => \nUP: juu"), true, parserFactory, true, ADDITIVE)
                .parse();

    }





}
