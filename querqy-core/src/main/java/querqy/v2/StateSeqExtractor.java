package querqy.v2;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class StateSeqExtractor<T> {

    private final SequenceHandler<T> sequenceHandler;
    private final NodeSeqBuffer nodeSeqBuffer;
    //private final List<Match>

    public StateSeqExtractor(SequenceHandler<T> sequenceHandler) {
        this.sequenceHandler = sequenceHandler;
        this.nodeSeqBuffer = new NodeSeqBuffer();
    }

    public void crawlQuery(Query query) {

        final Set<Node> nodeRegistry = query.getNodeRegistry();
        final State state = new State(nodeSeqBuffer);

        for (Node node : nodeRegistry) {
            this.nodeSeqBuffer.clear();
            crawlQuery(node, state);
        }
    }

    private void crawlQuery(Node node, State state) {
        if (node.isEndNode) {
            return;
        }

        nodeSeqBuffer.add(node);
        sequenceHandler.handleSequence(state).ifPresent(
                t -> {
                    int offset = nodeSeqBuffer.getOffset();
                    for (Node nextNode : node.getNext()) {
                        nodeSeqBuffer.setOffset(offset);
                        crawlQuery(nextNode, state.setState(t));
                    }
                }
        );
    }

    public class State {

        private final NodeSeqBuffer nodeSeqBuffer;
        T outerState;

        public State(final NodeSeqBuffer nodeSeqBuffer) {
            this.nodeSeqBuffer = nodeSeqBuffer;
        }

        public Optional<T> getState() {
            return outerState != null ? Optional.of(outerState) : Optional.empty();
        }

        public CharSequence getCurrentTerm() {
            return nodeSeqBuffer.getLast().getCharSeq();
        }

        public List<CharSequence> getSequence() {
            return nodeSeqBuffer.getNodes().stream().map(Node::getCharSeq).collect(Collectors.toList());
        }


        private State setState(T outerState) {
            this.outerState = outerState;
            return this;
        }

        public void collectInstructionForSeq() {

        }
    }

    public class Match {

    }
}
