package querqy.v2.node;

import querqy.CompoundCharSequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Node extends CharSeqContainer {

    private final boolean isStartNode;
    private final boolean isEndNode;

    private boolean isDeleted;

    private List<Node> downstream;
    private List<Node> upstream;

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
        super(seq);

        this.isStartNode = isStartNode;
        this.isEndNode = isEndNode;


        this.isDeleted = false;

        if (isStartNode) {
            this.upstream = new ArrayList<>();
            this.downstream = Collections.emptyList();
        } else if (isEndNode) {
            this.upstream = Collections.emptyList();
            this.downstream = new ArrayList<>();
        } else {
            this.upstream = new ArrayList<>();
            this.downstream = new ArrayList<>();
        }
    }

    public void addUpstreamNode(Node node) {
        this.upstream.add(node);
    }

    public void addAllUpstreamNodes(Collection<Node> nodes) {
        this.upstream.addAll(nodes);
    }

    public List<Node> getUpstream() {
        return this.upstream;
    }

    public void addDownstreamNode(Node node) {
        this.downstream.add(node);
    }

    public void addAllDownstreamNodes(Collection<Node> nodes) {
        this.downstream.addAll(nodes);
    }

    public List<Node> getDownstream() {
        return this.downstream;
    }

    public void markAsDeleted() {
        this.isDeleted = true;
    }

    public boolean hasUpstream() {
        return !upstream.isEmpty();
    }

    public boolean hasExactlyOneUpstreamNode() {
        return upstream.size() == 1;
    }

    public boolean hasUpstreamFork() {
        return upstream.size() > 1;
    }

    public boolean hasDownstream() {
        return !downstream.isEmpty();
    }

    public boolean isSeqTerminator() {
        return this.isEndNode || this.isDeleted;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public boolean isEndNode() {
        return isEndNode;
    }




    @Override
    public CharSequence getCharSeq() {
        return seq;
    }


    // TODO: move out of node
    @Deprecated
    public List<CharSequence> extractAllFullSequences() {
        List<CharSequence> seqs = new ArrayList<>();
        extractAllFullSequences(0, new ArrayList<>(), seqs);
        return seqs;
    }

    // TODO: move out of node
    @Deprecated
    private void extractAllFullSequences(int index, List<CharSequence> tempList, List<CharSequence> seqList) {
        if (this.isDeleted) {
            return;
        }

        if (this.isStartNode) {
            for (Node nextNode : this.upstream) {
                nextNode.extractAllFullSequences(index, tempList, seqList);
            }

        } else if (this.isEndNode) {
            seqList.add(new CompoundCharSequence(" ", tempList.subList(0, index)));

        } else {
            tempList.add(index, this.seq);
            index++;

            for (Node nextNode : this.upstream) {
                nextNode.extractAllFullSequences(index, tempList, seqList);
            }
        }
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



}
