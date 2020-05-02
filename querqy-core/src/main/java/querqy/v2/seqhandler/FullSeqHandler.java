package querqy.v2.seqhandler;

import querqy.v2.node.Node;
import querqy.v2.query.Query;
import querqy.v2.seqhandler.state.QueryState;
import querqy.v2.seqhandler.state.QueryStateView;

import java.util.function.Consumer;

public class FullSeqHandler implements SeqHandler {

    private final Consumer<QueryStateView> queryStateViewConsumer;

    public FullSeqHandler(Consumer<QueryStateView> queryStateViewConsumer) {
        this.queryStateViewConsumer = queryStateViewConsumer;
    }


    @Override
    public void findSeqsAndApplyModifications(Query query) {
        final QueryState queryState = new QueryState();

        for (final Node nextNode : query.startNode.getUpstream()) {

            queryState.clear();
            findFullSeqs(nextNode, queryState);
        }

    }

    private void findFullSeqs(Node node, QueryState queryState) {
        if (node.isDeleted()) {
            return;
        }

        if (node.isEndNode()) {
            queryStateViewConsumer.accept(queryState);
            return;
        }

        queryState.getNodeSeqBuffer().add(node);

        if (node.hasExactlyOneUpstreamNode()) {
            findFullSeqs(node.getUpstream().get(0), queryState);

        } else if (node.hasUpstream()) {
            QueryState.SavedState savedState = queryState.saveState();

            for (final Node upstreamNode : node.getUpstream()) {
                queryState.loadState(savedState);
                findFullSeqs(upstreamNode, queryState);
            }
        }






    }


}
