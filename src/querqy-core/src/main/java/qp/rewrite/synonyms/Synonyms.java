/**
 * 
 */
package qp.rewrite.synonyms;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import querqy.model.Term;

/**
 * @author rene
 *
 */
public class Synonyms {
	
	final Map<List<Term>, Set<List<Term>>> dictionary;
	
	public Synonyms() {
		dictionary = new HashMap<List<Term>, Set<List<Term>>>();
		dictionary.put(
				Arrays.asList(new Term(null, "a".toCharArray()), new Term(null, "b".toCharArray())), 
				new HashSet<List<Term>>(
						Arrays.asList(asList(new Term(null, "j".toCharArray()), new Term(null, "k".toCharArray())))
						));
		
//		dictionary.put(Arrays.asList(new Term("d")), new HashSet<List<Term>>(Arrays.asList(asList(new Term("l")))));
//		dictionary.put(Arrays.asList(new Term("a")), new HashSet<List<Term>>(Arrays.asList(asList(new Term("m"), new Term("n")))));
		
	}
	
	public Set<List<Term>> lookup(List<Term> input) {
		System.out.println("Looking up: " + input);
		return dictionary.get(input);
	}
	
	Set<List<Term>> makeValues(List<Term> ...values) {
		Set<List<Term>> result = new HashSet<>();
		result.addAll(Arrays.asList(values));
		return result;
	}

	List<Term> asList(Term... terms) {
		return Arrays.asList(terms);
	}
}
