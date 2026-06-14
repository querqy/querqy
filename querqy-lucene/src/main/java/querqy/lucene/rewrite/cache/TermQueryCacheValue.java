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
package querqy.lucene.rewrite.cache;

import querqy.lucene.rewrite.LuceneQueryFactory;
import querqy.lucene.rewrite.LuceneQueryFactoryAndPRMSQuery;
import querqy.lucene.rewrite.prms.PRMSQuery;

/**
 * @author rene
 *
 */
public class TermQueryCacheValue extends LuceneQueryFactoryAndPRMSQuery {
    
    
    public TermQueryCacheValue(LuceneQueryFactoryAndPRMSQuery queryFactoryAndPRMSQuery) {
        this(queryFactoryAndPRMSQuery.queryFactory, queryFactoryAndPRMSQuery.prmsQuery);
    }
    
    public TermQueryCacheValue(LuceneQueryFactory<?> queryFactory, PRMSQuery prmsQuery) {
        super(queryFactory, prmsQuery);
    }
    
    public boolean hasQuery() {
        return queryFactory != null;
    }
    

}
