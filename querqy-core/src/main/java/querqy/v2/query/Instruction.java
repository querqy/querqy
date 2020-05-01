package querqy.v2.query;

import querqy.v2.node.NodeSeq;
import querqy.v2.query.Query;

public interface Instruction {

    void apply(Query query, NodeSeq nodeSeq);

}
