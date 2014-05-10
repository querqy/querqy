/**
 * 
 */
package qp.model;

import java.io.PrintWriter;

/**
 * @author rene
 *
 */
public abstract class Clause implements Node {
	
	public enum Occur implements Node {
		
		SHOULD(""), MUST("+"), MUST_NOT("-");
		
		final String txt;
		
		Occur(String txt) {
			this.txt = txt;
		}
		
		@Override
		public String toString() {
			return txt;
		}

		@Override
		public void prettyPrint(String prefix, PrintWriter writer) {
			writer.print(prefix + toString());
			
		}
	}
	
	
	protected Occur occur = Occur.SHOULD;
	
	public Clause() {
		this(Occur.SHOULD);
	}
	
	public Clause(Occur occur) {
		this.occur = occur;
	}

}
