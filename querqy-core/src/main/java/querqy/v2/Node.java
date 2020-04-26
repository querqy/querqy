package querqy.v2;

import querqy.CompoundCharSequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Node {

    final boolean isStartNode;
    final boolean isEndNode;
    private final CharSequence seq;

    // TODO: Should be some kind of micro map? Required to fork edges for edge case deletions
    private List<Node> previousNodes;
    private List<Node> nextNodes;

    // TODO: start and end node should be own class?
    static Node createStartNode() {
        return new Node(true, false, null);
    }
    static Node createEndNode() {
        return new Node(false, true, null);
    }
    public static Node createTermNode(CharSequence seq) {
        return new Node(false, false, seq);
    }

    private Node(boolean isStartNode, boolean isEndNode, CharSequence seq) {
        this.isStartNode = isStartNode;
        this.isEndNode = isEndNode;
        this.seq = seq;

        if (isStartNode) {
            this.nextNodes = new ArrayList<>();
        } else if (isEndNode) {
            this.previousNodes = new ArrayList<>();
        } else {
            this.previousNodes = new ArrayList<>();
            this.nextNodes = new ArrayList<>();
        }
    }

    void addNext(Node node) {
        this.nextNodes.add(node);
    }

    void addAllNext(Collection<Node> nodes) {
        this.nextNodes.addAll(nodes);
    }

    List<Node> getNext() {
        return this.nextNodes;
    }

    void addPrevious(Node node) {
        this.previousNodes.add(node);
    }

    void addAllPrevious(Collection<Node> nodes) {
        this.previousNodes.addAll(nodes);
    }

    List<Node> getPrevious() {
        return this.previousNodes;
    }

    void removePrevious(Node node) {
        this.previousNodes.remove(node);
    }

    void removeNext(Node node) {
        this.nextNodes.remove(node);
    }


    boolean hasNext() {
        return nextNodes != null && !nextNodes.isEmpty();
    }

    boolean hasPrevious() {
        return previousNodes != null && !previousNodes.isEmpty();
    }

    public CharSequence getCharSeq() {
        return seq;
    }

    @Override
    public String toString() {
        if (isStartNode) {
            return "[START]";
        } else if (isEndNode) {
            return "[END]";
        } else {
            return seq.toString();
        }


    }






    /**
     * @deprecated
     * TODO: Wiring should not be done in node
     */
    @Deprecated
    public Node removeNodeFromPreviousNodes() {
        for (Node previousNode : this.previousNodes) {
            previousNode.removeNext(this);
        }
        return this;
    }

    @Deprecated
    public Node removeNodeFromNextNodes() {
        for (Node nextNode : this.nextNodes) {
            nextNode.removePrevious(this);
        }
        return this;
    }


    // TODO: move out of node
    public List<CharSequence> extractAllFullSequences() {
        List<CharSequence> seqs = new ArrayList<>();
        extractAllFullSequences(0, new ArrayList<>(), seqs);
        return seqs;
    }

    private void extractAllFullSequences(int index, List<CharSequence> tempList, List<CharSequence> seqList) {
        if (this.isStartNode) {
            for (Node nextNode : this.nextNodes) {
                nextNode.extractAllFullSequences(index, tempList, seqList);
            }

        } else if (this.isEndNode) {
            seqList.add(new CompoundCharSequence(" ", tempList.subList(0, index)));

        } else {
            tempList.add(index, this.seq);
            index++;

            for (Node nextNode : this.nextNodes) {
                nextNode.extractAllFullSequences(index, tempList, seqList);
            }
        }
    }


}
