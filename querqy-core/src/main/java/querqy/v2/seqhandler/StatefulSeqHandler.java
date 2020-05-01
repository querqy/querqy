package querqy.v2.seqhandler;

import querqy.v2.query.QueryModification;
import querqy.v2.node.Node;
import querqy.v2.query.Query;

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

            if (node.terminatesSeq()) {
                continue;
            }

            queryState.clear();
            findSeqs(node, queryState);
        }

        for (QueryModification modification : queryState.getQueryModifications()) {
            modification.apply(query);
        }
    }

    private void findSeqs(final Node node, final QueryState<T> queryState) {
        if (node.terminatesSeq()) {
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
