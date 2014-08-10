/**
 * 
 */
package querqy.model;

/**
 * @author René Kriegler, @renekrie
 *
 */
abstract class AbstractNode<P extends Node> implements CloneableNode<P> {
	
	protected final P parent;
	
	protected final boolean generated;
	
	public AbstractNode(P parent, boolean isGenerated) {
		this.parent = parent;
		this.generated = isGenerated;
	}

	@Override
	public P getParent() {
		return parent;
	}

	/* (non-Javadoc)
	 * @see querqy.model.Node#isGenerated()
	 */
	@Override
	public boolean isGenerated() {
		return generated;
	}
	
	//public abstract Node clone(P newParent);

}
