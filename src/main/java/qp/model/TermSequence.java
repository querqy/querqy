/**
 * 
 */
package qp.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author rene
 *
 */
public class TermSequence {
	
	final String fieldName;
	final int start;
	final List<Term> terms;
	
	public TermSequence(String fieldName, int start) {
		this.fieldName = fieldName;
		this.start = start;
		this.terms = new ArrayList<>();
	}
	
	public void addTerm(Term term) {
		terms.add(term);
	}

	public String getFieldName() {
		return fieldName;
	}

	public int getStart() {
		return start;
	}

	public List<Term> getTerms() {
		return terms;
	}
	
	

}
