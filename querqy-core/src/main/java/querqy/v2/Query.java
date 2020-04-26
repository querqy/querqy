package querqy.v2;

import java.util.Collection;
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

    public Query removeNode(Node node) {
        node.removeNodeFromPreviousNodes();
        node.removeNodeFromNextNodes();

        wireNodes(node.getPrevious(), node.getNext());
        return this;
    }

    public void wireNodes(Collection<Node> previousNodes, Collection<Node> nextNodes) {
        for (Node previousNode : previousNodes) {
            previousNode.addAllNext(nextNodes);
        }
        for (Node nextNode : nextNodes) {
            nextNode.addAllPrevious(previousNodes);
        }
    }

    private void openVariantFork(Node original, Node variant) {
        for (Node previousNode : original.getPrevious()) {
            previousNode.addNext(variant);
        }
    }

    private void closeVariantFork(Node original, Node variant) {
        for (Node nextNode : original.getNext()) {
            nextNode.addPrevious(variant);
        }
    }

    public void addVariant(NodeSeq original, NodeSeq variant) {
        variant.getFirstNode().addAllPrevious(original.getFirstNode().getPrevious());
        variant.getLastNode().addAllNext(original.getLastNode().getNext());

        openVariantFork(original.getFirstNode(), variant.getFirstNode());
        closeVariantFork(original.getLastNode(), variant.getLastNode());

        nodeRegistry.addAll(variant.getNodes());
    }

    public void addVariant(Node original, Node variant) {
        variant.addAllPrevious(original.getPrevious());
        variant.addAllNext(original.getNext());

        openVariantFork(original, variant);
        closeVariantFork(original, variant);

        nodeRegistry.add(variant);
    }

    public Set<Node> getNodeRegistry() {
        return this.nodeRegistry;
    }









    @Override
    public String toString() {

        List<CharSequence> seqs = startNode.extractAllFullSequences()
                .stream()
                .map(seq -> " " + seq.toString())
                .collect(Collectors.toList());

        return "[" + String.join("\n ", seqs) + " ]";
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

        Builder append(Node newNode) {
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
