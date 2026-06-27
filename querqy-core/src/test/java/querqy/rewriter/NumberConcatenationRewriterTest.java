/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2024 Querqy Contributors
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
package querqy.rewriter;

import org.junit.Test;

import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.model.Term;

import static org.hamcrest.MatcherAssert.assertThat;
import static querqy.QuerqyMatchers.bq;
import static querqy.QuerqyMatchers.dmq;
import static querqy.QuerqyMatchers.term;

public class NumberConcatenationRewriterTest {

    @Test
    public void testNumberConcatenationForTwoTokens() {
        Query query = new Query();
        addTerm(query, "123");
        addTerm(query, "456");
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        NumberConcatenationRewriter rewriter = new NumberConcatenationRewriter(false, 3);
        rewriter.rewrite(expandedQuery, null);

        assertThat((Query) expandedQuery.getUserQuery(),
                bq(
                        dmq(
                                term("123"),
                                term("123456")
                        ),
                        dmq(
                                term("456"),
                                term("123456")
                        )
                )
        );
    }

    @Test
    public void testNumberConcatenationForMultipleTokens() {
        Query query = new Query();
        addTerm(query, "a");
        addTerm(query, "123");
        addTerm(query, "456");
        addTerm(query, "789");
        addTerm(query, "b");
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        NumberConcatenationRewriter rewriter = new NumberConcatenationRewriter(false, 3);
        rewriter.rewrite(expandedQuery, null);

        assertThat((Query) expandedQuery.getUserQuery(),
                bq(
                        dmq(
                                term("a")
                        ),
                        dmq(
                                term("123"),
                                term("123456789")
                        ),
                        dmq(
                                term("456"),
                                term("123456789")
                        ),
                        dmq(
                                term("789"),
                                term("123456789")
                        ),
                        dmq(
                                term("b")
                        )
                )
        );
    }

    @Test
    public void testSingleNumericTokenIsPreserved() {
        Query query = new Query();
        addTerm(query, "1");
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        NumberConcatenationRewriter rewriter = new NumberConcatenationRewriter(false, 3);
        rewriter.rewrite(expandedQuery, null);

        assertThat((Query) expandedQuery.getUserQuery(),
                bq(
                        dmq(
                                term("1")
                        )
                )
        );
    }

    @Test
    public void testNumberConcatenationWithinSameField() {
        Query query = new Query();
        addTerm(query, "f1", "123");
        addTerm(query, "f1", "456");
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        NumberConcatenationRewriter rewriter = new NumberConcatenationRewriter(false, 3);
        rewriter.rewrite(expandedQuery, null);

        assertThat((Query) expandedQuery.getUserQuery(),
                bq(
                        dmq(
                                term("f1", "123"),
                                term("f1", "123456")
                        ),
                        dmq(
                                term("f1", "456"),
                                term("f1", "123456")
                        )
                )
        );
    }

    @Test
    public void testNoNumberConcatenationForDifferentFields() {
        Query query = new Query();
        addTerm(query, "f1", "123");
        addTerm(query, "f2", "456");
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        NumberConcatenationRewriter rewriter = new NumberConcatenationRewriter(false, 3);
        rewriter.rewrite(expandedQuery, null);

        assertThat((Query) expandedQuery.getUserQuery(),
                bq(
                        dmq(
                                term("f1", "123")
                        ),
                        dmq(
                                term("f2", "456")
                        )
                )
        );
    }

    @Test
    public void testNoNumberConcatenationForFieldAndAllFieldsQuery() {
        Query query = new Query();
        addTerm(query, "123");
        addTerm(query, "f", "456");
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        NumberConcatenationRewriter rewriter = new NumberConcatenationRewriter(false, 3);
        rewriter.rewrite(expandedQuery, null);

        assertThat((Query) expandedQuery.getUserQuery(),
                bq(
                        dmq(
                                term(null, "123")
                        ),
                        dmq(
                                term("f", "456")
                        )
                )
        );
    }

    @Test
    public void testNoNumberConcatenationForGeneratedTerms() {
        Query query = new Query();
        addTerm(query, "123");
        addTerm(query, "456", true);
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        NumberConcatenationRewriter rewriter = new NumberConcatenationRewriter(false, 3);
        rewriter.rewrite(expandedQuery, null);

        assertThat((Query) expandedQuery.getUserQuery(),
                bq(
                        dmq(
                                term(null, "123")
                        ),
                        dmq(
                                term(null, "456")
                        )
                )
        );
    }

    @Test
    public void testNumberConcatenationForGeneratedTermsIfConfigured() {
        Query query = new Query();
        addTerm(query, "123");
        addTerm(query, "456", true);
        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        NumberConcatenationRewriter rewriter = new NumberConcatenationRewriter(true, 3);
        rewriter.rewrite(expandedQuery, null);

        assertThat((Query) expandedQuery.getUserQuery(),
                bq(
                        dmq(
                                term(null, "123"),
                                term(null, "123456")
                        ),
                        dmq(
                                term(null, "456"),
                                term(null, "123456")
                        )
                )
        );
    }

    private void addTerm(Query query, String value) {
        addTerm(query, null, value);
    }

    private void addTerm(Query query, String field, String value) {
        DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, Clause.Occur.SHOULD, true);
        query.addClause(dmq);
        Term term = new Term(dmq, field, value);
        dmq.addClause(term);
    }

    private void addTerm(Query query, String value, boolean isGenerated) {
        addTerm(query, null, value, isGenerated);
    }

    private void addTerm(Query query, String field, String value, boolean isGenerated) {
        DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, Clause.Occur.SHOULD, true);
        query.addClause(dmq);
        Term term = new Term(dmq, field, value, isGenerated);
        dmq.addClause(term);
    }


}
