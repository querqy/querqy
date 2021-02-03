package querqy.rewrite.commonrules;

import java.util.Objects;

/**
 * Just as simple wrapper that marks a String as 'input'.
 */
public class InputString {

    public final String value;

    public InputString(final String value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        this.value = value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof InputString)) return false;
        final InputString that = (InputString) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
