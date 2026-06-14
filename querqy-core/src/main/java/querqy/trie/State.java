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
package querqy.trie;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class State<T> {
    
    public final T value;
    public final boolean isKnown;
    public final Node<T> node;
    /**
     * The index of the last matching char
     */
    public final int index;
    
    public State(final boolean isKnown, final T value, Node<T> node) {
        this(isKnown, value, node, -1);
    }
    
    public State(final boolean isKnown, final T value, final Node<T> node, final int index) {
        this.isKnown = isKnown;
        this.value = value;
        this.node = node;
        this.index = index;
    }
    
    public boolean isKnown() {
        return isKnown;
    }
    
    public boolean isFinal() {
        return isKnown() && value != null;
    }

    public T getValue() {
        return value;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return "State [value=" + value + ", isKnown=" + isKnown + ", index=" + index + "]";
    }
    
    
}
