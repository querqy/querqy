package querqy.v2;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
                NodeSeq.nodeSeqFromCharSeqs("G", "H", "I"));

        System.out.println(query);




        final List<Node> nodes = new ArrayList<>();

        final Object obj = new Object();

        System.out.println();
        System.out.println();
        System.out.println();
        StateHandler<Object> stateHandler = state -> {
            //System.out.println(node.seq);

            System.out.println("Seq: " + state.getSequence() + " Term: " + state.getCurrentTerm());
            return Optional.of(obj);
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
        statefulSeqHandler.crawlQueryAndApplyModifications(query);

        System.out.println(query.getNodeRegistry());



    }
}
