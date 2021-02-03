package querqy.rewrite.commonrules.model;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import querqy.model.ExpandedQuery;
import querqy.model.InputSequenceElement;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.commonrules.AbstractCommonRulesTest;

public class RulesCollectionTest {

   @Test
   public void testSingeInputSingleInstruction() {

      RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

      String s1 = "test";

      Input input = new Input(inputTerms(null, s1), false, false);

      Instructions instructions = instructions(1, "instruction1");
      builder.addRule(input, instructions);

      RulesCollection rulesCollection = builder.build();
      PositionSequence<InputSequenceElement> sequence = new PositionSequence<>();
      sequence.nextPosition();
      sequence.addElement(new Term(null, s1));

      List<Action> actions = AbstractCommonRulesTest.getActions(rulesCollection, sequence);
      assertThat(actions, contains(new Action(instructions, termMatches(s1), 0, 1)));

   }

   @Test
   public void testSingleInputTwoInstructionsFromSameRule() {

      RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

      String s1 = "test";

      Input input = new Input(inputTerms(null, s1), false, false);

      Instructions instructions = instructions(1, "instruction1", "instruction2");
      builder.addRule(input, instructions);

      RulesCollection rulesCollection = builder.build();
      PositionSequence<InputSequenceElement> sequence = new PositionSequence<>();
      sequence.nextPosition();
      sequence.addElement(new Term(null, s1));

      List<Action> actions = AbstractCommonRulesTest.getActions(rulesCollection, sequence);
      assertThat(actions, contains(new Action(instructions, termMatches(s1), 0, 1)));

   }

   @Test
   public void testSameInputTwoInstructionsFromDiffentRules() {

      RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

      String s1 = "test";

      Input input = new Input(inputTerms(null, s1), false, false);

      Instructions instructions1 = instructions(1, "instruction1");
      builder.addRule(input, instructions1);

      Instructions instructions2 = instructions(2, "instruction2");
      builder.addRule(input, instructions2);

      RulesCollection rulesCollection = builder.build();
      PositionSequence<InputSequenceElement> sequence = new PositionSequence<>();
      sequence.nextPosition();
      sequence.addElement(new Term(null, s1));

      List<Action> actions = AbstractCommonRulesTest.getActions(rulesCollection, sequence);
      assertThat(actions, contains(
              new Action(instructions1, termMatches(s1), 0, 1),
              new Action(instructions2, termMatches(s1), 0, 1)));

   }

   @Test
   public void testTwoInputsOneInstructionsPerInputOrdered() {

      RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

      String s1 = "test1";
      String s2 = "test2";

      Input input1 = new Input(inputTerms(null, s1), false, false);
      Input input2 = new Input(inputTerms(null, s2), false, false);

      Instructions instructions1 = instructions(1, "instruction1");
      builder.addRule(input1, instructions1);

      Instructions instructions2 = instructions(2, "instruction2");
      builder.addRule(input2, instructions2);

      // Input is just s1
      RulesCollection rulesCollection = builder.build();
      PositionSequence<InputSequenceElement> sequence = new PositionSequence<>();
      sequence.nextPosition();
      sequence.addElement(new Term(null, s1));

      List<Action> actions = AbstractCommonRulesTest.getActions(rulesCollection, sequence);

      assertThat(actions, contains(new Action(instructions1, termMatches(s1), 0, 1)));

      // Input is just s2
      sequence = new PositionSequence<>();
      sequence.nextPosition();
      sequence.addElement(new Term(null, s2));

      actions = AbstractCommonRulesTest.getActions(rulesCollection, sequence);
      assertThat(actions, contains(new Action(instructions2, termMatches(s2), 0, 1)));

      // Input is s2 s1, but order based on ord is s1 s2
      sequence.nextPosition();
      sequence.addElement(new Term(null, s1));

      actions = AbstractCommonRulesTest.getActions(rulesCollection, sequence);
      assertThat(actions, contains(
            new Action(instructions1, termMatches(s1), 1, 2),
            new Action(instructions2, termMatches(s2), 0, 1))

      );

   }

   @Test
   public void testCompoundAndInterlacedInput() {
      RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

      String s1 = "test1";
      String s2 = "test2";
      String s3 = "test3";

      Input input1 = new Input(inputTerms(null, s1, s2), false, false);
      Input input2 = new Input(inputTerms(null, s2, s3), false, false);

      Instructions instructions1 = instructions(1, "instruction1");
      builder.addRule(input1, instructions1);

      Instructions instructions2 = instructions(2, "instruction2");
      builder.addRule(input2, instructions2);

      // Input is just s1
      RulesCollection rulesCollection = builder.build();
      PositionSequence<InputSequenceElement> sequence = new PositionSequence<>();
      sequence.nextPosition();
      sequence.addElement(new Term(null, s1));

      List<Action> actions = AbstractCommonRulesTest.getActions(rulesCollection, sequence);
      assertTrue(actions.isEmpty());

      // Input is s1 s2
      sequence.nextPosition();
      sequence.addElement(new Term(null, s2));

      actions = AbstractCommonRulesTest.getActions(rulesCollection, sequence);
      assertThat(actions, contains(
            new Action(instructions1, termMatches(s1, s2), 0, 2))

      );

      // Input is s1 s2 s3
      sequence.nextPosition();
      sequence.addElement(new Term(null, s3));
      actions = AbstractCommonRulesTest.getActions(rulesCollection, sequence);
      assertThat(actions, contains(
            new Action(instructions1, termMatches(s1, s2), 0, 2),
            new Action(instructions2, termMatches(s2, s3), 1, 3)
            )

      );

   }

   @Test
   public void testTwoMatchingInputsOnePartial() {
      RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

      String s1 = "test1";
      String s2 = "test2";

      Input input1 = new Input(inputTerms(null, s1, s2), false, false);
      Input input2 = new Input(inputTerms(null, s2), false, false);

      Instructions instructions1 = instructions(1, "instruction1");
      Instructions instructions2 = instructions(2, "instruction2");

      builder.addRule(input2, instructions2);
      builder.addRule(input1, instructions1);

      // Input is s1 s2
      RulesCollection rulesCollection = builder.build();
      PositionSequence<InputSequenceElement> sequence = new PositionSequence<>();
      sequence.nextPosition();
      sequence.addElement(new Term(null, s1));

      sequence.nextPosition();
      sequence.addElement(new Term(null, s2));

      List<Action> actions = AbstractCommonRulesTest.getActions(rulesCollection, sequence);
      assertThat(actions, contains(
            new Action(instructions1, termMatches(s1, s2), 0, 2),
            new Action(instructions2, termMatches(s2), 1, 2)

            )

      );

   }

   @Test
   public void testMultipleTermsPerPosition() {
      RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

      String s1 = "test1";
      String s2 = "test2";

      Input input1 = new Input(inputTerms(null, s1), false, false);
      Input input2 = new Input(inputTerms(null, s2), false, false);

      Instructions instructions1 = instructions(1, "instruction1");
      builder.addRule(input1, instructions1);

      Instructions instructions2 = instructions(2, "instruction2");
      builder.addRule(input2, instructions2);

      // Input is just s1
      RulesCollection rulesCollection = builder.build();
      PositionSequence<InputSequenceElement> sequence = new PositionSequence<>();
      sequence.nextPosition();
      sequence.addElement(new Term(null, s1));

      List<Action> actions = AbstractCommonRulesTest.getActions(rulesCollection, sequence);
      assertThat(actions, contains(new Action(instructions1, termMatches(s1), 0, 1)));

      sequence.addElement(new Term(null, s2));

      actions = AbstractCommonRulesTest.getActions(rulesCollection, sequence);
      assertThat(actions, contains(new Action(instructions1, termMatches(s1), 0, 1),
            new Action(instructions2, termMatches(s2), 0, 1)));

   }

   @Test
   public void testMultipleTermsWithFieldNamesPerPosition() {
      RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

      String s1 = "test1";
      String s2 = "test2";

      Input input1 = new Input(
            Arrays.asList(
                  new querqy.rewrite.commonrules.model.Term(s1.toCharArray(), 0, s1.length(), Arrays.asList("f11",
                        "f12")),
                  new querqy.rewrite.commonrules.model.Term(s2.toCharArray(), 0, s2.length(), Arrays.asList("f21",
                        "f22"))

                  ), false, false);

      Instructions instructions1 = instructions(1, "instruction1");
      builder.addRule(input1, instructions1);

      Term term11 = new Term(null, "f11", s1);
      Term term12 = new Term(null, "f12", s1);
      Term term21 = new Term(null, "f21", s2);
      Term term22 = new Term(null, "f22", s2);

      // Input is just s1
      RulesCollection rulesCollection = builder.build();
      PositionSequence<InputSequenceElement> sequence = new PositionSequence<>();
      sequence.nextPosition();
      sequence.addElement(term11);
      sequence.addElement(term21);

      List<Action> actions = AbstractCommonRulesTest.getActions(rulesCollection, sequence);
      assertTrue(actions.isEmpty());

      sequence.nextPosition();
      sequence.addElement(term12);
      actions = AbstractCommonRulesTest.getActions(rulesCollection, sequence);
      assertTrue(actions.isEmpty());

      sequence.addElement(term22);
      actions = AbstractCommonRulesTest.getActions(rulesCollection, sequence);

      assertThat(actions, contains(new Action(instructions1,
            new TermMatches(Arrays.asList(new TermMatch(term11), new TermMatch(term22))), 0, 2)));
      sequence.clear();

      actions = AbstractCommonRulesTest.getActions(rulesCollection, sequence);
      assertTrue(actions.isEmpty());
      sequence.nextPosition();
      sequence.addElement(term12);
      sequence.nextPosition();
      sequence.addElement(term21);
      actions = AbstractCommonRulesTest.getActions(rulesCollection, sequence);

      assertThat(actions, contains(new Action(instructions1,
              new TermMatches(Arrays.asList(new TermMatch(term12), new TermMatch(term21))), 0, 2)));

   }

   List<querqy.rewrite.commonrules.model.Term> inputTerms(List<String> fieldNames, String... values) {
      List<querqy.rewrite.commonrules.model.Term> result = new LinkedList<>();
      for (String value : values) {
         char[] chars = value.toCharArray();
         result.add(new querqy.rewrite.commonrules.model.Term(chars, 0, chars.length, fieldNames));
      }
      return result;
   }

   TermMatches termMatches(String... values) {
      TermMatches result = new TermMatches();
      for (String value : values) {
         result.add(new TermMatch(new Term(null, value)));
      }
      return result;
   }

   Instructions instructions(int ord, String... names) {
      List<Instruction> instructions = new LinkedList<>();
      for (String name : names) {
         instructions.add(new SimpleInstruction(name));
      }
      return new Instructions(ord, Integer.toString(ord), instructions);
   }

   static class SimpleInstruction implements Instruction {

      final String name;

      public SimpleInstruction(String name) {
         this.name = name;
      }

      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + ((name == null) ? 0 : name.hashCode());
         return result;
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj)
            return true;
         if (obj == null)
            return false;
         if (getClass() != obj.getClass())
            return false;
         SimpleInstruction other = (SimpleInstruction) obj;
         if (name == null) {
            return other.name == null;
         } else {
            return name.equals(other.name);
         }
      }

      @Override
      public String toString() {
         return "SimpleInstruction [name=" + name + "]";
      }

      @Override
      public void apply(PositionSequence<Term> sequence,
                        TermMatches termsMatches, int startPosition, int endPosition, ExpandedQuery expandedQuery, SearchEngineRequestAdapter searchEngineRequestAdapter) {
      }

    @Override
    public Set<Term> getGenerableTerms() {
        return QueryRewriter.EMPTY_GENERABLE_TERMS;
    }

   }
}
