package querqy.v2.seqhandler;

import querqy.v2.query.Instruction;

import java.util.List;

public interface QueryStateView<T> {
    SeqState<T> getSeqState();
    List<CharSequence> getSequence();
    CharSequence getCurrentTerm();
    void collectInstructionsForSeq(final List<Instruction> instructions);
    void collectInstructionsForSeq(final Instruction instruction);
}
