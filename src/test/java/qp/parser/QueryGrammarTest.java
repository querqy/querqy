package qp.parser;

import static org.junit.Assert.*;

import java.util.Collection;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.xpath.XPath;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import static org.hamcrest.Matchers.*;

import org.junit.Test;


public class QueryGrammarTest {
	
	
	QueryParser getParser(String input) {
		
		QueryLexer lex = new QueryLexer(new ANTLRInputStream(input));
		CommonTokenStream tokens = new CommonTokenStream(lex);
		return new QueryParser(tokens);
		
	}
	
	class TextMatcher extends TypeSafeMatcher<ParseTree> {
	    
	    final String expected;
	    
	    TextMatcher(String expected) {
	        this.expected = expected;
	    }

        @Override
        public void describeTo(Description description) {
           description.appendText("expected text: " + expected);
            
        }

        @Override
        protected boolean matchesSafely(ParseTree item) {
            return expected.equals(item.getText());
        }
	    
	}
	
	TextMatcher text(String expected) {
	    return new TextMatcher(expected);
	}
	

	@Test
	public void testSingleTermQuery() {
		
		QueryParser parser = getParser("termOne");
		
		Collection<ParseTree> trees = XPath.findAll(parser.query(), "//termQuery", parser);
		assertThat(trees, hasSize(1));
		assertThat(trees , hasItem(text("termOne")));
	    
		
	}
	
	@Test
    public void testUnicode() throws Exception {
	    QueryParser parser = getParser("törmÈ ዩኒኮድ ምንድን ነው? ดินสอ 铅笔 قلم עפרון");
	    
        
        Collection<ParseTree> trees = XPath.findAll(parser.query(), "//termQuery", parser);
        assertThat(trees, hasSize(8));
        assertThat(trees , hasItem(text("törmÈ")));
        assertThat(trees , hasItem(text("ዩኒኮድ")));
        assertThat(trees , hasItem(text("ምንድን")));
        assertThat(trees , hasItem(text("ነው?")));
        assertThat(trees , hasItem(text("ดินสอ")));
        assertThat(trees , hasItem(text("铅笔")));
        assertThat(trees , hasItem(text("עפרון")));
        assertThat(trees , hasItem(text("قلم")));
        
    }

}
