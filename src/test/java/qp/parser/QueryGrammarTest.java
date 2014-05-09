package qp.parser;

import static org.junit.Assert.*;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.xpath.XPath;
import org.junit.Test;

import qp.parser.QueryParser.QueryContext;

public class QueryGrammarTest {
	
	
	QueryParser getParser(String input) {
		
		QueryLexer lex = new QueryLexer(new ANTLRInputStream(input));
		CommonTokenStream tokens = new CommonTokenStream(lex);
		return new QueryParser(tokens);
		
	}
	
	
	
	

	@Test
	public void testSingleTermQuery() {
		
		QueryParser parser = getParser("termOne");
	    
		for (ParseTree t : XPath.findAll(parser.query(), "//termQuery", parser)) {
			
			System.out.println(t.getText());
		}
	}

}
