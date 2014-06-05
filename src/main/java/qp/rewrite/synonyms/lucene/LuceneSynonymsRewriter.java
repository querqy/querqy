/**
 * 
 */
package qp.rewrite.synonyms.lucene;

import java.io.IOException;
import java.util.LinkedList;

import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.store.ByteArrayDataInput;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CharsRef;

import qp.model.AbstractNodeVisitor;
import qp.model.BooleanQuery;
import qp.model.DisjunctionMaxQuery;
import qp.model.Node;
import qp.model.Query;
import qp.model.Term;
import qp.rewrite.QueryRewriter;

/**
 * @author rene
 *
 */
public class LuceneSynonymsRewriter extends AbstractNodeVisitor<Node>  implements QueryRewriter {
	
	final SynonymMap synonymMap; 
    ByteArrayDataInput bytesReader = new ByteArrayDataInput();
    BytesRef scratchBytes = new BytesRef();
    CharsRef scratchChars = new CharsRef();
    
    LinkedList<Sequences> sequencesStack = new LinkedList<>();
	
	public LuceneSynonymsRewriter(SynonymMap synonymMap) {
		this.synonymMap = synonymMap;
	}

	/* (non-Javadoc)
	 * @see qp.rewrite.QueryRewriter#rewrite(qp.model.Query)
	 */
	@Override
	public Query rewrite(Query query) {
		visit(query);
		return query;
	}
	
	@Override
	public Node visit(DisjunctionMaxQuery disjunctionMaxQuery) {
		sequencesStack.getLast().nextPosition(disjunctionMaxQuery);
		return super.visit(disjunctionMaxQuery);
	}
	
	@Override
	public Node visit(Term term) {
		try {
			sequencesStack.getLast().putTerm(term);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return super.visit(term);
	}
	
	@Override
	public Node visit(BooleanQuery booleanQuery) {
		
		if ((!sequencesStack.isEmpty()) && booleanQuery.getParentQuery() instanceof BooleanQuery) {
			// left-hand siblings might be DMQs with Terms - terminate sequences
			sequencesStack.getLast().apply();
		}
		
		// new Sequences object for child DMQ/Term objects 
		sequencesStack.add(new Sequences(synonymMap));
		
		super.visit(booleanQuery);
		
		sequencesStack.removeLast().apply();
		
		return null;
	}
	

}
