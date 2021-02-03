package querqy.rewrite.commonrules.model;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import querqy.ComparableCharSequence;

public class InputTest {

   @Test
   public void testGetInputSequencesForSingleTermWithoutFieldName() {
      char[] s1 = "test".toCharArray();
      Term term1 = new Term(s1, 0, s1.length, null);

      Input input = new Input(Collections.singletonList(term1), false, false);
      List<ComparableCharSequence> sequences = input.getInputSequences(false);
      assertNotNull(sequences);
      assertEquals(1, sequences.size());
      ComparableCharSequence seq = sequences.get(0);
      assertNotNull(seq);

      assertEquals(0, seq.compareTo("test"));

   }

   @Test
   public void testGetInputSequencesForTermsWithoutFieldName() {
      char[] s1 = "test".toCharArray();
      Term term1 = new Term(s1, 0, s1.length, null);

      char[] s2 = "test2".toCharArray();
      Term term2 = new Term(s2, 0, s2.length, null);

      Input input = new Input(Arrays.asList(term1, term2), false, false);
      List<ComparableCharSequence> sequences = input.getInputSequences(false);
      assertNotNull(sequences);
      assertEquals(1, sequences.size());
      ComparableCharSequence seq = sequences.get(0);
      assertNotNull(seq);

      assertEquals(0, seq.compareTo("test test2"));

   }

   @Test
   public void testGetInputSequencesForSingleTermWithFieldName() {
      char[] s1 = "test".toCharArray();
      Term term1 = new Term(s1, 0, s1.length, Collections.singletonList("name1"));

      Input input = new Input(Collections.singletonList(term1), false, false);
      List<ComparableCharSequence> sequences = input.getInputSequences(false);
      assertNotNull(sequences);
      assertEquals(1, sequences.size());
      ComparableCharSequence seq = sequences.get(0);
      assertNotNull(seq);

      assertEquals(0, seq.compareTo("name1:test"));

   }

   @Test
   public void testGetInputSequencesForSingleTermWithFieldNames() {
      char[] s1 = "test".toCharArray();
      Term term1 = new Term(s1, 0, s1.length, Arrays.asList("name1", "name2"));

      Input input = new Input(Collections.singletonList(term1), false, false);
      List<ComparableCharSequence> sequences = input.getInputSequences(false);
      assertNotNull(sequences);
      assertEquals(2, sequences.size());
      ComparableCharSequence seq = sequences.get(0);
      assertNotNull(seq);
      assertEquals(0, seq.compareTo("name1:test"));

      seq = sequences.get(1);
      assertNotNull(seq);
      assertEquals(0, seq.compareTo("name2:test"));

   }

   @Test
   public void testGetInputSequencesForTermsWithFieldNames() {
      char[] s1 = "test".toCharArray();
      Term term1 = new Term(s1, 0, s1.length, Arrays.asList("name1", "name2"));

      char[] s2 = "test2".toCharArray();
      Term term2 = new Term(s2, 0, s2.length, Arrays.asList("name3", "name4"));

      // making up a syntax
      Input input = new Input(Arrays.asList(term1, term2), false, false);
      List<ComparableCharSequence> sequences = input.getInputSequences(false);
      assertNotNull(sequences);
      assertEquals(4, sequences.size());
      ComparableCharSequence seq = sequences.get(0);
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
      Term term1 = new Term(s1, 0, s1.length, Arrays.asList("name1", "name2"));

      char[] s2 = "test2".toCharArray();
      Term term2 = new Term(s2, 0, s2.length, null);

      // making up a syntax
      Input input = new Input(Arrays.asList(term1, term2), false, false);
      List<ComparableCharSequence> sequences = input.getInputSequences(false);
      assertNotNull(sequences);
      assertEquals(2, sequences.size());
      ComparableCharSequence seq = sequences.get(0);
      assertNotNull(seq);
      assertEquals("name1:test test2", seq.toString());

      seq = sequences.get(1);
      assertNotNull(seq);
      assertEquals("name2:test test2", seq.toString());

   }


}
