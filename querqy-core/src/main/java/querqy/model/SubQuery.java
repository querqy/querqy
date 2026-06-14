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

import java.util.LinkedList;
import java.util.List;

/**
 * @author René Kriegler, @renekrie
 *
 */

// TODO: Methods equals & hashcode should be redefined, as they are currently only comparing clauses but not super
//  attributes. Therefore equals is true even if occur or isGenerated differ.
public abstract class SubQuery<P extends Node, C extends Node> extends Clause<P> {

	protected final List<C> clauses = new LinkedList<>();
	
	public SubQuery(final P parentQuery, final boolean generated) {
		this(parentQuery, Occur.SHOULD, generated);
	}
	
	public SubQuery(final P parentQuery, final Occur occur, final boolean generated) {
		super(parentQuery, occur, generated);
	}
	
	
	@SuppressWarnings("unchecked")
	public <T extends C> List<T> getClauses(final Class<T> type) {
		final List<T> result = new LinkedList<>();
		for (final C clause: clauses) {
			if (type.equals(clause.getClass())) {
				result.add((T) clause);
			}
		}
		return result;
	}
	
	public void addClause(final C clause) {
		if (clause.getParent() != this) {
			throw new IllegalArgumentException("This query is not a parent of " + clause);
		}
		clauses.add(clause);
	}
	
	public void removeClause(final C clause) {
	    if (clause.getParent() != this) {
            throw new IllegalArgumentException("This query is not a parent of " + clause);
        }
	    clauses.remove(clause);
	}

	public abstract void removeClauseAndTraverseTree(final C clause);
	
	public List<C> getClauses() {
		return clauses;
	}

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((clauses == null) ? 0 : clauses.hashCode());
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
        final SubQuery<?,?> other = (SubQuery<?,?>) obj;
        if (clauses == null) {
            if (other.clauses != null)
                return false;
        } else if (!clauses.equals(other.clauses))
            return false;
        return true;
    }

}
