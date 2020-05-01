package querqy.v2;

import org.junit.Test;
import querqy.v2.seqhandler.SeqState;
import querqy.v2.node.Node;
import querqy.v2.node.NodeSeq;
import querqy.v2.node.NodeSeqBuffer;
import querqy.v2.query.Query;
import querqy.v2.seqhandler.StateHandler;
import querqy.v2.seqhandler.StatefulSeqHandler;

import java.util.ArrayList;
import java.util.List;

public class TestQuery {

    @Test
    public void test() {
        Node nodeA = Node.createTermNode("A");
        Node nodeB = Node.createTermNode("B");
        Node nodeC = Node.createTermNode("C");
        Node nodeD = Node.createTermNode("D");

        Node nodeE = Node.createTermNode("E");
        Node nodeF = Node.createTermNode("F");
        Node nodeG = Node.createTermNode("G");
        Node nodeH = Node.createTermNode("H");
        Node nodeI = Node.createTermNode("I");

        NodeSeqBuffer nodeSeqBuffer = new NodeSeqBuffer();

        Query query = new Query.Builder()
                .append(nodeA)
                .append(nodeB)
                .append(nodeC)
                .append(nodeD)
                .build();


        System.out.println(query);

        query.addVariant(nodeC, nodeE);

        System.out.println(query);

        query.addVariant(nodeD, nodeF);

        System.out.println(query);

        query.addVariant(nodeSeqBuffer.clear().addAll(nodeA, nodeB).createNodeSeqFromBuffer(),
                NodeSeq.nodeSeqFromNodes(nodeG, nodeH, nodeI));

        System.out.println(query);

        query.removeNode(nodeG);

        System.out.println(query);




        final List<Node> nodes = new ArrayList<>();

        final Object obj = new Object();

        System.out.println();
        System.out.println();
        System.out.println();
        StateHandler<Object> stateHandler = queryStateView -> {
            //System.out.println(node.seq);

            System.out.println("Seq: " + queryStateView.getSequence() + " Term: " + queryStateView.getCurrentTerm());
            return new SeqState<>(obj);
        };

        /*
        [ A B C D
          A B C F
          A B E D
          A B E F
          G H I C D
          G H I C F
          G H I E D
          G H I E F ]
         */

        StatefulSeqHandler<Object> statefulSeqHandler = new StatefulSeqHandler<>(stateHandler);
        statefulSeqHandler.findSeqsAndApplyModifications(query);

        System.out.println(query.getNodeRegistry());



    }
}
