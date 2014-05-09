/**
 * 
 */
package qp.model;

/**
 * @author rene
 *
 */
public class TermQuery extends BooleanClause {
	
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
	public void prettyPrint(String prefix) {
		System.out.println(prefix + "TQ " + occur + " " + text);
		
	}

	

}
