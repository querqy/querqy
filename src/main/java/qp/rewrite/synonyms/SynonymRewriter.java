/**
 * 
 */
package qp.rewrite.synonyms;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import qp.model.AbstractNodeVisitor;
import qp.model.BooleanClause;
import qp.model.BooleanQuery;
import qp.model.BooleanQuery.Operator;
import qp.model.DisjunctionMaxQuery;
import qp.model.Node;
import qp.model.Query;
import qp.model.SubQuery;
import qp.model.SubQuery.Occur;
import qp.model.Term;
import qp.rewrite.QueryRewriter;

/**
 * @author rene
 *
 */
public class SynonymRewriter extends AbstractNodeVisitor<Node> implements
		QueryRewriter {
	Synonyms synonyms = new Synonyms();
	
	@Override
	public Node visit(BooleanQuery booleanQuery) {
		
		List<List<Term>> buffer = new LinkedList<>();
		
		for (BooleanClause clause : booleanQuery.getClauses()) {
			
			// REVISIT: uses idx instead of start/class
			if (clause instanceof DisjunctionMaxQuery) {
				
				DisjunctionMaxQuery dmq = (DisjunctionMaxQuery) clause;
				List<Term> dmqTerms = dmq.getTerms();
				if (dmqTerms.isEmpty()) {
					buffer.clear(); // FIXME
				
				} else {
					
					List<List<Term>> newBuffer = new LinkedList<>();
					// for each term in the current dmq (= token position in the input query)
					for (Term term: dmqTerms) {
						
						// for each element in the buffer (= for each term/token chain)
						for (List<Term> bufferElement: buffer) {
						
							// append term to chain
							LinkedList<Term> newElement = new LinkedList<>(bufferElement);
							newElement.add(term);
							
							// and lookup  synonyms
							Set<List<Term>> syns = synonyms.lookup(newElement);
							if (syns != null && !syns.isEmpty()) {
								for (Term input: newElement) {
									DisjunctionMaxQuery parentDmq = (DisjunctionMaxQuery) input.getParentQuery();
									for (List<Term> replacement: syns) {
										
										switch (replacement.size()) {
										
										case 0: break;
										
										case 1: 
										{
											BooleanQuery bq = new BooleanQuery(parentDmq, Operator.AND, Occur.SHOULD);
											DisjunctionMaxQuery rDmq = new DisjunctionMaxQuery(bq, Occur.MUST);
											rDmq.addClause(replacement.get(0).clone(rDmq));
											bq.addClause(rDmq);
											bq.addClause(termsToBooleanQuery(bq, newElement, Occur.MUST_NOT));
											
											parentDmq.addClause(bq);
										}
										break;
										
										default:
											BooleanQuery bq = new BooleanQuery(parentDmq, Operator.AND, Occur.SHOULD);
											bq.addClause(termsToBooleanQuery(bq, replacement, Occur.MUST));
											bq.addClause(termsToBooleanQuery(bq, newElement, Occur.MUST_NOT));
											parentDmq.addClause(bq);
//											// single token replaced be more than one tokens --> add as Boolean AND
//											dmq.addClause(termsToBooleanQuery(replacement, Occur.SHOULD));
										}
									}
								}
							}
							
							// add new chain in buffer for next token position
							// REVISIT: need not do this at last pos
							newBuffer.add(newElement);
						}
						
						// now lookup synonym for the single term/token at this position
						List<Term> newElement = new LinkedList<>();
						newElement.add(term);
						
						Set<List<Term>> syns = synonyms.lookup(newElement);
						if (syns != null && !syns.isEmpty()) {
							for (List<Term> replacement: syns) {
								switch (replacement.size()) {
								case 0: break;
								case 1: dmq.addClause(replacement.get(0).clone(dmq)); break;
								default:
									// single token replaced be more than one tokens --> add as Boolean AND
									dmq.addClause(termsToBooleanQuery(dmq, replacement, Occur.SHOULD));
								}
							}
						}
						
						newBuffer.add(newElement);
					}
					buffer = newBuffer;
				}
				
			} else {
				buffer.clear();
				clause.accept(this);
			}
		}
		
		return booleanQuery;
	}
	
	public BooleanQuery termsToBooleanQuery(SubQuery<?> parentQuery, List<Term> terms, Occur bqOccur) {
		if (terms.size() < 2) {
			throw new IllegalArgumentException("At least two operands expected for BooleanQuery: " + terms);
		}
		BooleanQuery bq = new BooleanQuery(parentQuery, Operator.AND, bqOccur);
		for (Term newTerm: terms) {
			DisjunctionMaxQuery newDmq = new DisjunctionMaxQuery(bq, Occur.MUST);
			newDmq.addClause(newTerm.clone(newDmq));
			bq.addClause(newDmq);
		}
		return bq;
	}

	/* (non-Javadoc)
	 * @see qp.rewrite.QueryRewriter#rewrite(qp.model.Query)
	 */
	@Override
	public Query rewrite(Query query) {
		visit(query);
		return query;
	}

}
