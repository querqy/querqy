package querqy.v2.query;

import querqy.v2.node.NodeSeq;

import java.util.Arrays;
import java.util.List;

public class SynonymInstruction implements Instruction {

    private final List<CharSequence> synonymSequence;

    public SynonymInstruction(final List<CharSequence> synonymSequence) {
        this.synonymSequence = synonymSequence;
    }

    public SynonymInstruction(CharSequence... synonymSequence) {
        this.synonymSequence = Arrays.asList(synonymSequence);
    }

    @Override
    public void apply(final Query query, final NodeSeq nodeSeq) {
        query.add(nodeSeq, NodeSeq.nodeSeqFromCharSeqs(synonymSequence));
    }
}
