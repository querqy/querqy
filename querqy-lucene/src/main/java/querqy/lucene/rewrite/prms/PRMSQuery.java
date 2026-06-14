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

import org.apache.lucene.index.IndexReader;

/**
 * A query in the 'Probabilistic Retrieval Model for Semi-structured Data'
 *  
 * @author rene
 *
 */

public interface PRMSQuery {
    
    PRMSQuery NEVER_MATCH_PRMS_QUERY = indexReader -> 0.0;
    
    /**
     * Calculate the probability of this query for a given index
     *
     * @param indexReader The IndexReader
     * @return The probability of this query
     * @throws IOException In case of an error when reading from the index
     */
    double calculateLikelihood(IndexReader indexReader) throws IOException;

}
