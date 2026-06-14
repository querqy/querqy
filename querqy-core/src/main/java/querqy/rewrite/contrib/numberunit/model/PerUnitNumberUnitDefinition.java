/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020 Querqy Contributors
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
package querqy.rewrite.contrib.numberunit.model;

import java.math.BigDecimal;

public class PerUnitNumberUnitDefinition {

    public final NumberUnitDefinition numberUnitDefinition;
    public final BigDecimal multiplier;

    public PerUnitNumberUnitDefinition(final NumberUnitDefinition numberUnitDefinition, final BigDecimal multiplier) {
        this.numberUnitDefinition = numberUnitDefinition;
        this.multiplier = multiplier;
    }

}
