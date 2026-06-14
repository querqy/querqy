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
package querqy.rewrite.commonrules.model;

import java.util.HashSet;
import java.util.Set;

import querqy.ComparableCharSequence;
import querqy.model.AbstractNodeVisitor;
import querqy.model.Query;
import querqy.model.Term;

/**
 *
 * Utility class that collects 'generable terms'. These are the terms from boost queries or filter queries
 * as part of rewrite rules. Terms that contain placeholders will not be collected.
 *
 * @author rene
 *
 */
class TermsCollector extends AbstractNodeVisitor<Term> {

    protected final Set<Term> result = new HashSet<Term>();
    
    protected TermsCollector() {
        super();
    }
    
    public Set<Term> getResult() {
        return result;
    }

    @Override
    public Term visit(final Term term) {
        final ComparableCharSequence value = term.getValue();
        if (value instanceof querqy.rewrite.commonrules.model.Term) {
            if (((querqy.rewrite.commonrules.model.Term) value).hasPlaceHolder()) {
                // a term with a placeholder cannot be generated as such
                return term;
            }
        }
        result.add(term);
        return term;
    }
    
    public static Set<Term> collectGenerableTerms(final Query query) {
        final TermsCollector collector = new TermsCollector();
        collector.visit(query);
        return collector.getResult();
    }
    
}
