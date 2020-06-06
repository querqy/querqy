package querqy.rewrite.contrib.replace;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class WildcardReplaceInstructionTest {

    private List<CharSequence> list(String... seqs) {
        return new ArrayList<>(Arrays.asList(seqs));
    }

    private List<String> strlist(List<CharSequence> seqs) {
        return seqs.stream().map(CharSequence::toString).collect(Collectors.toList());
    }

    @Test
    public void testRemoveOneTerm() {
        WildcardReplaceInstruction wildcardReplaceInstruction =
                new WildcardReplaceInstruction(list());

        List<CharSequence> input = list("a", "b");
        wildcardReplaceInstruction.apply(input, 1, 1, "d");
        assertThat(strlist(input)).containsExactly("a");
    }

    @Test
    public void testRemoveManyTerms() {
        WildcardReplaceInstruction wildcardReplaceInstruction =
                new WildcardReplaceInstruction(list());

        List<CharSequence> input = list("a", "b", "c");
        wildcardReplaceInstruction.apply(input, 1, 2, "d");
        assertThat(strlist(input)).containsExactly("a");
    }

    @Test
    public void testReplaceOneTermByOne() {
        WildcardReplaceInstruction wildcardReplaceInstruction = new WildcardReplaceInstruction(list("c"));

        List<CharSequence> input = list("a", "b");
        wildcardReplaceInstruction.apply(input, 1, 1, "d");
        assertThat(strlist(input)).containsExactly("a", "c");
    }

    @Test
    public void testReplaceOneTermByOneWildcardMatchIsPartOfTerm() {
        WildcardReplaceInstruction wildcardReplaceInstruction = new WildcardReplaceInstruction(list("c$1"));

        List<CharSequence> input = list("a", "b");
        wildcardReplaceInstruction.apply(input, 1, 1, "d");
        assertThat(strlist(input)).containsExactly("a", "cd");

        wildcardReplaceInstruction = new WildcardReplaceInstruction(list("$1e"));

        input = list("a", "b");
        wildcardReplaceInstruction.apply(input, 1, 1, "d");
        assertThat(strlist(input)).containsExactly("a", "de");

        wildcardReplaceInstruction = new WildcardReplaceInstruction(list("d$1f"));

        input = list("a", "b");
        wildcardReplaceInstruction.apply(input, 1, 1, "e");
        assertThat(strlist(input)).containsExactly("a", "def");
    }

    @Test
    public void testReplaceOneTermByManyIncludingWildcardMatch() {

        WildcardReplaceInstruction wildcardReplaceInstruction =
                new WildcardReplaceInstruction(list("c", "$1"));

        List<CharSequence> input = list("a", "b");
        wildcardReplaceInstruction.apply(input, 0, 1, "d");
        assertThat(strlist(input)).containsExactly("c", "d", "b");

        input = list("a", "b");
        wildcardReplaceInstruction.apply(input, 1, 1, "d");
        assertThat(strlist(input)).containsExactly("a", "c", "d");

        input = list("a", "b", "e");
        wildcardReplaceInstruction.apply(input, 1, 1, "d");
        assertThat(strlist(input)).containsExactly("a", "c", "d", "e");
    }

    @Test
    public void testReplaceManyTermsByManyIncludingWildcardMatch() {

        WildcardReplaceInstruction wildcardReplaceInstruction =
                new WildcardReplaceInstruction(list("d", "$1", "f"));

        List<CharSequence> input = list("a", "b", "c");
        wildcardReplaceInstruction.apply(input, 0, 2, "e");
        assertThat(strlist(input)).containsExactly("d", "e", "f", "c");

        input = list("a", "b", "c");
        wildcardReplaceInstruction.apply(input, 1, 2, "e");
        assertThat(strlist(input)).containsExactly("a", "d", "e", "f");
    }
}