/**
 * 
 */
package qp.model;

import java.io.PrintWriter;

/**
 * @author rene
 *
 */
public class TermQuery extends Clause {
	
	protected String text;
	
	
	public TermQuery(String text, Occur occur) {
		super(occur);
		this.text = text;
	}
	
	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@Override
	public String toString() {
		return "TermQuery [occur=" + occur + ", text=" + text +"]";
	}

	@Override
	public void prettyPrint(String prefix, PrintWriter writer) {
		writer.println(prefix + occur + "TQ " + text);
		
	}

	

}
