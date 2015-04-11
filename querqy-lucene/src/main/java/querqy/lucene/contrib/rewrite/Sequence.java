/**
 * 
 */
package querqy.lucene.contrib.rewrite;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.store.ByteArrayDataInput;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.UnicodeUtil;
import org.apache.lucene.util.fst.FST;

import querqy.SimpleComparableCharSequence;
import querqy.model.BooleanQuery;
import querqy.model.DisjunctionMaxClause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.Term;
import querqy.model.Clause.Occur;

/**
 * @author rene
 *
 */
class Sequence {

   final FST.Arc<BytesRef> arc;
   final List<Term> terms;
   final BytesRef output;

   public Sequence(FST.Arc<BytesRef> arc, List<Term> terms, BytesRef output) {
      this.arc = arc;
      this.terms = terms;
      this.output = output;
   }

   public void addOutputs(Map<DisjunctionMaxQuery, Set<DisjunctionMaxClause>> addenda, SynonymMap map,
         ByteArrayDataInput bytesReader) {

      BytesRef finalOutput = map.fst.outputs.add(output, arc.nextFinalOutput);

      bytesReader.reset(finalOutput.bytes, finalOutput.offset, finalOutput.length);

      BytesRef scratchBytes = new BytesRef();
      CharsRef scratchChars = new CharsRef();

      final int code = bytesReader.readVInt();
      // final boolean keepOrig = (code & 0x1) == 0;
      final int count = code >>> 1;

      // iterate over all possible outputs
      for (int outputIDX = 0; outputIDX < count; outputIDX++) {

         map.words.get(bytesReader.readVInt(), scratchBytes);
         UnicodeUtil.UTF8toUTF16(scratchBytes, scratchChars);

         boolean replacementIsMultiTerm = false;
         // ignore ' ' at beginning and end
         for (int i = 1; i < scratchChars.length - 1 && !replacementIsMultiTerm; i++) {
            replacementIsMultiTerm = scratchChars.charAt(i) == ' ';
         }

         // iterate through all input terms
         for (Term term : terms) {

            // FIXME fix parent type of term to always DMQ?
            DisjunctionMaxQuery currentDmq = (DisjunctionMaxQuery) term.getParent();

            BooleanQuery add = new BooleanQuery(currentDmq, Occur.SHOULD, true);

            if (replacementIsMultiTerm) {
               BooleanQuery replaceSeq = new BooleanQuery(add, Occur.MUST, true);

               int start = 0;
               for (int i = 0; i < scratchChars.length; i++) {
                  if (scratchChars.charAt(i) == ' ' && (i > start)) {
                     DisjunctionMaxQuery newDmq = new DisjunctionMaxQuery(replaceSeq, Occur.MUST, true);
                     newDmq.addClause(
                           new Term(newDmq,
                                 new SimpleComparableCharSequence(scratchChars.chars, start, i - start)));
                     replaceSeq.addClause(newDmq);
                     start = i + 1;
                  }
               }

               if (start < scratchChars.length) {
                  DisjunctionMaxQuery newDmq = new DisjunctionMaxQuery(replaceSeq, Occur.MUST, true);
                  newDmq.addClause(new Term(newDmq, new SimpleComparableCharSequence(scratchChars.chars, start,
                        scratchChars.length - start)));
                  replaceSeq.addClause(newDmq);
               }

               add.addClause(replaceSeq);

            } else {

               DisjunctionMaxQuery replaceDmq = new DisjunctionMaxQuery(add, Occur.MUST, true);
               replaceDmq.addClause(new Term(replaceDmq, new SimpleComparableCharSequence(scratchChars.chars, 0,
                     scratchChars.length)));
               add.addClause(replaceDmq);
            }

            BooleanQuery neq = new BooleanQuery(add, Occur.MUST_NOT, true);

            for (Term negTerm : terms) {
               DisjunctionMaxQuery neqDmq = new DisjunctionMaxQuery(neq, Occur.MUST, true);
               neqDmq.addClause(negTerm.clone(neqDmq, true));
               neq.addClause(neqDmq);
            }

            add.addClause(neq);

            Set<DisjunctionMaxClause> adds = addenda.get(currentDmq);
            if (adds == null) {
               adds = new LinkedHashSet<>();
               addenda.put(currentDmq, adds);
            }

            adds.add(add);

         }

      }
   }

}
