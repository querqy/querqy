package querqy.v2;

import org.junit.Test;
import querqy.v2.node.Node;
import querqy.v2.node.NodeSeq;
import querqy.v2.node.NodeSeqBuffer;
import querqy.v2.query.Query;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestQuery {

    private List<CharSequence> list(CharSequence... seqs) {
        return Arrays.asList(seqs);
    }

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


        assertThat(query.findAllQueryVariants()).containsExactlyInAnyOrder(
                list("A", "B", "C", "D"));

        query.add(nodeC, nodeE);

        assertThat(query.findAllQueryVariants()).containsExactlyInAnyOrder(
                list("A", "B", "C", "D"),
                list("A", "B", "E", "D")
        );

        query.add(nodeD, nodeF);

        assertThat(query.findAllQueryVariants()).containsExactlyInAnyOrder(
                list("A", "B", "C", "D"),
                list("A", "B", "C", "F"),
                list("A", "B", "E", "D"),
                list("A", "B", "E", "F")
        );

        query.add(nodeSeqBuffer.clear().addAll(nodeA, nodeB).createNodeSeqFromBuffer(),
                NodeSeq.nodeSeqFromNodes(nodeG, nodeH, nodeI));

        assertThat(query.findAllQueryVariants()).containsExactlyInAnyOrder(
                list("A", "B", "C", "D"),
                list("A", "B", "C", "F"),
                list("A", "B", "E", "D"),
                list("A", "B", "E", "F"),

                list("G", "H", "I", "C", "D"),
                list("G", "H", "I", "C", "F"),
                list("G", "H", "I", "E", "D"),
                list("G", "H", "I", "E", "F")
        );

        query.removeNode(nodeG);

        assertThat(query.findAllQueryVariants()).containsExactlyInAnyOrder(
                list("A", "B", "C", "D"),
                list("A", "B", "C", "F"),
                list("A", "B", "E", "D"),
                list("A", "B", "E", "F"),

                list("H", "I", "C", "D"),
                list("H", "I", "C", "F"),
                list("H", "I", "E", "D"),
                list("H", "I", "E", "F")
        );

        query.removeNodes(Arrays.asList(nodeI, nodeC));

        assertThat(query.findAllQueryVariants()).containsExactlyInAnyOrder(
                list("A", "B", "D"),
                list("A", "B", "F"),
                list("A", "B", "E", "D"),
                list("A", "B", "E", "F"),

                list("H", "D"),
                list("H", "F"),
                list("H", "E", "D"),
                list("H", "E", "F")
        );








//        final List<Node> nodes = new ArrayList<>();
//
//        final Object obj = new Object();
//
//        System.out.println();
//        System.out.println();
//        System.out.println();
//        StateExchangeFunction<Object> stateExchangeFunction = queryStateView -> {
//            //System.out.println(node.seq);
//
//            System.out.println("Seq: " + queryStateView.viewSequenceBuffer() + " Term: " + queryStateView.getCurrentTerm());
//            return new SeqState<>(obj);
//        };
//
//        /*
//        [ A B C D
//          A B C F
//          A B E D
//          A B E F
//          G H I C D
//          G H I C F
//          G H I E D
//          G H I E F ]
//         */
//
//        StateExchangeSeqHandler<Object> stateExchangeSeqHandler = new StateExchangeSeqHandler<>(stateExchangeFunction);
//        stateExchangeSeqHandler.findSeqsAndApplyModifications(query);
//
//        System.out.println(query.getNodeRegistry());



    }
}
