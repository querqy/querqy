package querqy.lucene.rewrite;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by rene on 04/09/2016.
 */
public class ConstantFieldBoostTest {

    @Test
    public void testThatEqualsDependsOnBoostFactor() throws Exception {

        ConstantFieldBoost fieldBoost1 = new ConstantFieldBoost(1f);
        ConstantFieldBoost fieldBoost1a = new ConstantFieldBoost(1f);
        ConstantFieldBoost fieldBoost2 = new ConstantFieldBoost(2f);

        assertEquals(fieldBoost1, fieldBoost1a);

        assertNotEquals(fieldBoost1, fieldBoost2);
    }

    @Test
    public void testThatHashCodeDependsOnBoostFactor() throws Exception {

        ConstantFieldBoost fieldBoost1 = new ConstantFieldBoost(1f);
        ConstantFieldBoost fieldBoost1a = new ConstantFieldBoost(1f);
        ConstantFieldBoost fieldBoost2 = new ConstantFieldBoost(2f);

        assertEquals(fieldBoost1.hashCode(), fieldBoost1a.hashCode());

        assertNotEquals(fieldBoost1.hashCode(), fieldBoost2.hashCode());

    }
}
