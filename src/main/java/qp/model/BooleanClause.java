/**
 * 
 */
package qp.model;

/**
 * @author rene
 *
 */
public abstract class BooleanClause implements Node {
	
	public enum Occur implements Node {
		SHOULD, MUST, MUST_NOT;

		@Override
		public void prettyPrint(String prefix) {
			System.out.print(this + " ");
			
		}
	}
	
	protected Occur occur = Occur.SHOULD;
	
	public BooleanClause() {
		this(Occur.SHOULD);
	}
	
	public BooleanClause(Occur occur) {
		this.occur = occur;
	}

	public Occur getOccur() {
		return occur;
	}

	public void setOccur(Occur occur) {
		this.occur = occur;
	}

}
