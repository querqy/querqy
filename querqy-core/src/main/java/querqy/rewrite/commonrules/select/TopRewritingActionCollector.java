package querqy.rewrite.commonrules.select;

import querqy.rewrite.commonrules.model.Action;
import querqy.rewrite.commonrules.model.Instructions;
import querqy.rewrite.commonrules.model.InstructionsSupplier;
import querqy.rewrite.commonrules.model.TermMatches;
import querqy.rewrite.commonrules.select.booleaninput.BooleanInputQueryHandler;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public abstract class TopRewritingActionCollector {

    private final BooleanInputQueryHandler booleanInputQueryHandler = new BooleanInputQueryHandler();

    public void collect(final InstructionsSupplier instructionsSupplier, final Function<Instructions, Action> actionCreator) {

        if (instructionsSupplier.hasInstructions()) {
            this.offer(instructionsSupplier.getInstructionsList(), actionCreator);
        }

        instructionsSupplier.getLiteral().ifPresent(booleanInputQueryHandler::notifyLiteral);
    }

    public TopRewritingActionCollector evaluateBooleanInput() {
        booleanInputQueryHandler.evaluate().forEach(
                instructionsFromBooleanInput -> offer(
                        Collections.singletonList(instructionsFromBooleanInput),
                        instructions -> new Action(instructions, TermMatches.empty(), 0, 0)));

        return this;
    }

    public abstract void offer(List<Instructions> instructions, Function<Instructions, Action> actionCreator);

    public abstract List<Action> createActions();

    public abstract int getLimit();

    public abstract List<? extends FilterCriterion> getFilters();
}
