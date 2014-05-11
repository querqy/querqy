/**
 * 
 */
package qp.rewrite.synonyms;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import qp.model.Term;

/**
 * @author rene
 *
 */
public class Transition {
	
	final State from;
	final State to;
	List<Term> events = new LinkedList<>();
	final boolean isInput;
	
	public Transition(State from, State to, boolean isInput) {
		this.from = from;
		this.to = to;
		this.isInput = isInput;
	}
	
	public void addEvent(Term term) {
		this.events.add(term);
	}
	
	public void addEvents(Collection<Term> terms) {
		this.events.addAll(terms);
	}

}
