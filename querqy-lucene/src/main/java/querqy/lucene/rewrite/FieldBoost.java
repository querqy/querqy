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

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;

import querqy.model.Term;

/**
 * <p>A FieldBoost provides the boost factors for all Lucene queries that are created from a single Querqy query Term.</p>
 * <p>FieldBoosts must implement the equals and hashCode methods</p> 
 * 
 * @author rene
 *
 */
public interface FieldBoost {
    
    float getBoost(String fieldname, IndexReader indexReader) throws IOException;
    
    void registerTermSubQuery(TermSubQueryFactory termSubQueryFactory);
    
    String toString(String fieldname);
    
    int hashCode();
    
    boolean equals(Object obj);

}
