/**
 * 
 */
package querqy.rewrite.lucene;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.store.ByteArrayDataInput;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.UnicodeUtil;
import org.apache.lucene.util.fst.FST;

import querqy.model.BooleanQuery;
import querqy.model.DisjunctionMaxClause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.Term;
import querqy.model.SubQuery.Occur;

/**
 * @author rene
 *
 */
public class Sequences {
	
	final FST<BytesRef> fst;
	
	Map<DisjunctionMaxQuery, Set<DisjunctionMaxClause>> addenda = new HashMap<DisjunctionMaxQuery, Set<DisjunctionMaxClause>>();
	
	Set<Sequence> sequences = new LinkedHashSet<>();
	Set<Sequence> addSequences = new LinkedHashSet<>();
	
	private DisjunctionMaxQuery currentDmq = null;
	ByteArrayDataInput bytesReader = new ByteArrayDataInput();
	private final SynonymMap map;
	BytesRef scratchBytes = new BytesRef();
	
	public Sequences(SynonymMap map) {
		this.fst = map.fst;
		this.map = map;
	}
	
	public void apply() {
		for (Map.Entry<DisjunctionMaxQuery, Set<DisjunctionMaxClause>> entry : addenda.entrySet()) {
			DisjunctionMaxQuery dmq = entry.getKey();
			for (DisjunctionMaxClause clause: entry.getValue()) {
				dmq.addClause(clause);
			}
		}
		addenda.clear();
	}
	
	public void nextPosition(DisjunctionMaxQuery dmq) {
		currentDmq = dmq;
		sequences = addSequences;
		addSequences = new LinkedHashSet<>();
	}
	
	static final int cpBlank = Character.codePointAt(new char[] {' '}, 0);
	static final int lBlank = Character.charCount(cpBlank);
	
	public void appendToSequences(Term term) throws IOException {
	    
	    FST.BytesReader fstReader = fst.getBytesReader();
	    FST.Arc<BytesRef> scratchArc = new FST.Arc<BytesRef>();
	    
	    boolean ok = true;
	    
	    for (Sequence sequence: sequences) {
	        
	        // try to append a blank after the sequence
	        ok = null != fst.findTargetArc(cpBlank, sequence.arc, scratchArc, fstReader);
	        if (ok) {
	            // pending contains sequence + ' ' now
	            BytesRef pendingOutput = fst.outputs.add(sequence.output, scratchArc.output);

	            // iterate over term chars and try to append them to the sequence
	            int pos = 0;
	            while (ok &&  pos < term.length) {
	                int codePoint = term.codePointAt(pos); 
	                ok = null != fst.findTargetArc(codePoint, scratchArc, scratchArc, fstReader);
	                
	                pendingOutput = fst.outputs.add(pendingOutput, scratchArc.output);
	                
	                pos += Character.charCount(codePoint);
	            }
	            
	            if (ok) {
	                // ok means that we could consume the complete term char buffer, thus,
	                // append it to the sequence
	                List<Term> terms = new LinkedList<>(sequence.terms);
	                terms.add(term);
	                FST.Arc<BytesRef> arc = new  FST.Arc<>();
	                Sequence newSequence = new Sequence(arc.copyFrom(scratchArc), terms, pendingOutput);
	                addSequences.add(newSequence);
	                // however, it might not have consumed the complete dictionary lookup key (it might
	                // complete at the next term position)
	                if (scratchArc.isFinal()) {
	                    // the term completes the lookup key --> output the dictionary values
	                    newSequence.addOutputs(addenda, map, bytesReader);
	                }
	            }
	        }
	    }
	}
	
	public void putTerm(Term term) throws IOException {
	    appendToSequences(term);
		FST.Arc<BytesRef> scratchArc = new FST.Arc<BytesRef>();
		fst.getFirstArc(scratchArc);
		BytesRef pendingOutput = fst.outputs.getNoOutput();
		
		FST.BytesReader fstReader = fst.getBytesReader();
		//char[] buffer = term.getValue().toCharArray();
		
		boolean ok = true;
		int pos = 0;
		while (ok &&  pos < term.length) {
			int codePoint = term.codePointAt(pos);//Character.codePointAt(buffer, pos, buffer.length);
			ok = null != fst.findTargetArc(codePoint, scratchArc, scratchArc, fstReader);
			
			pendingOutput = fst.outputs.add(pendingOutput, scratchArc.output);
			
			pos += Character.charCount(codePoint);
		}
		if (ok) {
		    
		    List<Term> terms = Arrays.asList(term);
            FST.Arc<BytesRef> arc = new  FST.Arc<>();
            addSequences.add(new Sequence(arc.copyFrom(scratchArc), terms, pendingOutput));
            
			if (scratchArc.isFinal()) {
				addOutput(fst.outputs.add(pendingOutput, scratchArc.nextFinalOutput), term);
			}
			
		}
		
	}
	
	
    private void addOutput(BytesRef bytes, Term rewriteSource) {
        
    	bytesReader.reset(bytes.bytes, bytes.offset, bytes.length);

		final int code = bytesReader.readVInt();
		 //final boolean keepOrig = (code & 0x1) == 0;
		final int count = code >>> 1;
		 
		Set<DisjunctionMaxClause> adds = addenda.get(currentDmq);
		if (adds == null) {
			adds = new LinkedHashSet<>();
			addenda.put(currentDmq, adds);
		}
		 
		    
		for (int outputIDX = 0; outputIDX < count; outputIDX++) {
		    
		    // not re-using scratchChars globally -> would have to copy to Terms anyway
		    CharsRef scratchChars = new CharsRef();
		    
		    map.words.get(bytesReader.readVInt(), scratchBytes);
		    UnicodeUtil.UTF8toUTF16(scratchBytes, scratchChars);
		        
		    BooleanQuery add = null;
		        
		    int start = 0;
		    for (int i = 0; i < scratchChars.length; i++) {
		    	if (scratchChars.charAt(i) == ' ' && (i > start)) {
		        	if (add == null) {
		        		add = new BooleanQuery(currentDmq, Occur.SHOULD, true);
		       		}
		       		DisjunctionMaxQuery newDmq = new DisjunctionMaxQuery(add, Occur.MUST, true);
		       		newDmq.addClause(new Term(newDmq, scratchChars.chars, start, i - start, rewriteSource));
		       		add.addClause(newDmq);
		       		start = i + 1;
		        }
		    }
		    
		    if (add != null) {
		    	if (start < scratchChars.length) {
		    		DisjunctionMaxQuery newDmq = new DisjunctionMaxQuery(add, Occur.MUST, true);
		        	newDmq.addClause(new Term(newDmq, scratchChars.chars, start, scratchChars.length - start, rewriteSource));
		        	add.addClause(newDmq);
		        }
		       	adds.add(add);
		    } else {
		       	adds.add(new Term(currentDmq, scratchChars.chars, 0, scratchChars.length, rewriteSource));
		    }
		    
		 }

	 }

}
