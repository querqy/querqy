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
