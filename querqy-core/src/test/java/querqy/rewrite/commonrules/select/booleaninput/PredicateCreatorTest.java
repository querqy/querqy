package querqy.rewrite.commonrules.select.booleaninput;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.Test;
import querqy.rewrite.commonrules.RuleParseException;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInputElement;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInputElement.Type;

import static org.assertj.core.api.Assertions.assertThat;


public class PredicateCreatorTest {

    @Test
    public void testOperatorOr() throws RuleParseException {
        Predicate<boolean[]> predicate;

        predicate = predicate("a OR b");
        assertThat(predicate.test(new boolean[]{true, true})).isTrue();
        assertThat(predicate.test(new boolean[]{false, true})).isTrue();
        assertThat(predicate.test(new boolean[]{true, false})).isTrue();
        assertThat(predicate.test(new boolean[]{false, false})).isFalse();
    }

    @Test
    public void testOperatorAnd() throws RuleParseException {
        Predicate<boolean[]> predicate;

        predicate = predicate("a AND b");
        assertThat(predicate.test(new boolean[]{true, true})).isTrue();
        assertThat(predicate.test(new boolean[]{false, true})).isFalse();
        assertThat(predicate.test(new boolean[]{true, false})).isFalse();
        assertThat(predicate.test(new boolean[]{false, false})).isFalse();
    }

    @Test
    public void testOperatorAndOr() throws RuleParseException {
        Predicate<boolean[]> predicate;

        predicate = predicate("a OR b AND c");
        assertThat(predicate.test(new boolean[]{true, true, true})).isTrue();
        assertThat(predicate.test(new boolean[]{true, true, false})).isTrue();
        assertThat(predicate.test(new boolean[]{true, false, true})).isTrue();
        assertThat(predicate.test(new boolean[]{true, false, false})).isTrue();
        assertThat(predicate.test(new boolean[]{false, true, true})).isTrue();
        assertThat(predicate.test(new boolean[]{false, true, false})).isFalse();
        assertThat(predicate.test(new boolean[]{false, false, true})).isFalse();
        assertThat(predicate.test(new boolean[]{false, false, false})).isFalse();

        predicate = predicate("( a OR b ) AND c");
        assertThat(predicate.test(new boolean[]{true, true, true})).isTrue();
        assertThat(predicate.test(new boolean[]{true, true, false})).isFalse();
        assertThat(predicate.test(new boolean[]{true, false, true})).isTrue();
        assertThat(predicate.test(new boolean[]{true, false, false})).isFalse();
        assertThat(predicate.test(new boolean[]{false, true, true})).isTrue();
        assertThat(predicate.test(new boolean[]{false, true, false})).isFalse();
        assertThat(predicate.test(new boolean[]{false, false, true})).isFalse();
        assertThat(predicate.test(new boolean[]{false, false, false})).isFalse();

    }

    @Test
    public void testNotOperator() throws RuleParseException {
        Predicate<boolean[]> predicate;

        predicate = predicate("NOT a");
        assertThat(predicate.test(new boolean[]{true})).isFalse();
        assertThat(predicate.test(new boolean[]{false})).isTrue();

        predicate = predicate("NOT ( a )");
        assertThat(predicate.test(new boolean[]{true})).isFalse();
        assertThat(predicate.test(new boolean[]{false})).isTrue();

        predicate = predicate("NOT ( NOT a )");
        assertThat(predicate.test(new boolean[]{true})).isTrue();
        assertThat(predicate.test(new boolean[]{false})).isFalse();
    }

    @Test
    public void testOperatorsAndOrNot() throws RuleParseException {
        Predicate<boolean[]> predicate;

        predicate = predicate("( a OR b ) AND NOT c");
        assertThat(predicate.test(new boolean[]{true, true, true})).isFalse();
        assertThat(predicate.test(new boolean[]{true, true, false})).isTrue();
        assertThat(predicate.test(new boolean[]{true, false, true})).isFalse();
        assertThat(predicate.test(new boolean[]{true, false, false})).isTrue();
        assertThat(predicate.test(new boolean[]{false, true, true})).isFalse();
        assertThat(predicate.test(new boolean[]{false, true, false})).isTrue();
        assertThat(predicate.test(new boolean[]{false, false, true})).isFalse();
        assertThat(predicate.test(new boolean[]{false, false, false})).isFalse();

        predicate = predicate("NOT ( a OR b ) AND c");
        assertThat(predicate.test(new boolean[]{true, true, true})).isFalse();
        assertThat(predicate.test(new boolean[]{true, true, false})).isFalse();
        assertThat(predicate.test(new boolean[]{true, false, true})).isFalse();
        assertThat(predicate.test(new boolean[]{true, false, false})).isFalse();
        assertThat(predicate.test(new boolean[]{false, true, true})).isFalse();
        assertThat(predicate.test(new boolean[]{false, true, false})).isFalse();
        assertThat(predicate.test(new boolean[]{false, false, true})).isTrue();
        assertThat(predicate.test(new boolean[]{false, false, false})).isFalse();
    }

    private Predicate<boolean[]> predicate(final String booleanString) throws RuleParseException {
        final AtomicInteger integer = new AtomicInteger();
        return new PredicateCreator(
                elements(booleanString),
                terms -> integer.getAndIncrement()).build();
    }

    private List<BooleanInputElement> elements(final String booleanString) {
        return Arrays.stream(booleanString.split("\\s+"))
                .map(elemAsString -> new BooleanInputElement(elemAsString, Type.getType(elemAsString)))
                .collect(Collectors.toList());
    }
}
