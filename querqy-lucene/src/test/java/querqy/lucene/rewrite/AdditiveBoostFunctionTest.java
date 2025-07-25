package querqy.lucene.rewrite;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.valuesource.DoubleConstValueSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.HashMap;

@RunWith(MockitoJUnitRunner.class)
public class AdditiveBoostFunctionTest {

    @Mock
    ValueSource scoringFunction1, scoringFunction2;

    @Mock
    FunctionValues scoringFunctionValues1, scoringFunctionValues2;

    @Before
    public void setUp() throws IOException {
        Mockito.reset(scoringFunction1, scoringFunction2, scoringFunctionValues1, scoringFunctionValues2);
        when(scoringFunction1.getValues(ArgumentMatchers.anyMap(), ArgumentMatchers.any()))
                .thenReturn(scoringFunctionValues1);
        when(scoringFunction2.getValues(ArgumentMatchers.anyMap(), ArgumentMatchers.any()))
                .thenReturn(scoringFunctionValues2);

    }

    @Test
    public void testThatTheBoostValueIsReturnedIfDocDoesntMatchNegBoostScoringFunction() throws IOException {

        when(scoringFunctionValues1.exists(ArgumentMatchers.eq(1))).thenReturn(false);

        final AdditiveBoostFunction func = new AdditiveBoostFunction(scoringFunction1, -20f);
        final FunctionValues values = func.getValues(new HashMap(), null);
        assertEquals(20f, values.floatVal(1), 0.00001);

    }

    @Test
    public void testThatTheBoostValueIsReturnedIfNegBoostScoringFunctionReturnsZero() throws IOException {

        when(scoringFunctionValues1.exists(ArgumentMatchers.eq(1))).thenReturn(true);
        when(scoringFunctionValues1.floatVal(ArgumentMatchers.eq(1))).thenReturn(0f);

        final AdditiveBoostFunction func = new AdditiveBoostFunction(scoringFunction1, -20f);
        final FunctionValues values = func.getValues(new HashMap(), null);
        assertEquals(20f, values.floatVal(1), 0.00001);

    }

    @Test
    public void testGradedScoresForNegBoostScoringFunction() throws IOException {

        when(scoringFunctionValues1.exists(ArgumentMatchers.eq(1))).thenReturn(true);
        when(scoringFunctionValues1.floatVal(ArgumentMatchers.eq(1))).thenReturn(0f);

        when(scoringFunctionValues1.exists(ArgumentMatchers.eq(2))).thenReturn(true);
        when(scoringFunctionValues1.floatVal(ArgumentMatchers.eq(2))).thenReturn(1f);


        when(scoringFunctionValues1.exists(ArgumentMatchers.eq(3))).thenReturn(true);
        when(scoringFunctionValues1.floatVal(ArgumentMatchers.eq(3))).thenReturn(4f);

        final AdditiveBoostFunction func = new AdditiveBoostFunction(scoringFunction1, -20f);
        final FunctionValues values = func.getValues(new HashMap(), null);

        float match0Score = values.floatVal(1);
        float match1Score = values.floatVal(2);
        float match4Score = values.floatVal(3);

        assertEquals(20f, match0Score, 0.00001);
        assertTrue(match1Score < match0Score);
        assertTrue(match4Score < match1Score);

    }


    @Test
    public void testRelativeNegativeBoostValue() throws IOException {

        when(scoringFunctionValues1.exists(ArgumentMatchers.eq(1))).thenReturn(true);
        when(scoringFunctionValues1.floatVal(ArgumentMatchers.eq(1))).thenReturn(2f);

        when(scoringFunctionValues2.exists(ArgumentMatchers.eq(1))).thenReturn(false);


        when(scoringFunctionValues1.exists(ArgumentMatchers.eq(2))).thenReturn(false);

        when(scoringFunctionValues2.exists(ArgumentMatchers.eq(2))).thenReturn(true);
        when(scoringFunctionValues2.floatVal(ArgumentMatchers.eq(2))).thenReturn(2f);

        final AdditiveBoostFunction func1 = new AdditiveBoostFunction(scoringFunction1, -20f);
        final AdditiveBoostFunction func2 = new AdditiveBoostFunction(scoringFunction2, -10f);

        final FunctionValues values1 = func1.getValues(new HashMap(), null);
        final FunctionValues values2 = func2.getValues(new HashMap(), null);

        // doc 1 matches the higher neg boost
        // doc 2 matches the lower neg boost
        assertTrue(values1.floatVal(1) + values2.floatVal(1) < values1.floatVal(2) + values2.floatVal(2));

    }

    @Test
    public void testThatPositiveAndNegativeBoostEqualEachOtherOut() throws IOException {

        // doc 1 matches pos and neg boost, doc 2 matches neither
        when(scoringFunctionValues1.exists(ArgumentMatchers.eq(1))).thenReturn(true);
        when(scoringFunctionValues1.floatVal(ArgumentMatchers.eq(1))).thenReturn(2f);

        when(scoringFunctionValues2.exists(ArgumentMatchers.eq(1))).thenReturn(true);
        when(scoringFunctionValues2.floatVal(ArgumentMatchers.eq(1))).thenReturn(2f);


        when(scoringFunctionValues1.exists(ArgumentMatchers.eq(2))).thenReturn(false);
        when(scoringFunctionValues2.exists(ArgumentMatchers.eq(2))).thenReturn(false);


        final AdditiveBoostFunction func1 = new AdditiveBoostFunction(scoringFunction1, -20f);
        final AdditiveBoostFunction func2 = new AdditiveBoostFunction(scoringFunction2, 20f);

        final FunctionValues values1 = func1.getValues(new HashMap(), null);
        final FunctionValues values2 = func2.getValues(new HashMap(), null);

        assertEquals(values1.floatVal(1) + values2.floatVal(1), values1.floatVal(2) + values2.floatVal(2), 0.0001f);

    }

    @Test
    public void testEquals() {

        final AdditiveBoostFunction func1 = new AdditiveBoostFunction(new DoubleConstValueSource(Math.PI), 1f);
        final AdditiveBoostFunction func2 = new AdditiveBoostFunction(new DoubleConstValueSource(Math.PI), 1f);
        final AdditiveBoostFunction func3 = new AdditiveBoostFunction(new DoubleConstValueSource(Math.E), 1f);
        final AdditiveBoostFunction func4 = new AdditiveBoostFunction(new DoubleConstValueSource(Math.E), 2f);
        final AdditiveBoostFunction func5 = new AdditiveBoostFunction(new DoubleConstValueSource(Math.E), -2f);
        final AdditiveBoostFunction func6 = new AdditiveBoostFunction(new DoubleConstValueSource(Math.E), -2f);

        assertEquals(func1, func1);
        assertEquals(func1, func2);
        assertEquals(func2, func1);
        assertEquals(func2, func2);
        assertEquals(func5, func6);

        assertFalse(func1.equals(null));
        assertNotEquals(func3, func1);
        assertNotEquals(func3, func4);
        assertNotEquals(func4, func5);

    }

    @Test
    public void testHashCode() {
        final AdditiveBoostFunction func1 = new AdditiveBoostFunction(new DoubleConstValueSource(Math.PI), 1f);
        final AdditiveBoostFunction func2 = new AdditiveBoostFunction(new DoubleConstValueSource(Math.PI), 1f);
        final AdditiveBoostFunction func3 = new AdditiveBoostFunction(new DoubleConstValueSource(Math.E), 1f);
        final AdditiveBoostFunction func4 = new AdditiveBoostFunction(new DoubleConstValueSource(Math.E), 2f);

        assertEquals(func1.hashCode(), func2.hashCode());
        assertNotEquals(func2.hashCode(), func3.hashCode());
        assertNotEquals(func3.hashCode(), func4.hashCode());

    }

    @Test
    public void testDescription() {

        when(scoringFunction1.description()).thenReturn("test desc");

        final AdditiveBoostFunction func = new AdditiveBoostFunction(scoringFunction1, -2f);
        final String description = func.description();
        assertTrue(description.contains("AdditiveBoostFunction"));
        assertTrue(description.contains("test desc"));
        assertTrue(description.contains("-2"));

    }
}