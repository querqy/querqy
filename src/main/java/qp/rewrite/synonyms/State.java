/**
 * 
 */
package qp.rewrite.synonyms;

import java.util.LinkedList;
import java.util.List;

/**
 * @author rene
 *
 */
public class State {
	
	final int index;
	final List<Transition> outGoing;
	
	public State(int index) {
		this(index, new LinkedList<Transition>());
	}
	
	
	public State(int index, List<Transition> outGoingTransitions) {
		this.index = index;
		this.outGoing = outGoingTransitions;
	}
	
	public boolean isAdded() {
		return index < 0;
	}

}
