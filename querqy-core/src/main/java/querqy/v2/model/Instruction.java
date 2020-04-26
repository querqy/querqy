package querqy.v2.model;

import querqy.v2.NodeSeq;

public abstract class Instruction {

    public final NodeSeq nodeSeq;

    public Instruction(final NodeSeq nodeSeq) {
        this.nodeSeq = nodeSeq;
    }

    public abstract void apply();
}
