package querqy.v2.model;

import querqy.v2.NodeSeq;
import querqy.v2.Query;

import java.util.List;

public class QueryModification {

    private final NodeSeq nodeSeq;
    private final List<Instruction> instructions;

    public QueryModification(final NodeSeq nodeSeq, final List<Instruction> instructions) {
        this.nodeSeq = nodeSeq;
        this.instructions = instructions;
    }

    public void apply(Query query) {
        for (Instruction instruction : instructions) {
            instruction.apply(query, nodeSeq);
        }
    }
}
