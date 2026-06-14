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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.ToStringUtils;

import querqy.model.Term;

/**
 * In this FieldBoost implementation, the boost factors of the fields do not depend on each other.
 * 
 * @author rene
 *
 */
public class IndependentFieldBoost implements FieldBoost {
    
    final Map<String, Float> queryFieldsAndBoostings;
    final float defaultGeneratedFieldBoostFactor;
    final Set<String> generatedFields;
    
    public IndependentFieldBoost(Map<String, Float> queryFieldsAndBoostings, float defaultGeneratedFieldBoostFactor) {
        this.queryFieldsAndBoostings = queryFieldsAndBoostings;
        this.defaultGeneratedFieldBoostFactor = defaultGeneratedFieldBoostFactor;
        generatedFields = new HashSet<>();
    }
    
    @Override
    public float getBoost(String fieldname, IndexReader indexReader) {
        return getBoost(fieldname);
    }
    
    public float getBoost(String fieldname) {
        
        Float boost = queryFieldsAndBoostings.get(fieldname);
        
        return boost != null 
                ? boost : (generatedFields.contains(fieldname) 
                            ? defaultGeneratedFieldBoostFactor 
                            : 1f
                          ) ;
    }

    

    @Override
    public void registerTermSubQuery(final TermSubQueryFactory termSubQueryFactory) {
        if (termSubQueryFactory.getSourceTerm().isGenerated()) {
            generatedFields.add(termSubQueryFactory.getFieldname());
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = prime
                + Float.floatToIntBits(defaultGeneratedFieldBoostFactor);
        result = prime
                * result
                + ((queryFieldsAndBoostings == null) ? 0
                        : queryFieldsAndBoostings.hashCode());
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
        IndependentFieldBoost other = (IndependentFieldBoost) obj;
        if (Float.floatToIntBits(defaultGeneratedFieldBoostFactor) != Float
                .floatToIntBits(other.defaultGeneratedFieldBoostFactor))
            return false;
        if (queryFieldsAndBoostings == null) {
            if (other.queryFieldsAndBoostings != null)
                return false;
        } else if (!queryFieldsAndBoostings
                .equals(other.queryFieldsAndBoostings))
            return false;
        return true;
    }

    @Override
    public String toString(String fieldname) {

        final float boost = getBoost(fieldname);

        if (boost != 1f) {
            return "^" + boost;
        } else {
            return "";
        }

    }

}
