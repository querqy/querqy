package querqy.v2.seqhandler;

import querqy.v2.query.QueryModification;
import querqy.v2.node.Node;
import querqy.v2.query.Query;
import querqy.v2.seqhandler.state.QueryState;
import querqy.v2.seqhandler.state.SeqState;

import java.util.Set;

public class StateExchangeSeqHandler<T> implements SeqHandler {

    private final StateExchangeFunction<T> stateExchangeFunction;

    public StateExchangeSeqHandler(StateExchangeFunction<T> stateExchangeFunction) {
        this.stateExchangeFunction = stateExchangeFunction;
    }

    @Override
    public void findSeqsAndApplyModifications(Query query) {

        final Set<Node> nodeRegistry = query.getNodeRegistry();
        final QueryState<T> queryState = new QueryState<>();

        for (Node node : nodeRegistry) {

            if (node.isSeqTerminator()) {
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
        if (node.isSeqTerminator()) {
            return;
        }

        queryState.getNodeSeqBuffer().add(node);

        final SeqState<T> seqState = stateExchangeFunction.exchangeState(queryState);

        if (seqState.isPresent()) {

            queryState.setSeqState(seqState);

            QueryState.SavedState<T> savedState = queryState.saveState();

            for (Node nextNode : node.getUpstream()) {
                queryState.loadState(savedState);
                findSeqs(nextNode, queryState);
            }
        }

    }

}
