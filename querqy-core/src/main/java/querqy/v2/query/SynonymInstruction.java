package querqy.v2.query;

import querqy.v2.node.NodeSeq;
import querqy.v2.query.Instruction;
import querqy.v2.query.Query;

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
        query.addVariant(nodeSeq, NodeSeq.nodeSeqFromCharSeqs(synonymSequence));
    }
}
