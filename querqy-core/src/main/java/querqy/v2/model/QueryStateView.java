package querqy.v2.model;

import java.util.List;

public interface QueryStateView<T> {
    SeqState<T> getSeqState();
    List<CharSequence> getSequence();
    CharSequence getCurrentTerm();
    void collectInstructionsForSeq(final List<Instruction> instructions);
    void collectInstructionsForSeq(final Instruction instruction);
}
