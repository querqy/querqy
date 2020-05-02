package querqy.v2.query;

import querqy.v2.node.Node;
import querqy.v2.node.NodeSeq;
import querqy.v2.seqhandler.FullSeqHandler;
import querqy.v2.seqhandler.SeqHandler;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Query {
    public final Node startNode;

    private final Set<Node> nodeRegistry;

    private Query(Node startNode, Set<Node> nodeRegistry) {
        this.startNode = startNode;
        this.nodeRegistry = nodeRegistry;
    }

    public void removeNode(Node node) {
        for (Node downstreamNode : node.getDownstream()) {
            downstreamNode.addAllUpstreamNodes(node.getUpstream());
        }

        for (Node upstreamNode : node.getUpstream()) {
            upstreamNode.addAllDownstreamNodes(node.getDownstream());
        }

        node.markAsDeleted();
    }

    public void removeNodes(List<Node> nodes) {
        for (Node node : nodes) {
            removeNode(node);
        }
    }

    // TODO: nodes in NodeSeq original could have been deleted -> should have been solved
    // TODO: ensure that all nodes in original are part of the query (probably introduce flag "deleted" for nodes -> done
    public void add(NodeSeq original, NodeSeq variant) {
        variant.getFirstNode().addAllDownstreamNodes(original.getFirstNode().getDownstream());
        variant.getLastNode().addAllUpstreamNodes(original.getLastNode().getUpstream());

        openFork(original.getFirstNode(), variant.getFirstNode());
        closeFork(original.getLastNode(), variant.getLastNode());

        nodeRegistry.addAll(variant.getNodes());
    }

    // TODO: node original could have been deleted -> should be solved; needs to be tested
    public void add(Node original, Node variant) {
        variant.addAllDownstreamNodes(original.getDownstream());
        variant.addAllUpstreamNodes(original.getUpstream());

        openFork(original, variant);
        closeFork(original, variant);

        nodeRegistry.add(variant);
    }

    private void openFork(Node original, Node variant) {
        for (Node previousNode : original.getDownstream()) {
            previousNode.addUpstreamNode(variant);
        }
    }

    private void closeFork(Node original, Node variant) {
        for (Node nextNode : original.getUpstream()) {
            nextNode.addDownstreamNode(variant);
        }
    }

    public Set<Node> getNodeRegistry() {
        return this.nodeRegistry;
    }


    // TODO: should not depend on FullSeqHandler, but apply its own seq handling
    public List<List<CharSequence>> findAllQueryVariants() {

        List<List<CharSequence>> seqs = new ArrayList<>();
        SeqHandler seqHandler = new FullSeqHandler(
                queryStateView -> seqs.add(queryStateView.copySequenceFromBuffer()));

        seqHandler.findSeqsAndApplyModifications(this);
        return seqs;
    }

    @Override
    public String toString() {
        List<CharSequence> seqs = findAllQueryVariants().stream()
                .map(seq -> " " + seq.toString())
                .collect(Collectors.toList());

        return "[" + String.join("\n ", seqs) + " ]";


//        List<CharSequence> seqs = startNode.extractAllFullSequences()
//                .stream()
//                .map(seq -> " " + seq.toString())
//                .collect(Collectors.toList());
//
//        return "[" + String.join("\n ", seqs) + " ]";
    }

    public static Query.Builder builder() {
        return new Query.Builder();
    }

    public static class Builder {

        private final NodeSeq.Builder nodeSeqBuilder = NodeSeq.builder();
        private final Set<Node> nodeRegister = new LinkedHashSet<>();

        public Builder() {
            nodeSeqBuilder.append(Node.createStartNode());
        }

        public Builder append(CharSequence seq) {
            return append(Node.createTermNode(seq));
        }

        public Builder append(Node newNode) {
            nodeSeqBuilder.append(newNode);
            nodeRegister.add(newNode);
            return this;
        }

        public Query build() {
            nodeSeqBuilder.append(Node.createEndNode());
            return new Query(nodeSeqBuilder.build().getFirstNode(), nodeRegister);
        }
    }
}
