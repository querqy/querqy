package querqy.v2.model;

import querqy.v2.NodeSeq;
import querqy.v2.Query;

public interface Instruction {

    void apply(Query query, NodeSeq nodeSeq);

}
