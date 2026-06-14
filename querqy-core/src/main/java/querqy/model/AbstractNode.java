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
public abstract class AbstractNode<P extends Node> implements CloneableNode<P> {
	
	protected final P parent;
	
	protected final boolean generated;
	
	public AbstractNode(P parent, boolean isGenerated) {
		this.parent = parent;
		this.generated = isGenerated;
	}

	@Override
	public P getParent() {
		return parent;
	}

	/* (non-Javadoc)
	 * @see querqy.model.Node#isGenerated()
	 */
	@Override
	public boolean isGenerated() {
		return generated;
	}

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (generated ? 1231 : 1237);
        result = prime * result + ((parent == null) ? 0 : parent.hashCode());
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
        AbstractNode<?> other = (AbstractNode<?>) obj;
        if (generated != other.generated)
            return false;
        if (parent == null) {
            if (other.parent != null)
                return false;
        } else if (!parent.equals(other.parent))
            return false;
        return true;
    }

    
	
	
}
