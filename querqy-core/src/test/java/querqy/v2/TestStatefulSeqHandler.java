package querqy.v2;

import org.junit.Test;
import querqy.trie.State;
import querqy.trie.States;
import querqy.trie.TrieMap;
import querqy.v2.model.Instruction;
import querqy.v2.model.SynonymInstruction;

import java.util.Optional;

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

        StateHandler<State<Instruction>> stateHandler = state -> {

            Optional<State<Instruction>> iterationState = state.getState();

            State<Instruction> lookupState = state.getState().isPresent()
                    ? map.get(state.getCurrentTerm(), iterationState.get()).getStateForCompleteSequence()
                    : map.get(state.getCurrentTerm()).getStateForCompleteSequence();

            if (lookupState.isFinal()) {
                state.collectInstructionsForSeq(lookupState.value);
            }

            return lookupState.isKnown
                    ? Optional.of(lookupState)
                    : Optional.empty();

        };

        StatefulSeqHandler<State<Instruction>> statefulSeqHandler = new StatefulSeqHandler<>(stateHandler);
        statefulSeqHandler.crawlQueryAndApplyModifications(query);

        System.out.println(query);


    }
}
