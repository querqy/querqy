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
package querqy.rewriter.commonrules.model;

import java.util.Map;
import java.util.Set;

import querqy.model.BooleanQuery;
import querqy.model.ExpandedQuery;
import querqy.model.QuerqyQuery;
import querqy.model.Query;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.SearchEngineRequestAdapter;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class FilterInstruction implements Instruction {

   final QuerqyQuery<?> filterQuery;
    private final InstructionDescription instructionDescription;

    @Deprecated // Use only for testing
    public FilterInstruction(final QuerqyQuery<?> filterQuery) {
        this(filterQuery, InstructionDescription.empty());
    }

    public FilterInstruction(final QuerqyQuery<?> filterQuery, final InstructionDescription instructionDescription) {
        if (filterQuery == null) {
            throw new IllegalArgumentException("filterQuery must not be null");
        }

        this.filterQuery = filterQuery instanceof BooleanQuery
                ? InstructionHelper.applyMinShouldMatchAndGeneratedToBooleanQuery((BooleanQuery) filterQuery)
                : filterQuery;

        this.instructionDescription = instructionDescription;
    }


    /* (non-Javadoc)
    * * @see querqy.rewriter.commonrules.model.Instruction#apply(querqy.rewriter.commonrules.model.PositionSequence, querqy.rewriter.commonrules.model.TermMatches, int, int, querqy.model.ExpandedQuery, java.util.Map)
    */
    @Override
    public void apply(final TermMatches termMatches, final ExpandedQuery expandedQuery,
                      final SearchEngineRequestAdapter searchEngineRequestAdapter) {

        // TODO: we might not need to clone here, if we already cloned all queries in the constructor
        expandedQuery.addFilterQuery(filterQuery.clone(null, true));
    }
   
    @Override
    public Set<Term> getGenerableTerms() {
        return (filterQuery instanceof Query) 
            ?  TermsCollector.collectGenerableTerms((Query) filterQuery)
            : QueryRewriter.EMPTY_GENERABLE_TERMS;
    }

    @Override
    public InstructionDescription getInstructionDescription() {
        return instructionDescription;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
            + ((filterQuery == null) ? 0 : filterQuery.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final FilterInstruction other = (FilterInstruction) obj;
        if (filterQuery == null) {
            if (other.filterQuery != null)
                return false;
        } else if (!filterQuery.equals(other.filterQuery))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "FilterInstruction [filterQuery=" + filterQuery + "]";
    }

}
