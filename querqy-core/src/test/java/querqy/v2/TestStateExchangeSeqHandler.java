package querqy.v2;

import org.junit.Test;
import querqy.trie.State;
import querqy.trie.TrieMap;
import querqy.v2.query.Instruction;
import querqy.v2.seqhandler.state.SeqState;
import querqy.v2.query.SynonymInstruction;
import querqy.v2.query.Query;
import querqy.v2.seqhandler.StateExchangeFunction;
import querqy.v2.seqhandler.StateExchangeSeqHandler;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestStateExchangeSeqHandler {

    private List<CharSequence> list(CharSequence... seqs) {
        return Arrays.asList(seqs);
    }

    private List<List<CharSequence>> list(List<CharSequence>... seqLists) {
        return Arrays.asList(seqLists);
    }



    @Test
    public void testSynonyms() {

        Query query = Query.builder()
                .append("A")
                .append("B")
                .append("C")
                .build();

        TrieMap<Instruction> map = new TrieMap<>();
        map.put("A", new SynonymInstruction("D"));
        map.put("BC", new SynonymInstruction("E"));

        assertThat(query.findAllQueryVariants()).containsExactlyInAnyOrder(
                list("A", "B", "C"));

        StateExchangeFunction<State<Instruction>> stateExchangeFunction = queryStateView -> {

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

        StateExchangeSeqHandler<State<Instruction>> stateExchangeSeqHandler = new StateExchangeSeqHandler<>(stateExchangeFunction);
        stateExchangeSeqHandler.findSeqsAndApplyModifications(query);

        assertThat(query.findAllQueryVariants()).containsExactlyInAnyOrder(
                list("A", "B", "C"),
                list("D", "B", "C"),
                list("A", "E"),
                list("D", "E")
        );
    }
}
