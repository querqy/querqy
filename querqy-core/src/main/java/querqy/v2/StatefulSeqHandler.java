package querqy.v2;

import querqy.v2.model.Instruction;
import querqy.v2.model.QueryModification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class StatefulSeqHandler<T> {

    private final StateHandler<T> stateHandler;

    public StatefulSeqHandler(StateHandler<T> stateHandler) {
        this.stateHandler = stateHandler;
    }

    public void crawlQueryAndApplyModifications(Query query) {

        final Set<Node> nodeRegistry = query.getNodeRegistry();
        final State iterationState = new State();

        for (Node node : nodeRegistry) {
            iterationState.clear();
            crawlQuery(node, iterationState);
        }

        for (QueryModification modification : iterationState.getQueryModifications()) {
            modification.apply(query);
        }
    }

    private void crawlQuery(final Node node, final State state) {
        if (node.isEndNode) {
            return;
        }

        state.nodeSeqBuffer.add(node);
        stateHandler.handleSequence(state).ifPresent(
                t -> {
                    int offset = state.nodeSeqBuffer.getOffset();
                    for (Node nextNode : node.getNext()) {
                        state.nodeSeqBuffer.setOffset(offset);
                        state.outerState = t;
                        crawlQuery(nextNode, state);
                    }
                }
        );
    }

    public class State {

        private final NodeSeqBuffer nodeSeqBuffer;
        private List<QueryModification> queryModifications;
        private T outerState;

        private State() {
            this.nodeSeqBuffer = new NodeSeqBuffer();
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

        private void clear() {
            this.nodeSeqBuffer.clear();
            this.outerState = null;
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

        private List<QueryModification> getQueryModifications() {
            return this.queryModifications == null ? Collections.emptyList() : this.queryModifications;
        }
    }


}
