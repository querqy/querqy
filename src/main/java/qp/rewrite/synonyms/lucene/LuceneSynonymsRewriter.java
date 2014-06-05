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
		
		
//		List<List<Term>> sequence = new LinkedList<>();
//		
//		for (BooleanClause clause : booleanQuery.getClauses()) {
//			// REVISIT: uses idx instead of start/class
//			if (clause instanceof DisjunctionMaxQuery) {
//				DisjunctionMaxQuery dmq = (DisjunctionMaxQuery) clause;
//				
//				List<Term> dmqTerms = dmq.getTerms();
//				if (dmqTerms.isEmpty()) {
//					sequence.clear(); // FIXME - visit clauses of different types
//				} else {
//					
//				}
//			}
//		}
		
		return null;
	}
	
	
	// Interleaves all output tokens onto the futureOutputs:
	/* private void addOutput(BytesRef bytes) {
	    bytesReader.reset(bytes.bytes, bytes.offset, bytes.length);

	    final int code = bytesReader.readVInt();
	    final boolean keepOrig = (code & 0x1) == 0;
	    System.out.println(keepOrig);
	    final int count = code >>> 1;
	    
	    //System.out.println("  addOutput count=" + count + " keepOrig=" + keepOrig);
	    for (int outputIDX=0; outputIDX<count; outputIDX++) {
	    	
	    	synonymMap.words.get(bytesReader.readVInt(),
	                         scratchBytes);
	      //System.out.println("    outIDX=" + outputIDX + " bytes=" + scratchBytes.length);
	      UnicodeUtil.UTF8toUTF16(scratchBytes, scratchChars);
	      System.out.println(scratchChars);
	      
	    
	    }

	  }*/
	
	
	public static void main(String[] args) throws Exception {
		/*SolrSynonymsRewriterFactory factory = new SolrSynonymsRewriterFactory(SolrSynonymsRewriter.class.getClassLoader().getResourceAsStream("synonyms.txt")) ;
		SolrSynonymsRewriter rewriter = (SolrSynonymsRewriter) factory.getRewriter();
		SynonymMap map = factory.synonymMap;//rewriter.synonymMap;
		char[] buffer = "aa c".toCharArray();
		
		FST.Arc<BytesRef> scratchArc = new FST.Arc<BytesRef>();
		 map.fst.getFirstArc(scratchArc);
		System.out.println(scratchArc);
		BytesRef pendingOutput = map.fst.outputs.getNoOutput();
		
		FST.BytesReader fstReader = map.fst.getBytesReader();
		
		boolean ok = true;
		int pos = 0;
		while (ok &&  pos < buffer.length) {
			int codePoint = Character.codePointAt(buffer, pos, buffer.length);
			ok = null != map.fst.findTargetArc(codePoint, scratchArc, scratchArc, fstReader);
			
			pendingOutput = map.fst.outputs.add(pendingOutput, scratchArc.output);
			
			pos += Character.charCount(pos);
		}
		if (ok) {
			if (scratchArc.isFinal()) {
				rewriter.addOutput(
						map.fst.outputs.add(pendingOutput, scratchArc.nextFinalOutput));
//				System.out.println(scratchArc);
//				System.out.println(scratchArc.output.toString());
			}
		}
		
		*/
	}

}
