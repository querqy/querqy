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

import querqy.rewrite.commonrules.model.Instructions;

import java.util.Objects;

public class ExpressionFilterCriterion implements FilterCriterion {

    private final String expression;

    public ExpressionFilterCriterion(final String expression) {
        this.expression = expression;
    }

    @Override
    public boolean isValid(final Instructions instructions) {
        return instructions.getProperties().matches(expression);
    }

    public String getExpression() {
        return expression;
    }

    @Override
    public String toString() {
        return "ExpressionFilterCriterion{" +
                "expression='" + expression + "'}'";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ExpressionFilterCriterion)) return false;
        final ExpressionFilterCriterion that = (ExpressionFilterCriterion) o;
        return Objects.equals(expression, that.expression);
    }

    @Override
    public int hashCode() {
        return 811 + expression.hashCode();
    }
}
