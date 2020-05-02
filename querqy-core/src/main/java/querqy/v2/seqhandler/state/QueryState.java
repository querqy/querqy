package querqy.v2.seqhandler.state;

import querqy.v2.node.Node;
import querqy.v2.node.NodeSeqBuffer;
import querqy.v2.query.Instruction;
import querqy.v2.query.QueryModification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class QueryState<T> implements QueryStateView<T> {

    private final NodeSeqBuffer nodeSeqBuffer;
    private List<QueryModification> queryModifications;
    private SeqState<T> seqState;

    public QueryState() {
        this.nodeSeqBuffer = new NodeSeqBuffer();
    }

    public CharSequence getCurrentTerm() {
        return nodeSeqBuffer.getLast().getCharSeq();
    }

    // TODO: should be rather append(Node node); since SavedState was implemented no access to buffer is required anymore
    public NodeSeqBuffer getNodeSeqBuffer() {
        return this.nodeSeqBuffer;
    }

    @Override
    public SeqState<T> getSeqState() {
        return seqState != null ? seqState : SeqState.empty();
    }

    public void setSeqState(SeqState<T> seqState) {
        this.seqState = seqState;
    }

    // TODO: getView() should be method of buffer
    @Override
    public List<Node> viewSequenceBuffer() {
        return Collections.unmodifiableList(nodeSeqBuffer.getNodes());
    }

    @Override
    public List<CharSequence> copySequenceFromBuffer() {
        return nodeSeqBuffer.getNodes().stream().map(Node::getCharSeq).collect(Collectors.toList());
    }

    public void clear() {
        this.nodeSeqBuffer.clear();
        this.seqState = null;
    }

    public void collectInstructionsForSeq(final List<Instruction> instructions) {
        if (this.queryModifications == null) {
            this.queryModifications = new ArrayList<>();
        }

        this.queryModifications.add(new QueryModification(nodeSeqBuffer.createNodeSeqFromBuffer(),
                Collections.unmodifiableList(instructions)));
    }

    public void collectInstructionsForSeq(final Instruction instruction) {
        collectInstructionsForSeq(Collections.singletonList(instruction));
    }

    public List<QueryModification> getQueryModifications() {
        return this.queryModifications == null ? Collections.emptyList() : this.queryModifications;
    }

    public SavedState<T> saveState() {
        return new SavedState<>(this.nodeSeqBuffer.getOffset(), this.seqState);
    }

    public void loadState(SavedState<T> savedState) {
        this.nodeSeqBuffer.setOffset(savedState.nodeSeqBufferState);
        this.seqState = savedState.seqState;
    }


    public static class SavedState<T> {
        private final int nodeSeqBufferState;
        private final SeqState<T> seqState;

        private SavedState(int nodeSeqBufferState, SeqState<T> seqState) {
            this.nodeSeqBufferState = nodeSeqBufferState;
            this.seqState = seqState;
        }
    }


}
