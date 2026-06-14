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

/**
 * @author René Kriegler, @renekrie
 *
 */
public class BooleanQuery extends SubQuery<BooleanParent, BooleanClause> implements DisjunctionMaxClause,
      BooleanClause, BooleanParent, QuerqyQuery<BooleanParent> {

   public BooleanQuery(final BooleanParent parentQuery, final Occur occur, final boolean generated) {
      super(parentQuery, occur, generated);
   }

   @Override
   public <T> T accept(final NodeVisitor<T> visitor) {
      return visitor.visit(this);
   }

   @Override
   public String toString() {
      return "BooleanQuery [occur=" + occur
            + ", clauses=" + clauses + "]";
   }

   @Override
   public BooleanQuery clone(final BooleanParent newParent) {
       return clone(newParent, this.occur, this.generated);
   }

   @Override
   public BooleanQuery clone(final DisjunctionMaxQuery newParent, boolean generated) {
      return clone((BooleanParent) newParent, generated);
   }

   @Override
   public BooleanClause clone(final BooleanQuery newParent) {
      return clone((BooleanParent) newParent);
   }

   @Override
   public BooleanQuery clone(final BooleanParent newParent, final boolean generated) {
       return clone(newParent, this.occur, generated);
   }

   @Override
   public BooleanClause clone(final BooleanQuery newParent, final boolean generated) {
       return clone((BooleanParent) newParent, generated);
   }

    @Override
    public BooleanClause clone(final BooleanQuery newParent, final Occur occur) {
        return clone((BooleanParent) newParent, occur, this.generated);
    }

    @Override
    public BooleanClause clone(final BooleanQuery newParent, final Occur occur, final boolean generated) {
        return clone((BooleanParent) newParent, occur, generated);
    }

    public BooleanQuery clone(final BooleanParent newParent, final Occur occur, final boolean generated) {
        final BooleanQuery bq = new BooleanQuery(newParent, occur, generated);
        for (final BooleanClause clause : clauses) {
            bq.addClause(clause.clone(bq, generated));
        }
        return bq;
    }


    @Override
    public void removeClauseAndTraverseTree(final BooleanClause clause) {
        super.removeClause(clause);

        if (this.clauses.isEmpty() && this.getParent() != null) {
            this.getParent().removeClauseAndTraverseTree(this);
        }
    }

    @Override
    public void removeClauseAndTraverseTree(final BooleanQuery clause) {
        removeClauseAndTraverseTree((BooleanClause) clause);
    }


}
