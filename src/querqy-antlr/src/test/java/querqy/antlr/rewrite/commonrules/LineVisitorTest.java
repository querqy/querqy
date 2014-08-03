package querqy.antlr.rewrite.commonrules;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.*;

import java.util.Arrays;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Before;
import org.junit.Test;

import querqy.antlr.commonrules.SimpleParserLexer;
import querqy.antlr.commonrules.SimpleParserParser;
import querqy.antlr.commonrules.SimpleParserParser.LineContext;
import querqy.rewrite.commonrules.model.DeleteInstruction;
import querqy.rewrite.commonrules.model.Input;

public class LineVisitorTest extends AbstractCommonRulesTest {

    @Before
    public void setUp() throws Exception {
    }
    
    protected Object runVisitor(String line, Input prevInput) {
        char[] inputChars = line.toCharArray();
        
        SimpleParserLexer lex = new SimpleParserLexer(new ANTLRInputStream(inputChars, inputChars.length));
        CommonTokenStream tokens = new CommonTokenStream(lex);
        SimpleParserParser parser = new SimpleParserParser(tokens);
        
        LineContext lineContext = parser.line();
        return lineContext.accept(new LineVisitor(inputChars, prevInput));

    }
    
    protected <T> T runAndExpectClass(String line, Class<T> clazz) {
        return runAndExpectClass(line, clazz, null);
    }
    
    @SuppressWarnings("unchecked")
    protected <T> T runAndExpectClass(String line, Class<T> clazz, Input prevInput) {
        Object result = runVisitor(line, prevInput);
        assertNotNull(result);
        assertTrue(clazz.isAssignableFrom(result.getClass()));
        return (T) result;
    }

    @Test
    public void testInputSingleToken() {
        Input input = runAndExpectClass("a =>", Input.class);
        assertThat(input.getInputTerms(), contains(mkTerm("a")));
        
        input = runAndExpectClass("a  =>", Input.class);
        assertThat(input.getInputTerms(), contains(mkTerm("a")));
        
        input = runAndExpectClass("a=>", Input.class);
        assertThat(input.getInputTerms(), contains(mkTerm("a")));
        
        input = runAndExpectClass(" aacd =>", Input.class);
        assertThat(input.getInputTerms(), contains(mkTerm("aacd")));
    }
    
    @Test
    public void testInputTwoTokens() {
        Input input = runAndExpectClass("a a =>", Input.class);
        assertThat(input.getInputTerms(), contains(mkTerm("a"), mkTerm("a")));
        
        input = runAndExpectClass(" aacd dcf =>", Input.class);
        assertThat(input.getInputTerms(), contains(mkTerm("aacd"), mkTerm("dcf")));
    }
    
    @Test
    public void testInputTwoTokensOneWithField() {
        Input input = runAndExpectClass("a x:b =>", Input.class);
        assertThat(input.getInputTerms(), contains(mkTerm("a"), mkTerm("b", "x")));
        
        input = runAndExpectClass("a {x,y,z}:b =>", Input.class);
        assertThat(input.getInputTerms(), contains(mkTerm("a"), mkTerm("b", "x", "y", "z")));
        
        input = runAndExpectClass("y:aacd b =>", Input.class);
        assertThat(input.getInputTerms(), contains(mkTerm("aacd", "y"), mkTerm("b")));
        
        input = runAndExpectClass("{y, z}:aacd b =>", Input.class);
        assertThat(input.getInputTerms(), contains(mkTerm("aacd", "y", "z"), mkTerm("b")));
    }

    @Test
    public void testInputTwoTokensWithField() {
        Input input = runAndExpectClass("y:a x:b =>", Input.class);
        assertThat(input.getInputTerms(), contains(mkTerm("a", "y"), mkTerm("b", "x")));
        
        input = runAndExpectClass("{y,k}:a {x,l}:b =>", Input.class);
        assertThat(input.getInputTerms(), contains(mkTerm("a", "y", "k"), mkTerm("b", "x", "l")));
        
        input = runAndExpectClass("y:(aacd b) =>", Input.class);
        assertThat(input.getInputTerms(), contains(mkTerm("aacd", "y"), mkTerm("b", "y")));
        
        input = runAndExpectClass("{y,z}:(aacd b) =>", Input.class);
        assertThat(input.getInputTerms(), contains(mkTerm("aacd", "y", "z"), mkTerm("b", "y", "z")));
    }
    
    @Test
    public void testDeleteDeclaredSingleTerm() throws Exception {
        DeleteInstruction delete = runAndExpectClass("DELETE: ab", DeleteInstruction.class);
        assertThat(delete.getTermsToDelete(), contains(mkTerm("ab")));
        
    }
    
    @Test
    public void testDeleteDeclaredTermsWithFieldName() throws Exception {
        DeleteInstruction delete = runAndExpectClass("DELETE: x:ab cd", DeleteInstruction.class);
        assertThat(delete.getTermsToDelete(), contains(mkTerm("ab", "x"), mkTerm("cd")));
        
        delete = runAndExpectClass("DELETE: x:ab y:cd", DeleteInstruction.class);
        assertThat(delete.getTermsToDelete(), contains(mkTerm("ab", "x"), mkTerm("cd", "y")));
        
        delete = runAndExpectClass("DELETE: x:(ab cd)", DeleteInstruction.class);
        assertThat(delete.getTermsToDelete(), contains(mkTerm("ab", "x"), mkTerm("cd", "x")));
        
        delete = runAndExpectClass("delete: {y,z}:(aacd b)", DeleteInstruction.class);
        assertThat(delete.getTermsToDelete(), contains(mkTerm("aacd", "y", "z"), mkTerm("b", "y", "z")));
        
    }
    
    @Test
    public void testDeleteInputTerms() throws Exception {
        Input input = new Input(Arrays.asList(mkTerm("a")));
        DeleteInstruction delete = runAndExpectClass("delete", DeleteInstruction.class, input);
        assertThat(delete.getTermsToDelete(), contains(mkTerm("a")));
        
        input = new Input(Arrays.asList(mkTerm("a"), mkTerm("b", "x", "y")));
        delete = runAndExpectClass("DELETE:", DeleteInstruction.class, input);
        assertThat(delete.getTermsToDelete(), contains(mkTerm("a"), mkTerm("b", "x", "y")));
    }
    
    
}
