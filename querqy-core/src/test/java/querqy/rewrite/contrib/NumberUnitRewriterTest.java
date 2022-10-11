package querqy.rewrite.contrib;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import querqy.ComparableCharSequence;
import querqy.ComparableCharSequenceWrapper;
import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.model.Term;
import querqy.model.convert.builder.BooleanQueryBuilder;
import querqy.rewrite.contrib.numberunit.NumberUnitQueryCreator;
import querqy.rewrite.contrib.numberunit.model.NumberUnitQueryInput;
import querqy.rewrite.contrib.numberunit.model.PerUnitNumberUnitDefinition;
import querqy.trie.TrieMap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static querqy.model.convert.builder.BooleanQueryBuilder.bq;
import static querqy.model.convert.builder.DisjunctionMaxQueryBuilder.dmq;
import static querqy.model.convert.builder.ExpandedQueryBuilder.expanded;
import static querqy.model.convert.builder.TermBuilder.term;

@RunWith(MockitoJUnitRunner.class)
public class NumberUnitRewriterTest {

    @Mock
    NumberUnitQueryCreator numberUnitQueryCreator;

    @Mock
    List<PerUnitNumberUnitDefinition> perUnitNumberUnitDefinitions;

    private TrieMap<List<PerUnitNumberUnitDefinition>> numberUnitMap = new TrieMap<>();

    @Before
    public void setup() {
        numberUnitMap.put(new ComparableCharSequenceWrapper("zoll"), perUnitNumberUnitDefinitions);
        numberUnitMap.put(new ComparableCharSequenceWrapper("\""), perUnitNumberUnitDefinitions);

        doReturn(3).when(numberUnitQueryCreator).getScale();
        doReturn(RoundingMode.HALF_UP).when(numberUnitQueryCreator).getRoundingMode();
    }

    @Test
    public void testThatGeneratedTermsDoNotAffectNumberUnitLookups() {
        doReturn(new PerUnitNumberUnitDefinition[0]).when(perUnitNumberUnitDefinitions).toArray();

        BooleanQueryBuilder query = bq(
                dmq("term"),
                dmq(term("12"), term("generatedTerm", true)),
                dmq(term("zoll"), term("generatedTerm", true))
        );

        NumberUnitRewriter numberUnitRewriter = new NumberUnitRewriter(numberUnitMap, numberUnitQueryCreator);

        ExpandedQuery expandedQuery = numberUnitRewriter.rewrite(expanded(query).build(), null).getExpandedQuery();
        BooleanQueryBuilder rewrittenQuery = bq((Query) expandedQuery.getUserQuery());

        assertThat(rewrittenQuery).isEqualTo(
                bq("term"));
    }

    @Test
    public void testThatGeneratedTermsAreNotConsideredByNumberUnitLookups() {
        BooleanQueryBuilder query = bq(
                dmq("term"),
                dmq(term("term"), term("12", true)),
                dmq(term("term"), term("zoll", true))
        );

        NumberUnitRewriter numberUnitRewriter = new NumberUnitRewriter(numberUnitMap, numberUnitQueryCreator);

        ExpandedQuery expandedQuery = numberUnitRewriter.rewrite(expanded(query).build(), null).getExpandedQuery();
        BooleanQueryBuilder rewrittenQuery = bq((Query) expandedQuery.getUserQuery());

        assertThat(rewrittenQuery).isEqualTo(
                query);
    }

    @Test
    public void testValidNumberUnitInputAsOneToken() {
        NumberUnitRewriter numberUnitRewriter = new NumberUnitRewriter(numberUnitMap, numberUnitQueryCreator);

        Optional<NumberUnitQueryInput> numberUnitInput;

        numberUnitInput = numberUnitRewriter.parseNumberAndUnit(createSeq("12zoll"));
        assertThat(numberUnitInput).isNotEmpty();
        assertThat(numberUnitInput.get()).isEqualTo((new NumberUnitQueryInput(new BigDecimal("12"), perUnitNumberUnitDefinitions)));

        numberUnitInput = numberUnitRewriter.parseNumberAndUnit(createSeq("12\""));
        assertThat(numberUnitInput).isNotEmpty();
        assertThat(numberUnitInput.get()).isEqualTo((new NumberUnitQueryInput(new BigDecimal("12"), perUnitNumberUnitDefinitions)));

        numberUnitInput = numberUnitRewriter.parseNumberAndUnit(createSeq("12.3zoll"));
        assertThat(numberUnitInput).isNotEmpty();
        assertThat(numberUnitInput.get()).isEqualTo((new NumberUnitQueryInput(new BigDecimal("12.3"), perUnitNumberUnitDefinitions)));

        numberUnitInput = numberUnitRewriter.parseNumberAndUnit(createSeq("12,3zoll"));
        assertThat(numberUnitInput).isNotEmpty();
        assertThat(numberUnitInput.get()).isEqualTo((new NumberUnitQueryInput(new BigDecimal("12.3"), perUnitNumberUnitDefinitions)));

        numberUnitInput = numberUnitRewriter.parseNumberAndUnit(createSeq("12.zoll"));
        assertThat(numberUnitInput).isNotEmpty();
        assertThat(numberUnitInput.get()).isEqualTo((new NumberUnitQueryInput(new BigDecimal("12"), perUnitNumberUnitDefinitions)));

        numberUnitInput = numberUnitRewriter.parseNumberAndUnit(createSeq(".12zoll"));
        assertThat(numberUnitInput).isNotEmpty();
        assertThat(numberUnitInput.get()).isEqualTo((new NumberUnitQueryInput(new BigDecimal("0.12"), perUnitNumberUnitDefinitions)));
    }

    @Test
    public void testInvalidNumberInput() {
        NumberUnitRewriter numberUnitRewriter = new NumberUnitRewriter(numberUnitMap, numberUnitQueryCreator);

        Optional<NumberUnitQueryInput> numberUnitInput;

        numberUnitInput = numberUnitRewriter.parseNumberAndUnit(createSeq("1..2"));
        assertThat(numberUnitInput).isEmpty();

        numberUnitInput = numberUnitRewriter.parseNumberAndUnit(createSeq("1.2,9"));
        assertThat(numberUnitInput).isEmpty();

        numberUnitInput = numberUnitRewriter.parseNumberAndUnit(createSeq("1;29"));
        assertThat(numberUnitInput).isEmpty();

        numberUnitInput = numberUnitRewriter.parseNumberAndUnit(createSeq(".129."));
        assertThat(numberUnitInput).isEmpty();

        numberUnitInput = numberUnitRewriter.parseNumberAndUnit(createSeq("e129"));
        assertThat(numberUnitInput).isEmpty();

        numberUnitInput = numberUnitRewriter.parseNumberAndUnit(createSeq("..129"));
        assertThat(numberUnitInput).isEmpty();

    }

    @Test
    public void testDelimiterOnlyInput() {
        NumberUnitRewriter numberUnitRewriter = new NumberUnitRewriter(numberUnitMap, numberUnitQueryCreator);

        Optional<NumberUnitQueryInput> numberUnitInput;

        numberUnitInput = numberUnitRewriter.parseNumberAndUnit(createSeq(".."));
        assertThat(numberUnitInput).isEmpty();

        numberUnitInput = numberUnitRewriter.parseNumberAndUnit(createSeq("."));
        assertThat(numberUnitInput).isEmpty();

        numberUnitInput = numberUnitRewriter.parseNumberAndUnit(createSeq(","));
        assertThat(numberUnitInput).isEmpty();



    }

    @Test
    public void testValidNumberInput() {
        NumberUnitRewriter numberUnitRewriter = new NumberUnitRewriter(numberUnitMap, numberUnitQueryCreator);

        Optional<NumberUnitQueryInput> numberUnitInput;

        numberUnitInput = numberUnitRewriter.parseNumberAndUnit(createSeq("12"));
        assertThat(numberUnitInput).isNotEmpty();
        assertThat(numberUnitInput.get()).isEqualTo((new NumberUnitQueryInput(new BigDecimal("12"))));

        numberUnitInput = numberUnitRewriter.parseNumberAndUnit(createSeq("12."));
        assertThat(numberUnitInput).isNotEmpty();
        assertThat(numberUnitInput.get()).isEqualTo((new NumberUnitQueryInput(new BigDecimal("12"))));

        numberUnitInput = numberUnitRewriter.parseNumberAndUnit(createSeq("12,"));
        assertThat(numberUnitInput).isNotEmpty();
        assertThat(numberUnitInput.get()).isEqualTo((new NumberUnitQueryInput(new BigDecimal("12"))));

        numberUnitInput = numberUnitRewriter.parseNumberAndUnit(createSeq("12.3"));
        assertThat(numberUnitInput).isNotEmpty();
        assertThat(numberUnitInput.get()).isEqualTo((new NumberUnitQueryInput(new BigDecimal("12.3"))));

        numberUnitInput = numberUnitRewriter.parseNumberAndUnit(createSeq("12,3"));
        assertThat(numberUnitInput).isNotEmpty();
        assertThat(numberUnitInput.get()).isEqualTo((new NumberUnitQueryInput(new BigDecimal("12.3"))));

        numberUnitInput = numberUnitRewriter.parseNumberAndUnit(createSeq(".3"));
        assertThat(numberUnitInput).isNotEmpty();
        assertThat(numberUnitInput.get()).isEqualTo((new NumberUnitQueryInput(new BigDecimal("0.3"))));

        numberUnitInput = numberUnitRewriter.parseNumberAndUnit(createSeq(",3"));
        assertThat(numberUnitInput).isNotEmpty();
        assertThat(numberUnitInput.get()).isEqualTo((new NumberUnitQueryInput(new BigDecimal("0.3"))));

        numberUnitInput = numberUnitRewriter.parseNumberAndUnit(createSeq("12.36786"));
        assertThat(numberUnitInput).isNotEmpty();
        assertThat(numberUnitInput.get()).isEqualTo((new NumberUnitQueryInput(new BigDecimal("12.368"))));

        numberUnitInput = numberUnitRewriter.parseNumberAndUnit(createSeq("12,36786"));
        assertThat(numberUnitInput).isNotEmpty();
        assertThat(numberUnitInput.get()).isEqualTo((new NumberUnitQueryInput(new BigDecimal("12.368"))));

        numberUnitInput = numberUnitRewriter.parseNumberAndUnit(createSeq("12.36722"));
        assertThat(numberUnitInput).isNotEmpty();
        assertThat(numberUnitInput.get()).isEqualTo((new NumberUnitQueryInput(new BigDecimal("12.367"))));

        numberUnitInput = numberUnitRewriter.parseNumberAndUnit(createSeq("12.367227585647839464786564378"));
        assertThat(numberUnitInput).isNotEmpty();
        assertThat(numberUnitInput.get()).isEqualTo((new NumberUnitQueryInput(new BigDecimal("12.367"))));

    }

    private ComparableCharSequence createSeq(String input) {
        return new ComparableCharSequenceWrapper(input);
    }

    private ExpandedQuery createQuery(String... tokens) {
        Query query = new Query();
        Arrays.stream(tokens).forEach(token -> addTerm(query, token));
        return new ExpandedQuery(query);
    }

    private List<CharSequence> getCharSeqs(List<String> strings) {
        return strings.stream().map(ComparableCharSequenceWrapper::new).collect(Collectors.toList());
    }

    private void addTerm(Query query, String value) {
        DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, Clause.Occur.SHOULD, true);
        query.addClause(dmq);
        Term term = new Term(dmq, null, value);
        dmq.addClause(term);
    }

    private void addTerm(Query query, String value, boolean isGenerated) {
        DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, Clause.Occur.SHOULD, true);
        query.addClause(dmq);
        Term term = new Term(dmq, null, value, isGenerated);
        dmq.addClause(term);
    }

}
