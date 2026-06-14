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
package querqy.model;

import java.util.List;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class DisjunctionMaxQuery extends SubQuery<BooleanQuery, DisjunctionMaxClause> implements BooleanClause,
      BooleanParent {

   public DisjunctionMaxQuery(final BooleanQuery parentQuery, final Occur occur, final boolean generated) {
      super(parentQuery, occur, generated);
   }

   public List<Term> getTerms() {
      return getClauses(Term.class);
   }

   @Override
   public <T> T accept(final NodeVisitor<T> visitor) {
      return visitor.visit(this);
   }

   @Override
   public String toString() {
      return "DisjunctionMaxQuery [occur=" + occur + ", clauses=" + clauses
            + "]";
   }

   @Override
   public BooleanClause clone(final BooleanQuery newParent) {
       return clone(newParent, this.occur, this.generated);
   }

   @Override
   public BooleanClause clone(final BooleanQuery newParent, final boolean generated) {
       return clone(newParent, this.occur, generated);
   }

   @Override
   public BooleanClause clone(final BooleanQuery newParent, final Occur occur) {
       return clone(newParent, this.generated);
   }

   @Override
   public BooleanClause clone(final BooleanQuery newParent, final Occur occur, final boolean generated) {
       final DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(newParent, occur, generated);
       for (final DisjunctionMaxClause clause : clauses) {
           dmq.addClause(clause.clone(dmq, generated));
       }
       return dmq;
   }

    @Override
    public void removeClauseAndTraverseTree(final DisjunctionMaxClause clause) {
        super.removeClause(clause);

        if (this.clauses.isEmpty() && this.getParent() != null) {
            this.getParent().removeClauseAndTraverseTree(this);
        }
    }

    @Override
    public void removeClauseAndTraverseTree(final BooleanQuery booleanQuery) {
        this.removeClauseAndTraverseTree((DisjunctionMaxClause) booleanQuery);
    }
}
