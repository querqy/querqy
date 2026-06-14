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
package querqy.lucene.rewrite.prms;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.IndexReader;

/**
 * @author rene
 *
 */
public class PRMSAndQuery implements PRMSQuery {
    
    final List<PRMSQuery> clauses;
    
    private Double probability = null;
    
    public PRMSAndQuery(List<PRMSQuery> clauses) {
        if (clauses.isEmpty()) {
            throw new IllegalArgumentException("clauses.size() > 0 expected");
        }
        this.clauses = clauses;
    }

    /* (non-Javadoc)
     * @see querqy.lucene.rewrite.prms.PRMSQuery#calculateProbability(org.apache.lucene.index.IndexReader)
     */
    @Override
    public double calculateLikelihood(IndexReader indexReader)
            throws IOException {
        
        if (probability == null) {
            
            // We would need the joint probability of all clauses, which would be too expensive
            // to calculate from the index. As a workaround, we use the minimum probability of all 
            // clauses as the joint probability can never be greater than this in:
            // P(Clause1 ^ Clause2) = P(Clause1 | Clause2) * P(Clause2) = P(Clause2 | Clause1) * P(Clause1)
            double min = 1.0;
            for (PRMSQuery clause: clauses) {
                min = Math.min(min, clause.calculateLikelihood(indexReader));
            }
            probability = min;
            
        }
        
        return probability;
        
    }

    public List<PRMSQuery> getClauses() {
        return clauses;
    }

}
