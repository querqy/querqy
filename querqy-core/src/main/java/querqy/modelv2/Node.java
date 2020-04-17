package querqy.modelv2;

import querqy.CompoundCharSequence;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Node {

    public final boolean isStartNode;
    public final boolean isEndNode;
    public final CharSequence seq;
    private final Set<Node> nextNodes;

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
        this.nextNodes = !isEndNode ? new HashSet<>() : null;
    }

    public void addNext(Node node) {
        this.nextNodes.add(node);
    }

    public List<CharSequence> extractAllFullSequences() {
        List<CharSequence> seqs = new ArrayList<>();
        extractAllFullSequences(0, new ArrayList<>(), seqs);
        return seqs;
    }

    private void extractAllFullSequences(int index, List<CharSequence> tempList, List<CharSequence> seqList) {
        if (this.isStartNode) {
            for (Node nextNode : nextNodes) {
                nextNode.extractAllFullSequences(index, tempList, seqList);
            }

        } else if (this.isEndNode) {
            seqList.add(new CompoundCharSequence(" ", tempList.subList(0, index)));

        } else {
            tempList.add(index, this.seq);
            index++;

            for (Node nextNode : nextNodes) {
                nextNode.extractAllFullSequences(index, tempList, seqList);
            }
        }





    }






}
