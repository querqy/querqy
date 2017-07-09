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
	
	
	public Clause(final P parent, final Occur occur, final boolean isGenerated) {
		super(parent, isGenerated);
		this.occur = occur;
	}

	public Occur getOccur() {
		return occur;
	}

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((occur == null) ? 0 : occur.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Clause<?> other = (Clause<?>) obj;
        if (occur != other.occur)
            return false;
        return true;
    }

}
