package querqy.rewrite.commonrules.select.booleaninput;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import querqy.rewrite.commonrules.RuleParseException;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInputElement;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInputElement.Type;

public class PredicateCreator {

    private final List<BooleanInputElement> elements;
    private final ToIntFunction<List<String>> createReferenceIdFunction;

    private BooleanInputElement nextHighestPriorityElement = null;
    private int indexOfNextHighestPriorityElement = -1;

    public PredicateCreator(final List<BooleanInputElement> elements, final ToIntFunction<List<String>> createReferenceIdFunction)
            throws RuleParseException {
        this.elements = removeEncapsulatingParentheses(elements);
        this.createReferenceIdFunction = createReferenceIdFunction;

        findNextHighestPriorityElement();

        // should not happen as boolean input is validated before
        if (nextHighestPriorityElement == null || indexOfNextHighestPriorityElement < 0) {
            throw new RuleParseException(
                    String.format("Something unexpected happened while parsing a boolean input string: %s", elements));
        }
    }

    public Predicate<boolean[]> build() throws RuleParseException {
        switch (nextHighestPriorityElement.type) {
            case AND:
            case OR:
                final Predicate<boolean[]> left = new PredicateCreator(
                        elements.subList(0, indexOfNextHighestPriorityElement), createReferenceIdFunction).build();

                final Predicate<boolean[]> right = new PredicateCreator(
                        elements.subList(indexOfNextHighestPriorityElement + 1, elements.size()), createReferenceIdFunction).build();

                return nextHighestPriorityElement.type == Type.AND ? left.and(right) : left.or(right);

            case NOT:
                final Predicate<boolean[]> predicate = new PredicateCreator(
                        elements.subList(1, elements.size()), createReferenceIdFunction).build();

                return predicate.negate();

            case TERM:
                final List<String> terms = elements.stream().map(element -> element.term).collect(Collectors.toList());
                final int referenceId = createReferenceIdFunction.applyAsInt(terms);
                return booleans -> booleans[referenceId];

            default:
                // should not happen
                throw new RuleParseException(
                        String.format("Something unexpected happened while parsing a boolean input string: %s", elements));
        }
    }

    private List<BooleanInputElement> removeEncapsulatingParentheses(List<BooleanInputElement> elements) {
        while (elements.size() >= 2 &&
                elements.get(0).type == Type.LEFT_PARENTHESIS &&
                elements.get(elements.size() - 1).type == Type.RIGHT_PARENTHESIS) {
            elements = elements.subList(1, elements.size() - 1);
        }

        return elements;
    }

    private void findNextHighestPriorityElement() {
        int numberOpenGroups = 0;

        for (int i = 0; i < elements.size(); i++) {
            BooleanInputElement element = elements.get(i);

            if (element.type == Type.LEFT_PARENTHESIS) {
                numberOpenGroups++;

            } else if (element.type == Type.RIGHT_PARENTHESIS) {
                numberOpenGroups--;

            } else {
                if (numberOpenGroups == 0 && (nextHighestPriorityElement == null ||
                        element.type.getPriority() > nextHighestPriorityElement.type.getPriority())) {
                    nextHighestPriorityElement = element;
                    indexOfNextHighestPriorityElement = i;
                }
            }
        }
    }
}
