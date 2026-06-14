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

import querqy.model.Clause.Occur;

/**
 * @author René Kriegler, @renekrie
 *
 */
public interface BooleanClause extends Node {

    BooleanClause clone(BooleanQuery newParent);
    BooleanClause clone(BooleanQuery newParent, boolean generated);
    BooleanClause clone(BooleanQuery newParent, Occur occur);
    BooleanClause clone(BooleanQuery newParent, Occur occur, boolean generated);
    Occur getOccur();

}
