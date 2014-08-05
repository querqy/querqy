/**
 * 
 */
package querqy.antlr;

import java.io.PrintWriter;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import querqy.antlr.parser.QueryLexer;
import querqy.antlr.parser.QueryParser;
import querqy.antlr.parser.QueryParser.QueryContext;
import querqy.model.Node;
import querqy.model.PrettyPrinter;
import querqy.model.Query;
/*import querqy.rewrite.QueryRewriter;
import querqy.rewrite.synonyms.SynonymRewriter;
*/
/**
 * @author rene
 *
 */
public class Runme {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// de fg +(a b)
	    
	    char[] input = "a b".toCharArray();
	    
		QueryLexer lex = new QueryLexer(new ANTLRInputStream(input, input.length));// f2:c OR +f3:d"));//"de fg +(a AND b)"));//a AND b AND (c f)"));
		CommonTokenStream tokens = new CommonTokenStream(lex);
		QueryParser parser = new QueryParser(tokens);
		
		QueryContext t = parser.query();
		Node node = t.accept(new QueryTransformerVisitor(input));
		
		PrintWriter writer = new PrintWriter(System.out);
		PrettyPrinter printer = new PrettyPrinter(writer, 4);
		printer.visit((Query) node);
		writer.flush();
		System.out.println("----");
		
//		QueryRewriter rewriter = new SynonymRewriter();
//		Query query = rewriter.rewrite((Query) node);
//		printer = new PrettyPrinter(writer, 4);
//		printer.visit(query);
//		writer.flush();
//		System.out.println();
		
		
		
	}

}
