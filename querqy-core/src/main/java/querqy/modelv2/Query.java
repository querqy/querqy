package querqy.modelv2;

import java.util.ArrayList;
import java.util.List;

public class Query {
    public final Node startNode;
    //public final Node endNode;


    private Query(Node startNode) {
        this.startNode = startNode;
    }

    public static class Builder {
        private final Node startNode = Node.createStartNode();
        private Node lastNode = startNode;

        public Builder append(CharSequence seq) {
            final Node newNode = Node.createTermNode(seq);
            lastNode.addNext(newNode);
            lastNode = newNode;
            return this;
        }

        public Query build() {
            lastNode.addNext(Node.createEndNode());
            return new Query(startNode);
        }
    }

    @Override
    public String toString() {

        return String.join("\n", startNode.extractAllFullSequences());
    }

}
