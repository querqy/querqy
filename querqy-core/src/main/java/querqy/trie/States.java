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
package querqy.trie;

import java.util.LinkedList;
import java.util.List;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class States<T> {
    
    private List<State<T>> prefixes = null;
    private final State<T> completeSequence;
    
    public States(final State<T> completeSequence) {
        this.completeSequence = completeSequence;
    }
    
    public void addPrefix(final State<T> prefix) {
        if (prefixes == null) {
            prefixes = new LinkedList<>();
        }
        prefixes.add(prefix);
    }
    
    public State<T> getStateForCompleteSequence() {
        return completeSequence;
    }

    public List<State<T>> getPrefixes() {
        return prefixes;
    }

    @Override
    public String toString() {
        return "States [prefixes=" + prefixes + ", completeSequence="
                + completeSequence + "]";
    }
    
    
}
