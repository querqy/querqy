/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2014 Querqy Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package querqy.rewriter.commonrules;

import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.mock;
import static querqy.model.convert.builder.BooleanQueryBuilder.bq;
import static querqy.rewriter.commonrules.select.SelectionStrategyFactory.DEFAULT_SELECTION_STRATEGY;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import querqy.model.EmptySearchEngineRequestAdapter;
import querqy.model.ExpandedQuery;
import querqy.model.Input;
import querqy.model.InputSequenceElement;
import querqy.model.QuerqyQuery;
import querqy.model.Query;
import querqy.model.convert.builder.BooleanQueryBuilder;
import querqy.parser.WhiteSpaceQuerqyParser;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewriter.commonrules.model.Action;
import querqy.rewriter.commonrules.model.BoostInstruction;
import querqy.rewriter.commonrules.model.DecorateInstruction;
import querqy.rewriter.commonrules.model.DeleteInstruction;
import querqy.rewriter.commonrules.model.FilterInstruction;
import querqy.rewriter.commonrules.model.Instruction;
import querqy.rewriter.commonrules.model.InstructionDescription;
import querqy.rewriter.commonrules.model.Instructions;
import querqy.rewriter.commonrules.model.InstructionsProperties;
import querqy.rewriter.commonrules.model.PositionSequence;
import querqy.rewriter.commonrules.model.RulesCollection;
import querqy.rewriter.commonrules.model.RulesCollectionBuilder;
import querqy.rewriter.commonrules.model.SynonymInstruction;
import querqy.rewriter.commonrules.model.Term;
import querqy.rewriter.commonrules.model.TrieMapRulesCollectionBuilder;
import querqy.rewriter.commonrules.select.TopRewritingActionCollector;
import querqy.rewriter.commonrules.select.booleaninput.model.BooleanInput;
import querqy.rewriter.commonrules.select.booleaninput.model.BooleanInputLiteral;
import querqy.rewrite.lookup.LookupConfig;
import querqy.rewrite.lookup.triemap.TrieMapLookupQueryVisitorFactory;
import querqy.rewrite.rules.input.InputParserAdapter;

public abstract class AbstractCommonRulesTest {

    private int ruleCounter = 0;

    protected ExpandedQuery makeQuery(String input) {
        return new ExpandedQuery(new WhiteSpaceQuerqyParser().parse(input));
    }

    protected QuerqyQuery<?> makeQuerqyQuery(String input) {
        return new WhiteSpaceQuerqyParser().parse(input);
    }

    protected Term mkTerm(String s) {
        return new Term(s.toCharArray(), 0, s.length(), null);
    }

    protected Term mkTerm(String s, String... fieldName) {
        return new Term(s.toCharArray(), 0, s.length(), Arrays.asList(fieldName));
    }

    public static List<Action> getActions(final RulesCollection rules,
                                          final PositionSequence<InputSequenceElement> seq) {
        final TopRewritingActionCollector collector = DEFAULT_SELECTION_STRATEGY.createTopRewritingActionCollector();
        rules.collectRewriteActions(seq, collector);
        return collector.createActions();
    }

    public void addRule(RulesCollectionBuilder builder, Input.SimpleInput input, Instruction... instructions) {
        int ruleCount = ruleCounter++;
        builder.addRule(input, new Instructions(ruleCount, ruleCount, Arrays.asList(instructions)));
    }

    public CommonRulesRewriter rewriter(List<BooleanInputLiteral> literals) {
        return rewriter(literals.stream()
                .map(literal -> rule(input(literal.getTerms()), literal))
                .toArray(Rule[]::new));
    }

    public CommonRulesRewriter rewriter(Rule... rules) {
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        Arrays.stream(rules).forEach(rule -> {
            if (rule.instructions != null) {
                builder.addRule(rule.input, rule.instructions);
            } else {
                builder.addRule(rule.input, rule.literal);
            }
        });
        return new CommonRulesRewriter(
                TrieMapLookupQueryVisitorFactory.of(
                        builder.getTrieMap(), LookupConfig.defaultConfig(), builder.getSuffixWildcardRules()),
                DEFAULT_SELECTION_STRATEGY);
    }

    public BooleanQueryBuilder rewrite(BooleanQueryBuilder queryBuilder, CommonRulesRewriter rewriter) {
        return rewrite(queryBuilder, rewriter, new EmptySearchEngineRequestAdapter());
    }

    public BooleanQueryBuilder rewrite(BooleanQueryBuilder queryBuilder, CommonRulesRewriter rewriter,
                         SearchEngineRequestAdapter searchEngineRequestAdapter) {
        ExpandedQuery query = new ExpandedQuery(queryBuilder.buildQuerqyQuery());
        return new BooleanQueryBuilder((Query) rewriter.rewrite(query, searchEngineRequestAdapter).getExpandedQuery().getUserQuery());
    }

    public Rule rule(Input.SimpleInput input, Instruction... instructions) {
        int ruleCount = ruleCounter++;
        return new Rule(input, new Instructions(ruleCount, ruleCount, Arrays.asList(instructions)));
    }

    public Rule rule(Input.SimpleInput input, Instruction instruction, Property property) {
        int ruleCount = ruleCounter++;
        return new Rule(
                input,
                new Instructions(
                        ruleCount, ruleCount, List.of(instruction),
                        new InstructionsProperties(Map.of(property.getKey(), property.getValue()))));
    }

    public Rule rule(Input.SimpleInput input, BooleanInputLiteral literal) {
        return new Rule(input, literal);
    }

    public Input.SimpleInput input(final String input) {
        return (Input.SimpleInput) InputParserAdapter.builder().isAllowedToParseBooleanInput(true).build()
                .with(input)
                .parse()
                .getInput();
    }

    public Input.SimpleInput input(String... terms) {
        return new Input.SimpleInput(Arrays.stream(terms).map(this::mkTerm).collect(toList()), String.join(" ", terms));
    }

    public Input.SimpleInput input(List<String> terms) {
        return new Input.SimpleInput(
                terms.stream().map(this::mkTerm).collect(toList()), false, false, String.join(" ", terms));
    }

    public DeleteInstruction delete(String... terms) {
        return new DeleteInstruction(Arrays.stream(terms).map(this::mkTerm).collect(toList()));
    }

    public DeleteInstruction delete(List<Term> terms) {
        return new DeleteInstruction(terms);
    }

    public DecorateInstruction decorate(String key, String value) {
        return new DecorateInstruction(key, value);
    }

    public DecorateInstruction decorate(String value) {
        return new DecorateInstruction(value);
    }

    public SynonymInstruction synonym(String... terms) {
        return new SynonymInstruction(Arrays.stream(terms).map(this::mkTerm).collect(toList()));
    }

    public SynonymInstruction synonym(final String term, final InstructionDescription instructionDescription) {
        return new SynonymInstruction(List.of(mkTerm(term)), 1.0f, instructionDescription);
    }

    public FilterInstruction filter(String... terms) {
        return new FilterInstruction(bq(terms).build());
    }

    public FilterInstruction filter(final String terms) {
        return new FilterInstruction(makeQuerqyQuery(terms));
    }

    public BoostInstruction boostUp(final String terms, final float boost) {
        return boostUp(terms, boost, BoostInstruction.BoostMethod.ADDITIVE);
    }

    public BoostInstruction boostUp(final String terms, final float boost, final BoostInstruction.BoostMethod boostMethod) {
        return new BoostInstruction(
                makeQuerqyQuery(terms), BoostInstruction.BoostDirection.UP, boostMethod, boost);
    }

    public BoostInstruction boostDown(final String terms, final float boost) {
        return new BoostInstruction(
                makeQuerqyQuery(terms), BoostInstruction.BoostDirection.DOWN, BoostInstruction.BoostMethod.ADDITIVE, boost);
    }

    public EmptySearchEngineRequestAdapter emptyAdapter() {
        return new EmptySearchEngineRequestAdapter();
    }

    public Property property(final String key, final String value) {
        return new Property(key, value);
    }

    public static List<String> list(String... items) {
        return Arrays.asList(items);
    }

    public static List<Instruction> list(Instruction... instructions) {
        return Arrays.asList(instructions);
    }

    public Predicate<boolean[]> createConjunctionPredicate(int size) {
        Predicate<boolean[]> predicate = booleans -> booleans[0];

        for (int i = 1; i < size; i++) {
            final int j = i;
            predicate = predicate.and(booleans -> booleans[j]);
        }

        return predicate;
    }

    public void booleanInput(List<BooleanInputLiteral> literals) {
        booleanInput(literals, mock(Instructions.class));
    }

    public void booleanInput(List<BooleanInputLiteral> literals, Instructions instructions) {
        BooleanInput.BooleanInputBuilder builder = BooleanInput.builder("input");
        literals.forEach(builder::addLiteralAndCreateReferenceId);
        builder.withPredicate(createConjunctionPredicate(literals.size()))
                .withInstructions(instructions)
                .build();
    }

    public void booleanInput(List<BooleanInputLiteral> literals, Instruction... instructions) {
        int ruleCount = ruleCounter++;
        booleanInput(literals, new Instructions(ruleCount, ruleCount, Arrays.asList(instructions)));
    }

    public List<BooleanInputLiteral> literals(final int size) {
        return IntStream.range(0, size).mapToObj(number -> literal(Integer.toString(number))).collect(toList());
    }

    public BooleanInputLiteral literal(final String term) {
        return new BooleanInputLiteral(Collections.singletonList(term));
    }

    public List<BooleanInputLiteral> literals(final String... terms) {
        return Arrays.stream(terms).map(this::literal).collect(toList());
    }

    public static class Rule {
        public final Input.SimpleInput input;
        public final Instructions instructions;
        public final BooleanInputLiteral literal;

        public Rule(Input.SimpleInput input, Instructions instructions) {
            this(input, instructions, null);
        }

        public Rule(Input.SimpleInput input, BooleanInputLiteral literal) {
            this(input, null, literal);
        }

        private Rule(Input.SimpleInput input, Instructions instructions, BooleanInputLiteral literal) {
            this.input = input;
            this.instructions = instructions;
            this.literal = literal;
        }
    }

    public static class Property {
        public final String key;
        public final String value;

        public Property(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }

}
