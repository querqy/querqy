package querqy.v2;

import org.junit.Test;
import querqy.trie.State;
import querqy.trie.TrieMap;
import querqy.v2.query.Instruction;
import querqy.v2.seqhandler.SeqState;
import querqy.v2.query.SynonymInstruction;
import querqy.v2.query.Query;
import querqy.v2.seqhandler.StateHandler;
import querqy.v2.seqhandler.StatefulSeqHandler;

public class TestStatefulSeqHandler {

    @Test
    public void test() {

        Query query = Query.builder()
                .append("A")
                .append("B")
                .append("C")
                .build();

        TrieMap<Instruction> map = new TrieMap<>();
        map.put("A", new SynonymInstruction("D"));
        map.put("BC", new SynonymInstruction("E"));

        System.out.println(query);

        StateHandler<State<Instruction>> stateHandler = queryStateView -> {

            SeqState<State<Instruction>> seqState = queryStateView.getSeqState();

            State<Instruction> newLookupState = seqState.applyIfPresentOrElseGet(
                    lookupState -> map.get(queryStateView.getCurrentTerm(), lookupState).getStateForCompleteSequence(),
                    () -> map.get(queryStateView.getCurrentTerm()).getStateForCompleteSequence());

            if (newLookupState.isFinal()) {
                queryStateView.collectInstructionsForSeq(newLookupState.value);
            }

            return newLookupState.isKnown
                    ? new SeqState<>(newLookupState)
                    : SeqState.empty();
        };

        StatefulSeqHandler<State<Instruction>> statefulSeqHandler = new StatefulSeqHandler<>(stateHandler);
        statefulSeqHandler.findSeqsAndApplyModifications(query);

        System.out.println(query);


    }
}
