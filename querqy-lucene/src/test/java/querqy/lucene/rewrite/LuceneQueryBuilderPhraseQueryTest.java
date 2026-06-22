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
package querqy.lucene.rewrite;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.search.Query;
import org.junit.Before;
import org.junit.Test;
import querqy.model.BoostedPhraseQuery;
import querqy.model.Clause.Occur;
import querqy.model.PhraseQuery;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static querqy.lucene.rewrite.SearchFieldsAndBoosting.FieldBoostModel.FIXED;

public class LuceneQueryBuilderPhraseQueryTest extends AbstractLuceneQueryTest {

    private static final List<String> TERMS = Arrays.asList("laptop", "bag");

    private Analyzer analyzer;

    @Before
    public void setUp() {
        analyzer = new WhitespaceAnalyzer();
    }

    private LuceneQueryBuilder builder(final Map<String, Float> fields) {
        final SearchFieldsAndBoosting sfb = new SearchFieldsAndBoosting(FIXED, fields, fields, 1f);
        return new LuceneQueryBuilder(
                new DependentTermQueryBuilder(new DocumentFrequencyCorrection()),
                analyzer, sfb, 0.1f, 1f, null, null);
    }

    // --- plain PhraseQuery, no field ---

    @Test
    public void testPlainPhraseQuery_noField_oneField_boostOne_returnsUnwrappedPhraseQuery() {
        final PhraseQuery pq = new PhraseQuery(null, Occur.SHOULD, true, TERMS, 0);
        final Query result = builder(Map.of("f1", 1.0f)).createLucenePhraseQuery(pq);
        assertThat(result, pq("f1", "laptop", "bag"));
    }

    @Test
    public void testPlainPhraseQuery_noField_oneField_boostTwo_returnsBoostWrapped() {
        final PhraseQuery pq = new PhraseQuery(null, Occur.SHOULD, true, TERMS, 0);
        final Query result = builder(Map.of("f1", 2.0f)).createLucenePhraseQuery(pq);
        assertThat(result, pq(2f, "f1", "laptop", "bag"));
    }

    @Test
    public void testPlainPhraseQuery_noField_twoFields_returnsDmq() {
        final PhraseQuery pq = new PhraseQuery(null, Occur.SHOULD, true, TERMS, 0);
        final Query result = builder(Map.of("f1", 1.0f, "f2", 2.0f)).createLucenePhraseQuery(pq);
        assertThat(result, dmq(1f, 0.1f, pq("f1", "laptop", "bag"), pq(2f, "f2", "laptop", "bag")));
    }

    // --- plain PhraseQuery, with field ---

    @Test
    public void testPlainPhraseQuery_withField_returnsUnwrappedPhraseQueryOnThatField() {
        // Field set on PhraseQuery: no boost applied regardless of searchFieldsAndBoosting
        final PhraseQuery pq = new PhraseQuery(null, Occur.SHOULD, true, "f1", TERMS, 0);
        final Query result = builder(Map.of("f1", 1.0f, "f2", 2.0f)).createLucenePhraseQuery(pq);
        assertThat(result, pq("f1", "laptop", "bag"));
    }

    @Test
    public void testPlainPhraseQuery_withField_fieldNotInConfig_stillQueryOnThatField() {
        // PhraseQuery.field is used directly even if not in searchFieldsAndBoosting
        final PhraseQuery pq = new PhraseQuery(null, Occur.SHOULD, true, "title", TERMS, 0);
        final Query result = builder(Map.of("f1", 1.0f)).createLucenePhraseQuery(pq);
        assertThat(result, pq("title", "laptop", "bag"));
    }

    // --- BoostedPhraseQuery, no field ---

    @Test
    public void testBoostedPhraseQuery_noField_phraseBoostOneFieldBoostOne_returnsUnwrapped() {
        // phraseBoost 1.0 × fieldBoost 1.0 = 1.0 → no wrapper
        final BoostedPhraseQuery bpq = new BoostedPhraseQuery(null, Occur.SHOULD, null, TERMS, 0, 1f);
        final Query result = builder(Map.of("f1", 1.0f)).createLucenePhraseQuery(bpq);
        assertThat(result, pq("f1", "laptop", "bag"));
    }

    @Test
    public void testBoostedPhraseQuery_noField_phraseBoostMultipliedByFieldBoost() {
        // phraseBoost 1.5 × fieldBoost 2.0 = 3.0
        final BoostedPhraseQuery bpq = new BoostedPhraseQuery(null, Occur.SHOULD, null, TERMS, 0, 1.5f);
        final Query result = builder(Map.of("f1", 2.0f)).createLucenePhraseQuery(bpq);
        assertThat(result, pq(3.0f, "f1", "laptop", "bag"));
    }

    @Test
    public void testBoostedPhraseQuery_noField_twoFields_dmqWithCombinedBoosts() {
        // f1: 1.0 × 1.5 = 1.5; f2: 2.0 × 1.5 = 3.0
        final BoostedPhraseQuery bpq = new BoostedPhraseQuery(null, Occur.SHOULD, null, TERMS, 0, 1.5f);
        final Query result = builder(Map.of("f1", 1.0f, "f2", 2.0f)).createLucenePhraseQuery(bpq);
        assertThat(result, dmq(1f, 0.1f, pq(1.5f, "f1", "laptop", "bag"), pq(3.0f, "f2", "laptop", "bag")));
    }

    // --- BoostedPhraseQuery, with field ---

    @Test
    public void testBoostedPhraseQuery_withField_onlyPhraseBoostApplied_fieldConfigIgnored() {
        // f2 has boost 2.0 in config, but since PhraseQuery.field is set the config boost is ignored
        final BoostedPhraseQuery bpq = new BoostedPhraseQuery(null, Occur.SHOULD, "f2", TERMS, 0, 1.5f);
        final Query result = builder(Map.of("f1", 1.0f, "f2", 2.0f)).createLucenePhraseQuery(bpq);
        assertThat(result, pq(1.5f, "f2", "laptop", "bag"));
    }

    @Test
    public void testBoostedPhraseQuery_withField_phraseBoostOne_returnsUnwrapped() {
        // phrase boost 1.0 → no BoostQuery wrapper, regardless of field config boost
        final BoostedPhraseQuery bpq = new BoostedPhraseQuery(null, Occur.SHOULD, "f1", TERMS, 0, 1f);
        final Query result = builder(Map.of("f1", 2.0f)).createLucenePhraseQuery(bpq);
        assertThat(result, pq("f1", "laptop", "bag"));
    }

    // --- slop ---

    @Test
    public void testPhraseQuerySlop_slopIsPreserved() {
        final PhraseQuery pq = new PhraseQuery(null, Occur.SHOULD, true, TERMS, 3);
        final Query result = builder(Map.of("f1", 1.0f)).createLucenePhraseQuery(pq);
        assertThat(result, pq("f1", 3, "laptop", "bag"));
    }

    @Test
    public void testBoostedPhraseQuerySlop_slopIsPreserved() {
        final BoostedPhraseQuery bpq = new BoostedPhraseQuery(null, Occur.SHOULD, "f1", TERMS, 2, 1.5f);
        final Query result = builder(Map.of("f1", 1.0f)).createLucenePhraseQuery(bpq);
        assertThat(result, pq(1.5f, "f1", 2, "laptop", "bag"));
    }
}
