/**
 * 
 */
package querqy.model;


/**
 * @author René Kriegler, @renekrie
 *
 */
public abstract class Clause<P extends Node> extends AbstractNode<P> {
	
	public enum Occur {
		
		SHOULD(""), MUST("+"), MUST_NOT("-");
		
		final String txt;
		
		Occur(String txt) {
			this.txt = txt;
		}
		
		@Override
		public String toString() {
			return txt;
		}

	}
	
	public final Occur occur;
	
	
	public Clause(P parent, Occur occur, boolean isGenerated) {
		super(parent, isGenerated);
		this.occur = occur;
	}

	public Occur getOccur() {
		return occur;
	}

	
	
}
