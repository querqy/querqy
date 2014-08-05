package querqy.antlr.rewrite.commonrules;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import querqy.model.Term;
import querqy.rewrite.commonrules.model.Action;
import querqy.rewrite.commonrules.model.DeleteInstruction;
import querqy.rewrite.commonrules.model.Instruction;
import querqy.rewrite.commonrules.model.Instructions;
import querqy.rewrite.commonrules.model.RulesCollection;
import querqy.rewrite.commonrules.model.TermPositionSequence;

public class SimpleParserTest extends AbstractCommonRulesTest {
    
    Reader reader; 

    @Before
    public void setUp() throws Exception {
    }
    
    SimpleCommonRulesParser createParserWithEmptyReader() {
        reader = new StringReader("");
        return new SimpleCommonRulesParser(reader);
    }
    
    SimpleCommonRulesParser createParserFromResource(String resourceName) {
        reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(resourceName));
        return new SimpleCommonRulesParser(reader);
    }
    
    RulesCollection createRulesFromResource(String resourceName) throws IOException, RuleParseException {
        SimpleCommonRulesParser parser = createParserFromResource(resourceName);
        return parser.parse();
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
        RulesCollection rules = createRulesFromResource("rules-test.txt");
        Term t1 = new Term(null, "aa");
        Term t2 = new Term(null, "l");
        TermPositionSequence seq = new TermPositionSequence();
        seq.nextPosition();
        seq.addTerm(t1);
        seq.nextPosition();
        seq.addTerm(t2);
        List<Action> actions = rules.getRewriteActions(seq);
        assertThat(actions, contains( 
                new Action(
                        Arrays.asList(
                                new Instructions(Arrays.asList((Instruction) new DeleteInstruction(Arrays.asList(mkTerm("aa")))))), 
                                Arrays.asList(t1), 0, 1)));
    }
    
    @Test
    public void test02() throws Exception {
        RulesCollection rules = createRulesFromResource("rules-test.txt");
        Term t1 = new Term(null, "a");
        Term t2 = new Term(null, "b");
        Term t3 = new Term(null, "c");
        Term t4 = new Term(null, "l");
        TermPositionSequence seq = new TermPositionSequence();
        seq.nextPosition();
        seq.addTerm(t1);
        seq.nextPosition();
        seq.addTerm(t2);
        seq.nextPosition();
        seq.addTerm(t3);
        seq.nextPosition();
        seq.addTerm(t4);
        List<Action> actions = rules.getRewriteActions(seq);
        assertThat(actions, contains( 
                new Action(
                        Arrays.asList(
                                new Instructions(
                                        Arrays.asList(
                                                (Instruction) new DeleteInstruction(Arrays.asList(mkTerm("b")))))), 
                                Arrays.asList(t1, t2), 0, 2),
                                
                new Action(
                        Arrays.asList(
                                new Instructions(
                                        Arrays.asList(
                                                (Instruction) new DeleteInstruction(Arrays.asList(mkTerm("a"))),
                                                (Instruction) new DeleteInstruction(Arrays.asList(mkTerm("c")))
                                                        ))), 
                                Arrays.asList(t1, t2, t3), 0, 3)
                
                
                ));
    }
    
    @Test
    public void testError01() throws Exception {
        try {
            createRulesFromResource("rules-with-errors01.txt");
            fail();
        } catch (RuleParseException e) {
            assertEquals("Line 5: Condition doesn't contain the term to delete: "
                    + "Term [fieldNames=null, value=c]", e.getMessage());
        }
    }
    

}
