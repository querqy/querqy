/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2026 Querqy Contributors
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
package querqy.rewrite.contrib;

import org.hamcrest.Matchers;
import org.junit.Test;
import querqy.model.BooleanClause;
import querqy.model.BoostQuery;
import querqy.model.BoostedPhraseQuery;
import querqy.model.Clause.Occur;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.QuerqyQuery;
import querqy.model.Query;
import querqy.model.Term;
import querqy.rewrite.RewriterOutput;
import querqy.rewrite.contrib.PhraseBoostRewriter.FieldAndBoost;
import querqy.rewrite.contrib.PhraseBoostRewriter.PhraseTypeConfig;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class PhraseBoostRewriterTest {

    // ---- helpers ----

    private static Query queryOf(String... terms) {
        Query q = new Query();
        for (String t : terms) {
            addTerm(q, t, false);
        }
        return q;
    }

    private static void addTerm(Query q, String value, boolean generated) {
        DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(q, Occur.SHOULD, false);
        q.addClause(dmq);
        dmq.addClause(new Term(dmq, null, value, generated));
    }

    private static void addMustNotTerm(Query q, String value) {
        DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(q, Occur.MUST_NOT, false);
        q.addClause(dmq);
        dmq.addClause(new Term(dmq, null, value, false));
    }

    private static void addTermWithGeneratedSynonym(Query q, String value, String generatedSynonym) {
        DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(q, Occur.SHOULD, false);
        q.addClause(dmq);
        dmq.addClause(new Term(dmq, null, value, false));
        dmq.addClause(new Term(dmq, null, generatedSynonym, true));
    }

    private static PhraseTypeConfig config(int slop, String... fieldSpecs) {
        List<FieldAndBoost> fields = Arrays.stream(fieldSpecs)
                .map(FieldAndBoost::parse)
                .collect(Collectors.toList());
        return new PhraseTypeConfig(fields, slop);
    }

    private static DisjunctionMaxQuery singleBoostDmq(ExpandedQuery eq) {
        Collection<BoostQuery> up = eq.getBoostUpQueries();
        assertEquals(1, up.size());
        BoostQuery bq = up.iterator().next();
        assertEquals(1.0f, bq.getBoost(), 0f);
        QuerqyQuery<?> q = bq.getQuery();
        assertThat(q, Matchers.instanceOf(Query.class));
        List<BooleanClause> clauses = ((Query) q).getClauses();
        assertEquals(1, clauses.size());
        assertThat(clauses.get(0), Matchers.instanceOf(DisjunctionMaxQuery.class));
        return (DisjunctionMaxQuery) clauses.get(0);
    }

    private static List<BoostedPhraseQuery> bpqs(DisjunctionMaxQuery dmq) {
        return dmq.getClauses().stream()
                .peek(c -> assertThat(c, Matchers.instanceOf(BoostedPhraseQuery.class)))
                .map(c -> (BoostedPhraseQuery) c)
                .collect(Collectors.toList());
    }

    private static void assertPhrase(BoostedPhraseQuery pq, String field, List<String> terms,
                                     int slop, float boost) {
        assertEquals(field, pq.getField());
        assertEquals(terms, pq.getTerms());
        assertEquals(slop, pq.getSlop());
        assertEquals(boost, pq.getBoost(), 0f);
    }

    // ---- FieldAndBoost.parse ----

    @Test
    public void testFieldSpecParsingNoBoost() {
        FieldAndBoost fab = FieldAndBoost.parse("title");
        assertEquals("title", fab.field);
        assertEquals(1.0f, fab.boost, 0f);
    }

    @Test
    public void testFieldSpecParsingIntegerBoost() {
        FieldAndBoost fab = FieldAndBoost.parse("brand^4");
        assertEquals("brand", fab.field);
        assertEquals(4.0f, fab.boost, 0f);
    }

    @Test
    public void testFieldSpecParsingDecimalBoost() {
        FieldAndBoost fab = FieldAndBoost.parse("title^1.5");
        assertEquals("title", fab.field);
        assertEquals(1.5f, fab.boost, 0.001f);
    }

    // ---- no boost produced ----

    @Test
    public void testNoBoostForSingleTerm() {
        ExpandedQuery eq = new ExpandedQuery(queryOf("apple"));
        RewriterOutput out = new PhraseBoostRewriter(config(0, "title"), null, null, 0.1f)
                .rewrite(eq, null);
        assertSame(eq, out.getExpandedQuery());
        assertNull(out.getExpandedQuery().getBoostUpQueries());
    }

    @Test
    public void testNoBoostWhenAllConfigsNull() {
        ExpandedQuery eq = new ExpandedQuery(queryOf("A", "B", "C"));
        RewriterOutput out = new PhraseBoostRewriter(null, null, null, 0.0f).rewrite(eq, null);
        assertSame(eq, out.getExpandedQuery());
        assertNull(out.getExpandedQuery().getBoostUpQueries());
    }

    @Test
    public void testGeneratedTermInSingleClauseDmqNotCollected() {
        // Position 2 has only a generated term — sequence has 1 element → no boost
        Query q = new Query();
        addTerm(q, "apple", false);
        addTerm(q, "generated_synonym", true);
        ExpandedQuery eq = new ExpandedQuery(q);
        RewriterOutput out = new PhraseBoostRewriter(config(0, "title"), null, null, 0.0f)
                .rewrite(eq, null);
        assertSame(eq, out.getExpandedQuery());
        assertNull(out.getExpandedQuery().getBoostUpQueries());
    }

    @Test
    public void testMustNotTermNotCollected() {
        // "apple" SHOULD, "bad" MUST_NOT — sequence = ["apple"] → no boost
        Query q = new Query();
        addTerm(q, "apple", false);
        addMustNotTerm(q, "bad");
        ExpandedQuery eq = new ExpandedQuery(q);
        RewriterOutput out = new PhraseBoostRewriter(config(0, "title"), null, null, 0.0f)
                .rewrite(eq, null);
        assertSame(eq, out.getExpandedQuery());
        assertNull(out.getExpandedQuery().getBoostUpQueries());
    }

    // ---- bigrams ----

    @Test
    public void testBigramForTwoTermQuery() {
        ExpandedQuery eq = new ExpandedQuery(queryOf("A", "B"));
        RewriterOutput out = new PhraseBoostRewriter(config(1, "title"), null, null, 0.1f)
                .rewrite(eq, null);
        assertSame(eq, out.getExpandedQuery());

        DisjunctionMaxQuery dmq = singleBoostDmq(out.getExpandedQuery());
        assertEquals(Optional.of(0.1f), dmq.getTieBreaker());

        List<BoostedPhraseQuery> phrases = bpqs(dmq);
        assertEquals(1, phrases.size());
        assertPhrase(phrases.get(0), "title", Arrays.asList("A", "B"), 1, 1.0f);
    }

    @Test
    public void testBigramsForFourTermQuery() {
        ExpandedQuery eq = new ExpandedQuery(queryOf("A", "B", "C", "D"));
        RewriterOutput out = new PhraseBoostRewriter(config(0, "title"), null, null, 0.0f)
                .rewrite(eq, null);
        assertSame(eq, out.getExpandedQuery());

        List<BoostedPhraseQuery> phrases = bpqs(singleBoostDmq(out.getExpandedQuery()));
        assertEquals(3, phrases.size());
        assertPhrase(phrases.get(0), "title", Arrays.asList("A", "B"), 0, 1.0f);
        assertPhrase(phrases.get(1), "title", Arrays.asList("B", "C"), 0, 1.0f);
        assertPhrase(phrases.get(2), "title", Arrays.asList("C", "D"), 0, 1.0f);
    }

    @Test
    public void testBigramFieldBoostApplied() {
        ExpandedQuery eq = new ExpandedQuery(queryOf("A", "B"));
        RewriterOutput out = new PhraseBoostRewriter(config(0, "brand^4"), null, null, 0.0f)
                .rewrite(eq, null);
        assertSame(eq, out.getExpandedQuery());

        List<BoostedPhraseQuery> phrases = bpqs(singleBoostDmq(out.getExpandedQuery()));
        assertEquals(1, phrases.size());
        assertPhrase(phrases.get(0), "brand", Arrays.asList("A", "B"), 0, 4.0f);
    }

    @Test
    public void testBigramsAcrossMultipleFields() {
        ExpandedQuery eq = new ExpandedQuery(queryOf("A", "B"));
        RewriterOutput out = new PhraseBoostRewriter(config(0, "title", "brand^4"), null, null, 0.0f)
                .rewrite(eq, null);
        assertSame(eq, out.getExpandedQuery());

        List<BoostedPhraseQuery> phrases = bpqs(singleBoostDmq(out.getExpandedQuery()));
        assertEquals(2, phrases.size());
        assertPhrase(phrases.get(0), "title", Arrays.asList("A", "B"), 0, 1.0f);
        assertPhrase(phrases.get(1), "brand", Arrays.asList("A", "B"), 0, 4.0f);
    }

    // ---- trigrams ----

    @Test
    public void testNoTrigramsForTwoTermQuery() {
        // sequence.size() < 3 → trigram config is skipped
        ExpandedQuery eq = new ExpandedQuery(queryOf("A", "B"));
        RewriterOutput out = new PhraseBoostRewriter(null, config(0, "title"), null, 0.0f)
                .rewrite(eq, null);
        assertSame(eq, out.getExpandedQuery());
        assertNull(out.getExpandedQuery().getBoostUpQueries());
    }

    @Test
    public void testTrigramForThreeTermQuery() {
        ExpandedQuery eq = new ExpandedQuery(queryOf("A", "B", "C"));
        RewriterOutput out = new PhraseBoostRewriter(null, config(0, "title^1.25"), null, 0.0f)
                .rewrite(eq, null);
        assertSame(eq, out.getExpandedQuery());

        List<BoostedPhraseQuery> phrases = bpqs(singleBoostDmq(out.getExpandedQuery()));
        assertEquals(1, phrases.size());
        assertPhrase(phrases.get(0), "title", Arrays.asList("A", "B", "C"), 0, 1.25f);
    }

    @Test
    public void testTrigramsForFourTermQuery() {
        ExpandedQuery eq = new ExpandedQuery(queryOf("A", "B", "C", "D"));
        RewriterOutput out = new PhraseBoostRewriter(null, config(0, "title"), null, 0.0f)
                .rewrite(eq, null);
        assertSame(eq, out.getExpandedQuery());

        List<BoostedPhraseQuery> phrases = bpqs(singleBoostDmq(out.getExpandedQuery()));
        assertEquals(2, phrases.size());
        assertPhrase(phrases.get(0), "title", Arrays.asList("A", "B", "C"), 0, 1.0f);
        assertPhrase(phrases.get(1), "title", Arrays.asList("B", "C", "D"), 0, 1.0f);
    }

    // ---- full phrase ----

    @Test
    public void testFullPhraseForTwoTermQuery() {
        ExpandedQuery eq = new ExpandedQuery(queryOf("A", "B"));
        RewriterOutput out = new PhraseBoostRewriter(null, null, config(2, "title"), 0.0f)
                .rewrite(eq, null);
        assertSame(eq, out.getExpandedQuery());

        List<BoostedPhraseQuery> phrases = bpqs(singleBoostDmq(out.getExpandedQuery()));
        assertEquals(1, phrases.size());
        assertPhrase(phrases.get(0), "title", Arrays.asList("A", "B"), 2, 1.0f);
    }

    // ---- combined ----

    @Test
    public void testAllPhraseTypesForFourTermQuery() {
        ExpandedQuery eq = new ExpandedQuery(queryOf("A", "B", "C", "D"));
        RewriterOutput out = new PhraseBoostRewriter(
                config(0, "title^1.5"),
                config(0, "title^1.25"),
                config(0, "title"),
                0.1f).rewrite(eq, null);
        assertSame(eq, out.getExpandedQuery());

        DisjunctionMaxQuery dmq = singleBoostDmq(out.getExpandedQuery());
        assertEquals(Optional.of(0.1f), dmq.getTieBreaker());

        List<BoostedPhraseQuery> phrases = bpqs(dmq);
        assertEquals(6, phrases.size()); // 3 bigrams + 2 trigrams + 1 full

        // bigrams
        assertPhrase(phrases.get(0), "title", Arrays.asList("A", "B"), 0, 1.5f);
        assertPhrase(phrases.get(1), "title", Arrays.asList("B", "C"), 0, 1.5f);
        assertPhrase(phrases.get(2), "title", Arrays.asList("C", "D"), 0, 1.5f);
        // trigrams
        assertPhrase(phrases.get(3), "title", Arrays.asList("A", "B", "C"), 0, 1.25f);
        assertPhrase(phrases.get(4), "title", Arrays.asList("B", "C", "D"), 0, 1.25f);
        // full phrase
        assertPhrase(phrases.get(5), "title", Arrays.asList("A", "B", "C", "D"), 0, 1.0f);
    }

    // ---- term collection edge cases ----

    @Test
    public void testNonGeneratedTermPickedFromMultiClauseDmq() {
        // Each query position has the original term + a generated synonym
        Query q = new Query();
        addTermWithGeneratedSynonym(q, "A", "A_gen");
        addTermWithGeneratedSynonym(q, "B", "B_gen");
        ExpandedQuery eq = new ExpandedQuery(q);
        RewriterOutput out = new PhraseBoostRewriter(config(0, "title"), null, null, 0.0f)
                .rewrite(eq, null);
        assertSame(eq, out.getExpandedQuery());

        List<BoostedPhraseQuery> phrases = bpqs(singleBoostDmq(out.getExpandedQuery()));
        assertEquals(1, phrases.size());
        assertEquals(Arrays.asList("A", "B"), phrases.get(0).getTerms());
    }

    @Test
    public void testMustNotTermsDoNotContributeToPhrases() {
        // "A" SHOULD, "B" MUST_NOT, "C" SHOULD → sequence = ["A", "C"]
        Query q = new Query();
        addTerm(q, "A", false);
        addMustNotTerm(q, "B");
        addTerm(q, "C", false);
        ExpandedQuery eq = new ExpandedQuery(q);
        RewriterOutput out = new PhraseBoostRewriter(config(0, "title"), null, null, 0.0f)
                .rewrite(eq, null);
        assertSame(eq, out.getExpandedQuery());

        // sequence = ["A", "C"] → one bigram "A C"
        List<BoostedPhraseQuery> phrases = bpqs(singleBoostDmq(out.getExpandedQuery()));
        assertEquals(1, phrases.size());
        assertEquals(Arrays.asList("A", "C"), phrases.get(0).getTerms());
    }

    // ---- PhraseBoostRewriterFactory ----

    @Test
    public void testFactoryCreateBuildsRewriter() {
        PhraseBoostRewriterFactory factory = PhraseBoostRewriterFactory.create(
                "id",
                Arrays.asList("title", "brand^2"), 0,
                Arrays.asList("title"), 1,
                null, 0,
                0.1f);

        ExpandedQuery eq = new ExpandedQuery(queryOf("A", "B", "C"));
        RewriterOutput out = factory.createRewriter(eq, null).rewrite(eq, null);
        assertSame(eq, out.getExpandedQuery());

        DisjunctionMaxQuery dmq = singleBoostDmq(out.getExpandedQuery());
        // 2 bigrams × 2 fields + 1 trigram × 1 field = 5 phrases
        assertEquals(5, dmq.getClauses().size());
    }

    @Test
    public void testFactoryReturnsSameRewriterInstance() {
        PhraseBoostRewriterFactory factory = PhraseBoostRewriterFactory.create(
                "id", Arrays.asList("title"), 0, null, 0, null, 0, 0.0f);
        ExpandedQuery eq = new ExpandedQuery(queryOf("A", "B"));
        assertSame(factory.createRewriter(eq, null), factory.createRewriter(eq, null));
    }
}
