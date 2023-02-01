package querqy.rewrite.commonrules.model;

import org.junit.Test;
import querqy.CharSequenceUtil;
import querqy.model.Input;
import querqy.rewrite.lookup.preprocessing.LookupPreprocessorFactory;
import querqy.rewrite.lookup.preprocessing.LookupPreprocessorType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static querqy.rewrite.commonrules.model.TrieMapRulesCollection.BOUNDARY_WORD;

public class InputSequenceNormalizerTest {

    final static InputSequenceNormalizer IDENTITY = new InputSequenceNormalizer(LookupPreprocessorFactory.identity());
    final static InputSequenceNormalizer GERMAN = new InputSequenceNormalizer(LookupPreprocessorFactory
            .fromType(LookupPreprocessorType.GERMAN));

    @Test
    public void testGetInputSequencesForSingleTermWithoutFieldName() {
        char[] s1 = "test".toCharArray();
        Term term1 = new Term(s1, 0, s1.length, null);

        Input.SimpleInput input = new Input.SimpleInput(Collections.singletonList(term1), false,
                false, new String(s1));

        List<CharSequence> sequences = IDENTITY.getNormalizedInputSequences(input);

        assertNotNull(sequences);
        assertEquals(1, sequences.size());
        CharSequence seq = sequences.get(0);
        assertNotNull(seq);

        assertEquals(0, CharSequenceUtil.compare(seq, "test"));

    }

    @Test
    public void testGetInputSequencesForTermsWithoutFieldName() {
        char[] s1 = "test".toCharArray();
        Term term1 = new Term(s1, 0, s1.length, null);

        char[] s2 = "test2".toCharArray();
        Term term2 = new Term(s2, 0, s2.length, null);

        Input.SimpleInput input = new Input.SimpleInput(asList(term1, term2), false, false,
                "test test2");

        List<CharSequence> sequences = IDENTITY.getNormalizedInputSequences(input);
        assertNotNull(sequences);
        assertEquals(1, sequences.size());
        CharSequence seq = sequences.get(0);
        assertNotNull(seq);

        assertEquals(0, CharSequenceUtil.compare(seq, "test test2"));

    }

    @Test
    public void testGetInputSequencesForSingleTermWithFieldName() {
        char[] s1 = "test".toCharArray();
        Term term1 = new Term(s1, 0, s1.length, Collections.singletonList("name1"));

        Input.SimpleInput input = new Input.SimpleInput(Collections.singletonList(term1), false,
                false, new String(s1));
        List<CharSequence> sequences = IDENTITY.getNormalizedInputSequences(input);
        assertNotNull(sequences);
        assertEquals(1, sequences.size());
        CharSequence seq = sequences.get(0);
        assertNotNull(seq);

        assertEquals(0, CharSequenceUtil.compare(seq, "name1:test"));

    }

    @Test
    public void testGetInputSequencesForSingleTermWithFieldNames() {
        char[] s1 = "test".toCharArray();
        Term term1 = new Term(s1, 0, s1.length, asList("name1", "name2"));

        Input.SimpleInput input = new Input.SimpleInput(Collections.singletonList(term1), false,
                false, new String(s1));
        List<CharSequence> sequences = IDENTITY.getNormalizedInputSequences(input);
        assertNotNull(sequences);
        assertEquals(2, sequences.size());
        CharSequence seq = sequences.get(0);
        assertNotNull(seq);
        assertEquals(0, CharSequenceUtil.compare(seq, "name1:test"));

        seq = sequences.get(1);
        assertNotNull(seq);
        assertEquals(0, CharSequenceUtil.compare(seq, "name2:test"));

    }

    @Test
    public void testGetInputSequencesForTermsWithFieldNames() {
        char[] s1 = "test".toCharArray();
        Term term1 = new Term(s1, 0, s1.length, asList("name1", "name2"));

        char[] s2 = "test2".toCharArray();
        Term term2 = new Term(s2, 0, s2.length, asList("name3", "name4"));

        // making up a syntax
        Input.SimpleInput input = new Input.SimpleInput(asList(term1, term2), false, false,
                "test test2");
        List<CharSequence> sequences = IDENTITY.getNormalizedInputSequences(input);
        assertNotNull(sequences);
        assertEquals(4, sequences.size());
        CharSequence seq = sequences.get(0);
        assertNotNull(seq);
        assertEquals("name1:test name3:test2", seq.toString());

        seq = sequences.get(1);
        assertNotNull(seq);
        assertEquals("name1:test name4:test2", seq.toString());

        seq = sequences.get(2);
        assertNotNull(seq);
        assertEquals("name2:test name3:test2", seq.toString());

        seq = sequences.get(3);
        assertNotNull(seq);
        assertEquals("name2:test name4:test2", seq.toString());

    }

    @Test
    public void testGetInputSequencesForTermsWithAndWithoutFieldNames() {
        char[] s1 = "test".toCharArray();
        Term term1 = new Term(s1, 0, s1.length, asList("name1", "name2"));

        char[] s2 = "test2".toCharArray();
        Term term2 = new Term(s2, 0, s2.length, null);

        // making up a syntax
        Input.SimpleInput input = new Input.SimpleInput(asList(term1, term2), false, false,
                "test test2");
        List<CharSequence> sequences = IDENTITY.getNormalizedInputSequences(input);
        assertNotNull(sequences);
        assertEquals(2, sequences.size());
        CharSequence seq = sequences.get(0);
        assertNotNull(seq);
        assertEquals("name1:test test2", seq.toString());

        seq = sequences.get(1);
        assertNotNull(seq);
        assertEquals("name2:test test2", seq.toString());

    }

    @Test
    public void testThatPrefixTermWithFieldNameDoesNotGetLowerCasedForIdentityPreprocessor() {
        char[] s1 = "TEST".toCharArray();
        Term term1 = new PrefixTerm(s1, 0, s1.length, Collections.singletonList("name1"));
        Input.SimpleInput input = new Input.SimpleInput(Collections.singletonList(term1), false, false,
                "name1:TEST*");
        List<CharSequence> sequences = IDENTITY.getNormalizedInputSequences(input);
        assertNotNull(sequences);
        assertEquals(1, sequences.size());
        CharSequence seq = sequences.get(0);
        assertNotNull(seq);
        assertEquals("name1:TEST", seq.toString());
    }

    @Test
    public void testThatPrefixTermWithoutFieldNameDoesNotGetLowerCasedForIdentityPreprocessor() {
        char[] s1 = "TEST".toCharArray();
        Term term1 = new PrefixTerm(s1, 0, s1.length, null);
        Input.SimpleInput input = new Input.SimpleInput(Collections.singletonList(term1), false, false,
                "TEST*");
        List<CharSequence> sequences = IDENTITY.getNormalizedInputSequences(input);
        assertNotNull(sequences);
        assertEquals(1, sequences.size());
        CharSequence seq = sequences.get(0);
        assertNotNull(seq);
        assertEquals("TEST", seq.toString());
    }

    @Test
    public void testThatSimpleTermWithFieldNameDoesNotGetLowerCasedForIdentityPreprocessor() {
        char[] s1 = "TEST".toCharArray();
        Term term1 = new Term(s1, 0, s1.length, Collections.singletonList("name1"));
        Input.SimpleInput input = new Input.SimpleInput(Collections.singletonList(term1), false, false,
                "name1:TEST*");
        List<CharSequence> sequences = IDENTITY.getNormalizedInputSequences(input);
        assertNotNull(sequences);
        assertEquals(1, sequences.size());
        CharSequence seq = sequences.get(0);
        assertNotNull(seq);
        assertEquals("name1:TEST", seq.toString());
    }

    @Test
    public void testThatSimpleTermWithoutFieldNameDoesNotGetLowerCasedForIdentityPreprocessor() {
        char[] s1 = "TEST".toCharArray();
        Term term1 = new Term(s1, 0, s1.length, null);
        Input.SimpleInput input = new Input.SimpleInput(Collections.singletonList(term1), false, false,
                "TEST");
        List<CharSequence> sequences = IDENTITY.getNormalizedInputSequences(input);
        assertNotNull(sequences);
        assertEquals(1, sequences.size());
        CharSequence seq = sequences.get(0);
        assertNotNull(seq);
        assertEquals("TEST", seq.toString());
    }

    @Test
    public void testThatOnlyLowerCaseIsAppliedToPrefixTermForNonIdentityPreprocessor() {
        char[] s1 = "Glueck".toCharArray();
        Term term1 = new PrefixTerm(s1, 0, s1.length, null);
        Input.SimpleInput input = new Input.SimpleInput(Collections.singletonList(term1), false, false,
                "Glueck*");
        List<CharSequence> sequences = GERMAN.getNormalizedInputSequences(input);
        assertNotNull(sequences);
        assertEquals(1, sequences.size());
        CharSequence seq = sequences.get(0);
        assertNotNull(seq);
        assertEquals("glueck", seq.toString());
    }

    @Test
    public void testThatNonIdentityPreprocessorIsAppliedToSimpleTerm() {
        char[] s1 = "Glueck".toCharArray();
        Term term1 = new Term(s1, 0, s1.length, null);
        Input.SimpleInput input = new Input.SimpleInput(Collections.singletonList(term1), false, false,
                "Glueck");
        List<CharSequence> sequences = GERMAN.getNormalizedInputSequences(input);
        assertNotNull(sequences);
        assertEquals(1, sequences.size());
        CharSequence seq = sequences.get(0);
        assertNotNull(seq);
        assertEquals("glück", seq.toString());
    }

    @Test
    public void testThatLeftBoundaryIsAppliedToSingleTerm() {
        char[] s1 = "Glueck".toCharArray();
        Term term1 = new Term(s1, 0, s1.length, null);
        Input.SimpleInput input = new Input.SimpleInput(Collections.singletonList(term1), true, false,
                "\"Glueck");
        List<CharSequence> sequences = GERMAN.getNormalizedInputSequences(input);
        assertNotNull(sequences);
        assertEquals(1, sequences.size());
        CharSequence seq = sequences.get(0);
        assertNotNull(seq);
        assertEquals(BOUNDARY_WORD + " glück", seq.toString());
    }

    @Test
    public void testThatRightBoundaryIsAppliedToSingleTerm() {
        char[] s1 = "Glueck".toCharArray();
        Term term1 = new Term(s1, 0, s1.length, null);
        Input.SimpleInput input = new Input.SimpleInput(Collections.singletonList(term1), false, true,
                "Glueck\"");
        List<CharSequence> sequences = GERMAN.getNormalizedInputSequences(input);
        assertNotNull(sequences);
        assertEquals(1, sequences.size());
        CharSequence seq = sequences.get(0);
        assertNotNull(seq);
        assertEquals( "glück " + BOUNDARY_WORD, seq.toString());
    }

    @Test
    public void testThatBothBoundariesAreAppliedToSingleTerm() {
        char[] s1 = "Glueck".toCharArray();
        Term term1 = new Term(s1, 0, s1.length, null);
        Input.SimpleInput input = new Input.SimpleInput(Collections.singletonList(term1), true, true,
                "\"Glueck\"");
        List<CharSequence> sequences = GERMAN.getNormalizedInputSequences(input);
        assertNotNull(sequences);
        assertEquals(1, sequences.size());
        CharSequence seq = sequences.get(0);
        assertNotNull(seq);
        assertEquals( BOUNDARY_WORD + " glück " + BOUNDARY_WORD, seq.toString());
    }

    @Test
    public void testThatBothBoundariesAreAppliedToMultiTermInput() {
        char[] s1 = "Stueck".toCharArray();
        Term term1 = new Term(s1, 0, s1.length, null);

        char[] s2 = "vom".toCharArray();
        Term term2 = new Term(s2, 0, s2.length, null);

        char[] s3 = "Glueck".toCharArray();
        Term term3 = new Term(s3, 0, s3.length, null);

        Input.SimpleInput input = new Input.SimpleInput(Arrays.asList(term1, term2, term3), true, true,
                "\"Stueck vom Glueck\"");
        List<CharSequence> sequences = GERMAN.getNormalizedInputSequences(input);
        assertNotNull(sequences);
        assertEquals(1, sequences.size());
        CharSequence seq = sequences.get(0);
        assertNotNull(seq);
        assertEquals( BOUNDARY_WORD + " stück vom glück " + BOUNDARY_WORD, seq.toString());
    }


}