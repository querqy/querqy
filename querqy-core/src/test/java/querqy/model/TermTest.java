package querqy.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class TermTest {

    private List<Term> uniqueTerms = Arrays.asList(
            new Term(null, "a"),
            new Term(null, "b"),
            new Term(null, "x", "a"),
            new Term(null, "x", "b"),
            new Term(null, "y", "a"),
            new Term(null, "y", "b"),
            new Term(null, "x", "aa"),
            new Term(null, "x", "A"),
            new Term(null, "*", "a")
    );

    @Test
    public void testThatToStringReturnsValidCharSequence() {
        assertEquals("abcde", new Term(null, "abcde").toString());
    }


    @Test
    public void sameInstanceIsEqual() {
        for (Term t : uniqueTerms) {
            assertThat(t, is(equalTo(t)));
        }
    }

    @Test
    public void equalIfSameFieldAndValue() {
        for (Term t : uniqueTerms) {
            assertThat(t, is(equalTo(t.clone(null))));
        }
    }

    @Test
    public void notEqualIfDifferentValueOrField() {
        for (int i = 0; i < uniqueTerms.size(); i++) {
            for (int j = 0; j < uniqueTerms.size(); j++) {
                if (i != j) {
                    assertThat(uniqueTerms.get(i), is(not(equalTo(uniqueTerms.get(j)))));
                }
            }
        }
    }

}
