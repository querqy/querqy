/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2026 Querqy Contributors
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
package querqy.rewriter.numberunit;

import querqy.rewriter.numberunit.model.NumberUnitDefinition;

import java.util.List;

class ParsedNumberUnitConfig {

    final int scaleForLinearFunctions;
    final List<NumberUnitDefinition> numberUnitDefinitions;

    ParsedNumberUnitConfig(final int scaleForLinearFunctions, final List<NumberUnitDefinition> numberUnitDefinitions) {
        this.scaleForLinearFunctions = scaleForLinearFunctions;
        this.numberUnitDefinitions = numberUnitDefinitions;
    }
}
