/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Querqy Contributors
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
package querqy.rewrite.commonrules.select;

/**
 * This class allows to select rules by using filter expressions for primitive values. Syntax: &quot;name:value&quot;
 */
public class PrimitiveValueSelectionStrategyFactory extends ExpressionCriteriaSelectionStrategyFactory {

    @Override
    public FilterCriterion stringToFilterCriterion(final String s) {

        return criteriaToJsonPathExpressionCriterion(s);

    }

    public static FilterCriterion criteriaToJsonPathExpressionCriterion(final String s) {

        final String str = s.trim();
        if (str.length() < 3) {
            throw new IllegalArgumentException("Invalid criterion string " + s);
        }

        final int pos = str.indexOf(':');
        if (pos < 1) {
            throw new IllegalArgumentException("Invalid criterion string " + s);
        }

        final String name = str.substring(0, pos).trim();
        final String value = str.substring(pos + 1).trim();

        if (name.isEmpty() || value.isEmpty()) {
            throw new IllegalArgumentException("Invalid criterion string " + s);
        }

        return new ExpressionFilterCriterion("$.[?(@." + name + " == '" + value.replace("'", "\\'") + "')]");
    }

}
