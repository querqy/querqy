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
public class PRMSDisjunctionMaxQuery implements PRMSQuery {
    
    final List<PRMSQuery> disjuncts;
    
    private Double probability = null;
    
    public PRMSDisjunctionMaxQuery(List<PRMSQuery> disjuncts) {
        if (disjuncts.isEmpty()) {
            throw new IllegalArgumentException("disjuncts.size() > 0 expected");
        }
        this.disjuncts = disjuncts;
    }

    /* (non-Javadoc)
     * @see querqy.lucene.rewrite.prms.PRMSQuery#calculateProbability(org.apache.lucene.index.IndexReader)
     */
    @Override
    public double calculateLikelihood(IndexReader indexReader)
            throws IOException {
        
        if (probability == null) {
            
            double max = 0.0;
            for (PRMSQuery clause: disjuncts) {
                max = Math.max(max, clause.calculateLikelihood(indexReader));
            }
            
            probability = max;
        
        }
        
        return probability;
    }

    public List<PRMSQuery> getDisjuncts() {
        return disjuncts;
    }
    

}
