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
package querqy.parser;

import querqy.model.Query;

/**
 * Transforms a query string into the {@linkplain Query} model.
 * 
 * @author Shopping24 GmbH, Torsten Bøgh Köster (@tboeghk)
 * @author René Kriegler, @renekrie
 */
public interface QuerqyParser {

    /**
     * Accepts a query input and transforms it into a {@linkplain Query}.
     * @param input The input string, must not be null.
     * @return The query parsed from the input
     */
    Query parse(String input);

}
