/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2015 Querqy Contributors
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

import org.apache.lucene.search.Query;

import querqy.lucene.rewrite.prms.PRMSQuery;
import querqy.model.Term;

/**
 * A LuceneQueryFactory that wraps a TermQueryFactory or the factory of a more complex
 * query that results from Lucene analysis.
 * 
 * @author rene
 *
 */
public class TermSubQueryFactory implements LuceneQueryFactory<Query> {
    
    final LuceneQueryFactory<?> root;
    final FieldBoost boost;
    public final PRMSQuery prmsQuery;
    private final Term sourceTerm;
    private final String fieldname;
    
    public TermSubQueryFactory(final LuceneQueryFactoryAndPRMSQuery rootAndPrmsQuery, final FieldBoost boost,
                               final Term sourceTerm, final String fieldname) {
        this(rootAndPrmsQuery.queryFactory, rootAndPrmsQuery.prmsQuery, boost, sourceTerm, fieldname);
    }
    
    public TermSubQueryFactory(final LuceneQueryFactory<?> root, final PRMSQuery prmsQuery, final FieldBoost boost,
                               final Term sourceTerm, final String fieldname) {
        this.root = root;
        this.boost = boost;
        this.prmsQuery = prmsQuery;
        this.sourceTerm = sourceTerm;
        this.fieldname = fieldname;
    }

    @Override
    public void prepareDocumentFrequencyCorrection(final DocumentFrequencyCorrection dfc, final boolean isBelowDMQ) {
        root.prepareDocumentFrequencyCorrection(dfc, isBelowDMQ);
    }

    @Override
    public Query createQuery(final FieldBoost boost, final TermQueryBuilder termQueryBuilder) {
        
        return root.createQuery(this.boost, termQueryBuilder);
    }
    
    public boolean isNeverMatchQuery() {
        return root instanceof NeverMatchQueryFactory;
    }

    @Override
    public <R> R accept(final LuceneQueryFactoryVisitor<R> visitor) {
        return visitor.visit(this);
    }

    public String getFieldname() {
        return fieldname;
    }

    public Term getSourceTerm() {
        return sourceTerm;
    }
}
