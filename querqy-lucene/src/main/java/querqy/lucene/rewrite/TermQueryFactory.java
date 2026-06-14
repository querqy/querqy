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
package querqy.lucene.rewrite;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

/**
 * @author rene
 *
 */
public class TermQueryFactory implements LuceneQueryFactory<TermQuery> {

    protected final Term term;
    protected final querqy.model.Term sourceTerm;
   
    public TermQueryFactory(final Term term, final querqy.model.Term sourceTerm) {
        this.term = term;
        this.sourceTerm = sourceTerm;
    }

    @Override
    public void prepareDocumentFrequencyCorrection(final DocumentFrequencyCorrection dfc, final boolean isBelowDMQ) {

        if (!isBelowDMQ) {
            // a TQ might end up directly under a BQ as an optimisation
            // make sure, we start a new clause in df correction
            dfc.newClause();
        }

        dfc.prepareTerm(term);

    }

    @Override
    public TermQuery createQuery(final FieldBoost boost, final TermQueryBuilder termQueryBuilder) {

        return termQueryBuilder.createTermQuery(term, boost);

    }

    @Override
    public <R> R accept(final LuceneQueryFactoryVisitor<R> visitor) {
        return visitor.visit(this);
    }

    public String getFieldname() {
        return term.field();
    }
}
