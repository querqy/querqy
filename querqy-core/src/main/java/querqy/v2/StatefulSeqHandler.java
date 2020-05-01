package querqy.v2;

import querqy.v2.model.Instruction;
import querqy.v2.model.QueryModification;
import querqy.v2.model.QueryState;
import querqy.v2.model.SeqState;

import java.util.Set;

public class StatefulSeqHandler<T> {

    private final StateHandler<T> stateHandler;

    public StatefulSeqHandler(StateHandler<T> stateHandler) {
        this.stateHandler = stateHandler;
    }

    public void findSeqsAndApplyModifications(Query query) {

        final Set<Node> nodeRegistry = query.getNodeRegistry();
        final QueryState<T> queryState = new QueryState<>();

        for (Node node : nodeRegistry) {
            queryState.clear();
            findSeqs(node, queryState);
        }

        for (QueryModification modification : queryState.getQueryModifications()) {
            modification.apply(query);
        }
    }

    private void findSeqs(final Node node, final QueryState<T> queryState) {
        if (node.isEndNode) {
            return;
        }

        queryState.getNodeSeqBuffer().add(node);

        SeqState<T> seqState = stateHandler.handleSequence(queryState);
        if (seqState.hasValue()) {
            int offset = queryState.getNodeSeqBuffer().getOffset();

            for (Node nextNode : node.getNext()) {
                queryState.getNodeSeqBuffer().setOffset(offset);
                queryState.setSeqState(seqState);
                findSeqs(nextNode, queryState);
            }
        }

    }

}
