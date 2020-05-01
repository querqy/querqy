package querqy.v2.node;

import querqy.CompoundCharSequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Node {

    private final boolean isStartNode;
    private final boolean isEndNode;
    private final CharSequence seq;

    private boolean isDeleted;
    private List<Node> previousNodes;
    private List<Node> nextNodes;

    public static Node createStartNode() {
        return new Node(true, false, null);
    }

    public static Node createEndNode() {
        return new Node(false, true, null);
    }

    public static Node createTermNode(CharSequence seq) {
        return new Node(false, false, seq);
    }

    private Node(boolean isStartNode, boolean isEndNode, CharSequence seq) {
        this.isStartNode = isStartNode;
        this.isEndNode = isEndNode;
        this.seq = seq;

        this.isDeleted = false;

        if (isStartNode) {
            this.nextNodes = new ArrayList<>();
        } else if (isEndNode) {
            this.previousNodes = new ArrayList<>();
        } else {
            this.previousNodes = new ArrayList<>();
            this.nextNodes = new ArrayList<>();
        }
    }

    public void addNext(Node node) {
        this.nextNodes.add(node);
    }

    public void addAllNext(Collection<Node> nodes) {
        this.nextNodes.addAll(nodes);
    }

    public List<Node> getNext() {
        return this.nextNodes;
    }

    public void addPrevious(Node node) {
        this.previousNodes.add(node);
    }

    public void addAllPrevious(Collection<Node> nodes) {
        this.previousNodes.addAll(nodes);
    }

    public List<Node> getPrevious() {
        return this.previousNodes;
    }

    public void markAsDeleted() {
        this.isDeleted = true;
    }

    public boolean hasNext() {
        return nextNodes != null && !nextNodes.isEmpty();
    }

    public boolean hasPrevious() {
        return previousNodes != null && !previousNodes.isEmpty();
    }

    public boolean terminatesSeq() {
        return this.isEndNode || this.isDeleted;
    }

    public boolean isDeleted() {
        return isDeleted;
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
