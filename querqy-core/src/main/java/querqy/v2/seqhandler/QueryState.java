package querqy.v2.seqhandler;

import querqy.v2.node.Node;
import querqy.v2.node.NodeSeqBuffer;
import querqy.v2.query.Instruction;
import querqy.v2.query.QueryModification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

// TODO: find better naming - QueryState misleading?
public class QueryState<T> implements QueryStateView<T> {
    private final NodeSeqBuffer nodeSeqBuffer;
    private List<QueryModification> queryModifications;
    private SeqState<T> seqState;

    public QueryState() {
        this.nodeSeqBuffer = new NodeSeqBuffer();
    }

    public SeqState<T> getSeqState() {
        return seqState != null ? seqState : SeqState.empty();
    }

    public void setSeqState(SeqState<T> seqState) {
        this.seqState = seqState;
    }

    public CharSequence getCurrentTerm() {
        return nodeSeqBuffer.getLast().getCharSeq();
    }

    public NodeSeqBuffer getNodeSeqBuffer() {
        return this.nodeSeqBuffer;
    }

    // TODO: find better way to hide nodes - rewriters should not be able to change node references
    public List<CharSequence> getSequence() {
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

}
