/**
 * 
 */
package qp.model;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

/**
 * @author rene
 *
 */
public class Query extends BooleanQuery {
	
	public Query() {
		super(Operator.NONE, Occur.SHOULD);
	}
	
	@Override
	public void prettyPrint(String prefix, PrintWriter writer) {
		writer.print(prefix);
		writer.println("Q: " + operator + "(");
		for (Clause clause: clauses) {
			clause.prettyPrint(prefix + prefix, writer);
		}
		writer.println(prefix + ")");
		
	}
	
	
}
