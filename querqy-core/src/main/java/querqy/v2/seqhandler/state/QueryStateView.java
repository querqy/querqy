package querqy.v2.seqhandler.state;

import querqy.v2.node.CharSeqContainer;
import querqy.v2.query.Instruction;

import java.util.List;

public interface QueryStateView<T> {
    SeqState<T> getSeqState();

    List<? extends CharSeqContainer> viewSequenceBuffer();

    List<CharSequence> copySequenceFromBuffer();

    CharSequence getCurrentTerm();

    void collectInstructionsForSeq(final List<Instruction> instructions);

    void collectInstructionsForSeq(final Instruction instruction);
}
