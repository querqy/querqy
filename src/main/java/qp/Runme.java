/**
 * 
 */
package qp;

import java.util.LinkedList;

import qp.model.BooleanClause;
import qp.model.BooleanClause.Occur;
import qp.model.BooleanQuery;
import qp.model.Node;
import qp.parser.QueryBaseListener;
import qp.parser.QueryBaseVisitor;
import qp.parser.QueryLexer;
import qp.parser.QueryParser;
import qp.parser.QueryParser.BooleanClauseContext;
import qp.parser.QueryParser.BooleanPrefixContext;
import qp.parser.QueryParser.QueryContext;
import qp.parser.QueryParser.TermQueryContext;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;
import org.antlr.v4.runtime.tree.Tree;
/**
 * @author rene
 *
 */
public class Runme {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		QueryLexer lex = new QueryLexer(new ANTLRInputStream("a AND b AND (c f)"));
		CommonTokenStream tokens = new CommonTokenStream(lex);
		QueryParser parser = new QueryParser(tokens);
		
		//QueryTransformerListener listener = new QueryTransformerListener();
		//parser.addParseListener(listener);
		
		QueryContext t = parser.query();
		//System.out.println(listener.getQuery());
//		QueryBaseVisitor<Object> visitor = new QueryBaseVisitor<Object>() {
//			@Override
//			public Object visitTermSequence(TermSequenceContext ctx) {
//				// TODO Auto-generated method stub
//				return super.visitTermSequence(ctx);
//			}
//			public Object visitPhrase(qp.parser.QueryParser.PhraseContext ctx) {
//				for (TermContext tctx : ctx.termSequence().term()) {
//					System.out.println(tctx.getText());
//				}
//				return "NONE";
//			};
//		};
//		t.accept(visitor);
		Node node = t.accept(new QueryTransformerVisitor());
		node.prettyPrint("   ");
		System.out.println();

	}

}
