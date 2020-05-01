package querqy.v2.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NodeSeq {

    private final List<Node> nodes;

    NodeSeq(List<Node> nodes) {
        if (nodes.isEmpty()) {
            throw new UnsupportedOperationException("NodeSeqs of length 0 cannot be built");
        }

        this.nodes = Collections.unmodifiableList(nodes);
    }

    public Node getFirstNode() {
        return nodes.get(0);
    }

    public Node getLastNode() {
        return nodes.get(nodes.size() - 1);
    }

    public List<Node> getNodes() {
        return this.nodes;
    }

    public void markAllAsDeleted() {
        for (Node node : nodes) {
            node.markAsDeleted();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Node lastNode;

        private final List<Node> nodes = new ArrayList<>();

        public Builder append(Node newNode) {

            if (newNode.hasPrevious() || newNode.hasNext()) {
                throw new IllegalArgumentException(
                        "Node to be appended to NodeSeq must not have references to other nodes. " +
                        "Use NodeSeqBuffer.createNodeSeqFromBuffer to create NodeSeq with already wired nodes.");
            }

            if (lastNode == null) {
                lastNode = newNode;

            } else {
                this.lastNode.addNext(newNode);
                newNode.addPrevious(this.lastNode);
                this.lastNode = newNode;
            }

            nodes.add(newNode);

            return this;
        }

        public Builder append(CharSequence seq) {
            return append(Node.createTermNode(seq));
        }

        public NodeSeq build() {
            return new NodeSeq(nodes);
        }
    }

    public static NodeSeq nodeSeqFromNodes(Node... nodes) {
        NodeSeq.Builder builder = builder();

        for (Node node : nodes) {
            builder.append(node);
        }

        return builder.build();
    }

    public static NodeSeq nodeSeqFromCharSeqs(CharSequence... seqs) {
        NodeSeq.Builder builder = builder();

        for (CharSequence seq : seqs) {
            builder.append(seq);
        }

        return builder.build();
    }

    public static NodeSeq nodeSeqFromCharSeqs(List<CharSequence> seqs) {
        NodeSeq.Builder builder = builder();

        for (CharSequence seq : seqs) {
            builder.append(seq);
        }

        return builder.build();
    }

    @Override
    public String toString() {
        return "NodeSeq{" +
                "nodes=" + nodes +
                '}';
    }
}
