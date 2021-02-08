package querqy.lucene.rewrite;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class BoostedDelegatingFieldBoostTest {

    @Test
    public void shouldDoProperToString() {
        assertThat(new ConstantFieldBoost(666f).toString("title"), 
            is("^ConstantFieldBoost(title^666.0)"));
        assertThat(new BoostedDelegatingFieldBoost(new ConstantFieldBoost(666f), 2f).toString("title"), 
            is("^BoostedDelegatingFieldBoost(^ConstantFieldBoost(title^666.0)^2.0)"));
    }

    @Test
    public void testCorrectEquals() {
        BoostedDelegatingFieldBoost first = new BoostedDelegatingFieldBoost(new ConstantFieldBoost(666f), 4f);
        BoostedDelegatingFieldBoost second = new BoostedDelegatingFieldBoost(new ConstantFieldBoost(666f), 4f);
        BoostedDelegatingFieldBoost third = new BoostedDelegatingFieldBoost(new ConstantFieldBoost(666f), 4.000001f);
        BoostedDelegatingFieldBoost fourth = new BoostedDelegatingFieldBoost(new ConstantFieldBoost(667f), 4f);

        assertEquals(first, second);
        assertNotEquals(first, third);
        assertNotEquals(first, fourth);
        assertNotEquals(third, fourth);
    }
    
    @Test
    public void testCorrectHashCode() {
        BoostedDelegatingFieldBoost first = new BoostedDelegatingFieldBoost(new ConstantFieldBoost(666f), 4f);
        BoostedDelegatingFieldBoost second = new BoostedDelegatingFieldBoost(new ConstantFieldBoost(666f), 4f);
        BoostedDelegatingFieldBoost third = new BoostedDelegatingFieldBoost(new ConstantFieldBoost(666f), 4.000001f);
        BoostedDelegatingFieldBoost fourth = new BoostedDelegatingFieldBoost(new ConstantFieldBoost(667f), 4f);

        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first.hashCode(), third.hashCode());
        assertNotEquals(first.hashCode(), fourth.hashCode());
        assertNotEquals(third.hashCode(), fourth.hashCode());
    }

}
