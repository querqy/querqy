/**
 * 
 */
package qp;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.lucene.search.BooleanQuery;

import qp.model.Node;
import qp.model.PrettyPrinter;
import qp.model.Query;
import qp.parser.QueryLexer;
import qp.parser.QueryParser;
import qp.parser.QueryParser.QueryContext;
import qp.rewrite.QueryRewriter;
import qp.rewrite.synonyms.SynonymRewriter;
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
		
		QueryRewriter rewriter = new SynonymRewriter();
		Query query = rewriter.rewrite((Query) node);
		printer = new PrettyPrinter(writer, 4);
		printer.visit(query);
		writer.flush();
		System.out.println();
		
		org.apache.lucene.search.Query lq = new LuceneQueryGenerator(query, 
				new HashSet<>(Arrays.asList("f1", "f39"))).getLuceneQuery();
		
		System.out.println(lq);
		System.out.println();
		
	}

}
