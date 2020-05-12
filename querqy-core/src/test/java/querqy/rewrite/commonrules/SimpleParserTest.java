package querqy.rewrite.commonrules;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Test;

import querqy.model.Clause.Occur;
import querqy.model.InputSequenceElement;
import querqy.model.Query;
import querqy.model.StringRawQuery;
import querqy.model.Term;
import querqy.rewrite.commonrules.model.*;
import querqy.rewrite.commonrules.model.BoostInstruction.BoostDirection;

public class SimpleParserTest extends AbstractCommonRulesTest {
    
    Reader reader; 
    QuerqyParserFactory querqyParserFactory = new WhiteSpaceQuerqyParserFactory();
    
    SimpleCommonRulesParser createParserWithEmptyReader() {
        return createParserFromString("", false);
    }

    SimpleCommonRulesParser createParserFromString(final String rulesString, final boolean ignoreCase) {
        reader = new StringReader(rulesString);
        return new SimpleCommonRulesParser(reader, querqyParserFactory, ignoreCase);
    }

    SimpleCommonRulesParser createParserFromResource(String resourceName, boolean ignoreCase) {
        reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(resourceName));
        return new SimpleCommonRulesParser(reader, querqyParserFactory, ignoreCase);
    }

    RulesCollection createRulesFromResource(String resourceName, boolean ignoreCase) throws IOException, RuleParseException {
        SimpleCommonRulesParser parser = createParserFromResource(resourceName, ignoreCase);
        return parser.parse();
    }
    
    Query makeQueryUsingFactory(String qString) {
        return querqyParserFactory.createParser().parse(qString);
    }
    
    
    @After
    public void tearDown() throws IOException {
        if (reader != null) {
            reader.close();
        }

    }

    @Test
    public void testStripLine() {
        SimpleCommonRulesParser parser = createParserWithEmptyReader();
        assertEquals("", parser.stripLine(""));
        assertEquals("", parser.stripLine(" "));
        assertEquals("", parser.stripLine("#dss"));
        assertEquals("", parser.stripLine(" #sdsd"));
        assertEquals("", parser.stripLine("\t #sdsd"));
    }
    
    @Test
    public void test01() throws Exception {
        RulesCollection rules = createRulesFromResource("rules-test.txt", false);
        Term t1 = new Term(null, "aa");
        Term t2 = new Term(null, "l");
        PositionSequence<InputSequenceElement> seq = new PositionSequence<>();
        seq.nextPosition();
        seq.addElement(t1);
        seq.nextPosition();
        seq.addElement(t2);
        List<Action> actions = getActions(rules, seq);
        assertThat(actions, contains(
                new Action(new Instructions(1, "1", Collections.singletonList(
                        new DeleteInstruction(Collections.singletonList(mkTerm("aa")))
                )),
                        new TermMatches(new TermMatch(t1)), 0, 1)));
    }

    @Test
    public void test02() throws Exception {
        RulesCollection rules = createRulesFromResource("rules-test.txt", false);
        Term t1 = new Term(null, "a");
        Term t2 = new Term(null, "b");
        Term t3 = new Term(null, "c");
        Term t4 = new Term(null, "l");
        PositionSequence<InputSequenceElement> seq = new PositionSequence<>();
        seq.nextPosition();
        seq.addElement(t1);
        seq.nextPosition();
        seq.addElement(t2);
        seq.nextPosition();
        seq.addElement(t3);
        seq.nextPosition();
        seq.addElement(t4);
        List<Action> actions = getActions(rules, seq);
        assertThat(actions, contains( 

                new Action(
                                new Instructions(1, "1",
                                        Collections.singletonList(
                                                new DeleteInstruction(Collections.singletonList(mkTerm("b")))
                                        )),
                                                new TermMatches(Arrays.asList(new TermMatch(t1), new TermMatch(t2))), 0, 2),
                                
                new Action(
                                new Instructions(3, "3",
                                        Arrays.asList(
                                                new DeleteInstruction(Collections.singletonList(mkTerm("a"))),
                                                new DeleteInstruction(Collections.singletonList(mkTerm("c")))
                                        )),
                                        new TermMatches(Arrays.asList(new TermMatch(t1), new TermMatch(t2), new TermMatch(t3))), 0, 3),
                new Action(
                        new Instructions(6, "6",
                                Collections.singletonList(
                                        new BoostInstruction(
                                                new StringRawQuery(null, "color:x", Occur.SHOULD, false), BoostDirection.DOWN, 2f)
                                )),
                        new TermMatches(new TermMatch(t1)), 0, 1)

                ));	
    }
    
    @Test
    public void test04() throws Exception {
        RulesCollection rules = createRulesFromResource("rules-test.txt", false);
        Term t1 = new Term(null, "t1");
        Term t2 = new Term(null, "t2");
        PositionSequence<InputSequenceElement> seq = new PositionSequence<>();
        seq.nextPosition();
        seq.addElement(t1);
        seq.nextPosition();
        seq.addElement(t2);
        List<Action> actions = getActions(rules, seq);
        assertThat(actions, contains( 
                new Action(
                                new Instructions(1, "1",
                                        Collections.singletonList(
                                                new BoostInstruction(makeQueryUsingFactory("tboost tb2"), BoostDirection.UP, 3.5f)
                                        )),
                                                        new TermMatches(Arrays.asList(new TermMatch(t1), new TermMatch(t2))), 0, 2)
                
                
                ));
    }
    
    @Test
    public void test05() throws Exception {
        RulesCollection rules = createRulesFromResource("rules-test.txt", false);
        Term t1 = new Term(null, "tf2");
        PositionSequence<InputSequenceElement> seq = new PositionSequence<>();
        seq.nextPosition();
        seq.addElement(t1);
        List<Action> actions = getActions(rules, seq);
        assertThat(actions, contains(
                new Action(
                        new Instructions(1, "1",
                                Collections.singletonList(new FilterInstruction(makeQueryUsingFactory("flt2 flt3"))
                                )
                        ),
                        new TermMatches(new TermMatch(t1)), 0, 1)
                
                
                ));
    }
  
/*    
    
ts3 =>
    SYNONYM: syn2
    
ts4 ts5 =>
    SYNONYM: syn3  syn4
    
ts6 =>
    SYNONYM: syn5 syn6 syn7
*/
    
    /**
     *   
     *   ts1 ts2 =>
     *       SYNONYM: syn1
     *   
     * @throws Exception
     */
    @Test
    public void test06() throws Exception {
        RulesCollection rules = createRulesFromResource("rules-test.txt", false);
        Term t1 = new Term(null, "ts1");
        Term t2 = new Term(null, "ts2");
        PositionSequence<InputSequenceElement> seq = new PositionSequence<>();
        seq.nextPosition();
        seq.addElement(t1);
        seq.nextPosition();
        seq.addElement(t2);
        List<Action> actions = getActions(rules, seq);
        assertThat(actions, contains(
                new Action(
                        new Instructions(1, "1",
                                Collections.singletonList(
                                        new SynonymInstruction(Collections.singletonList(mkTerm("syn1")))
                                )
                                        ),
                        new TermMatches(Arrays.asList(new TermMatch(t1), new TermMatch(t2))), 0, 2)
                
                
                ));
    }
    
    /**
     * ts6 =>
     *    SYNONYM: syn5 f1:syn6 {f2,f3}:syn7
     * @throws Exception
     */
    @Test
    public void test07() throws Exception {
        RulesCollection rules = createRulesFromResource("rules-test.txt", false);
        Term t1 = new Term(null, "ts6");
        PositionSequence<InputSequenceElement> seq = new PositionSequence<>();
        seq.nextPosition();
        seq.addElement(t1);
        List<Action> actions = getActions(rules, seq);
        assertThat(actions, contains(
                new Action(
                                new Instructions(1, "1",
                                        Collections.singletonList(
                                                new SynonymInstruction(
                                                        Arrays.asList(
                                                                mkTerm("syn5"),
                                                                mkTerm("syn6", "f1"),
                                                                mkTerm("syn7", "f2", "f3")
                                                        ))
                                        )
                                        ),
                                                        new TermMatches(new TermMatch(t1)), 0, 1)
                
                
                ));
    }
    
    /**
     * tS7 Ts8 TS => 
     *    FILTER : FLT4
     * @throws Exception
     */
    @Test
    public void test08() throws Exception {
        // test case sensitive - no match
        RulesCollection rules = createRulesFromResource("rules-test.txt", false);
        Term t1 = new Term(null, "ts7");
        Term t2 = new Term(null, "ts8");
        Term t3 = new Term(null, "ts");
        PositionSequence<InputSequenceElement> seq = new PositionSequence<>();
        seq.nextPosition();
        seq.addElement(t1);
        seq.nextPosition();
        seq.addElement(t2);
        seq.nextPosition();
        seq.addElement(t3);
        List<Action> actions = getActions(rules, seq);
        assertTrue(actions.isEmpty());
        
    }
    
    /**
     * tS7 Ts8 TS => 
     *    FILTER : FLT4
     * @throws Exception
     */
    @Test
    public void test09() throws Exception {
        // test case sensitive -  match
        RulesCollection rules = createRulesFromResource("rules-test.txt", false);
        Term t1 = new Term(null, "tS7");
        Term t2 = new Term(null, "Ts8");
        Term t3 = new Term(null, "TS");
        PositionSequence<InputSequenceElement> seq = new PositionSequence<>();
        seq.nextPosition();
        seq.addElement(t1);
        seq.nextPosition();
        seq.addElement(t2);
        seq.nextPosition();
        seq.addElement(t3);
        List<Action> actions = getActions(rules, seq);

        assertThat(actions, contains( 
                new Action(
                        new Instructions(1, "1",
                                Collections.singletonList(
                                                new FilterInstruction(makeQueryUsingFactory("FLT4"))
                                        )),
                        new TermMatches(Arrays.asList(new TermMatch(t1), new TermMatch(t2), new TermMatch(t3))), 0, 3)
                
                
                ));
    }
    
    /**
     * tS7 Ts8 TS => 
     *    FILTER : FLT4
     * @throws Exception
     */
    @Test
    public void test10() throws Exception {
        // test case insensitive -  match
        RulesCollection rules = createRulesFromResource("rules-test.txt", true);
        Term t1 = new Term(null, "tS7");
        Term t2 = new Term(null, "Ts8");
        Term t3 = new Term(null, "TS");
        PositionSequence<InputSequenceElement> seq = new PositionSequence<>();
        seq.nextPosition();
        seq.addElement(t1);
        seq.nextPosition();
        seq.addElement(t2);
        seq.nextPosition();
        seq.addElement(t3);
        List<Action> actions = getActions(rules, seq);

        assertThat(actions, contains( 
                new Action(
                        new Instructions(1, "1",
                                Collections.singletonList(
                                        new FilterInstruction(makeQueryUsingFactory("FLT4"))
                                )),
                        new TermMatches(Arrays.asList(new TermMatch(t1), new TermMatch(t2), new TermMatch(t3))), 0, 3)
                
                
                ));
    }
    
    /**
     * tS7 Ts8 TS => 
     *    FILTER : FLT4
     * @throws Exception
     */
    @Test
    public void test11() throws Exception {
        // test case insensitive -  match
        RulesCollection rules = createRulesFromResource("rules-test.txt", true);
        Term t1 = new Term(null, "Ts7");
        Term t2 = new Term(null, "tS8");
        Term t3 = new Term(null, "ts");
        PositionSequence<InputSequenceElement> seq = new PositionSequence<>();
        seq.nextPosition();
        seq.addElement(t1);
        seq.nextPosition();
        seq.addElement(t2);
        seq.nextPosition();
        seq.addElement(t3);
        List<Action> actions = getActions(rules, seq);

        assertThat(actions, contains( 
                new Action(
                        new Instructions(1, "1",
                                Collections.singletonList(
                                                new FilterInstruction(makeQueryUsingFactory("FLT4"))
                                        )),
                        new TermMatches(Arrays.asList(new TermMatch(t1), new TermMatch(t2), new TermMatch(t3))), 0, 3)
                ));
    }
    
    /**
     * "tb1 =>
     *   FILTER: FLTTB1
     *
     * "tb2" =>
     *   FILTER: FLTTB2
     *
     * tb3" =>
     *   FILTER: FLTTB3
     * @throws Exception
     */
    @Test
    public void test12() throws Exception {
        RulesCollection rules = createRulesFromResource("rules-test.txt", true);
        Term t1 = new Term(null, "tb1");
        Term t2 = new Term(null, "tbx");
        PositionSequence<InputSequenceElement> seq = new PositionSequence<>();
        seq.nextPosition();
        seq.addElement(CommonRulesRewriter.LEFT_BOUNDARY);
        seq.nextPosition();
        seq.addElement(t1);
        seq.nextPosition();
        seq.addElement(t2);
        seq.nextPosition();
        seq.addElement(CommonRulesRewriter.RIGHT_BOUNDARY);
        List<Action> actions = getActions(rules, seq);

        assertThat(actions, contains( 
                new Action(
                        new Instructions(1, "1",
                                Collections.singletonList(
                                        new FilterInstruction(makeQueryUsingFactory("FLTTB1"))
                                )),
                        new TermMatches(Collections.singletonList(new TermMatch(t1))), 0, 1)
                ));
    }
    
    /**
     * "tb1 =>
     *   FILTER: FLTTB1
     *
     * "tb2" =>
     *   FILTER: FLTTB2
     *
     * tb3" =>
     *   FILTER: FLTTB3
     * @throws Exception
     */
    @Test
    public void test13() throws Exception {
        RulesCollection rules = createRulesFromResource("rules-test.txt", true);
        Term t1 = new Term(null, "tb1");
        Term tx = new Term(null, "tbx");
        PositionSequence<InputSequenceElement> seq = new PositionSequence<>();
        seq.nextPosition();
        seq.addElement(CommonRulesRewriter.LEFT_BOUNDARY);
        seq.nextPosition();
        seq.addElement(tx);
        seq.nextPosition();
        seq.addElement(t1);
        seq.nextPosition();
        seq.addElement(CommonRulesRewriter.RIGHT_BOUNDARY);
        List<Action> actions = getActions(rules, seq);

        assertThat(actions, not(contains( 
                new Action(
                        new Instructions(1, "1",
                                Collections.singletonList(new FilterInstruction(makeQueryUsingFactory("FLTTB1")
                                ))),
                                new TermMatches(Collections.singletonList(new TermMatch(t1))), 0, 1)
                )));
    }   
    
    /**
     * "tb1 =>
     *   FILTER: FLTTB1
     *
     * "tb2" =>
     *   FILTER: FLTTB2
     *
     * tb3" =>
     *   FILTER: FLTTB3
     * @throws Exception
     */
    @Test
    public void test14() throws Exception {
        RulesCollection rules = createRulesFromResource("rules-test.txt", true);
        Term t2 = new Term(null, "tb2");
        PositionSequence<InputSequenceElement> seq = new PositionSequence<>();
        seq.nextPosition();
        seq.addElement(CommonRulesRewriter.LEFT_BOUNDARY);
        seq.nextPosition();
        seq.addElement(t2);
        seq.nextPosition();
        seq.addElement(CommonRulesRewriter.RIGHT_BOUNDARY);
        List<Action> actions = getActions(rules, seq);

        assertThat(actions, contains( 
                new Action(
                        new Instructions(1, "1",
                                Collections.singletonList(
                                        new FilterInstruction(makeQueryUsingFactory("FLTTB2"))
                                )),
                        new TermMatches(Collections.singletonList(new TermMatch(t2))), 0, 1)
                ));
    }

    /**
     * "tb1 =>
     *   FILTER: FLTTB1
     *
     * "tb2" =>
     *   FILTER: FLTTB2
     *
     * tb3" =>
     *   FILTER: FLTTB3
     * @throws Exception
     */
    @Test
    public void test15() throws Exception {
        RulesCollection rules = createRulesFromResource("rules-test.txt", true);
        Term t2 = new Term(null, "tb2");
        Term tx = new Term(null, "tbx");
        PositionSequence<InputSequenceElement> seq = new PositionSequence<>();
        seq.nextPosition();
        seq.addElement(CommonRulesRewriter.LEFT_BOUNDARY);
        seq.nextPosition();
        seq.addElement(t2);
        seq.nextPosition();
        seq.addElement(tx);
        seq.nextPosition();
        seq.addElement(CommonRulesRewriter.RIGHT_BOUNDARY);
        List<Action> actions = getActions(rules, seq);

        assertThat(actions, not(contains( 
                new Action(
                        new Instructions(1, "1",
                                Collections.singletonList(new FilterInstruction(makeQueryUsingFactory("FLTTB2")
                                ))),
                         new TermMatches(Collections.singletonList(new TermMatch(t2))), 0, 1)

                )));
    }  
    
    /**
     * "tb4*" =>
     *      FILTER: FLTTB4
     *      
     * @throws Exception
     */
    //@Test TODO enable test once we can handle wild card before right input boundary
    public void test17() throws Exception {
        RulesCollection rules = createRulesFromResource("rules-test.txt", true);
        Term t4 = new Term(null, "tb4abc");
        PositionSequence<InputSequenceElement> seq = new PositionSequence<>();
        seq.nextPosition();
        seq.addElement(CommonRulesRewriter.LEFT_BOUNDARY);
        seq.nextPosition();
        seq.addElement(t4);
        seq.nextPosition();
        seq.addElement(CommonRulesRewriter.RIGHT_BOUNDARY);
        List<Action> actions = getActions(rules, seq);

        assertThat(actions, contains( 
                new Action(
                        new Instructions(1, "1",
                                Collections.singletonList(new FilterInstruction(makeQueryUsingFactory("FLTTB4")
                                ))),
                        new TermMatches(Collections.singletonList(new TermMatch(t4))), 0, 1)
                ));
    }

    
    @Test
    public void testError01() throws Exception {
        try {
            createRulesFromResource("rules-with-errors01.txt", false);
            fail();
        } catch (RuleParseException e) {
            assertEquals("Line 5: Condition doesn't contain the term to delete: "
                    + "Term [fieldNames=null, value=c]", e.getMessage());
        }
    }

}
