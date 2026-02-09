package querqy.regex;

import java.util.List;

public abstract class Symbol {

    protected int minOccur;
    protected int maxOccur; // Integer.MAX_VALUE = infinity

    protected Symbol(final int minOccur, final int maxOccur) {
        this.minOccur = minOccur;
        this.maxOccur = maxOccur;
    }

    public int getMinOccur() {
        return minOccur;
    }

    public int getMaxOccur() {
        return maxOccur;
    }

    public void setQuantifier(int min, int max) {
        this.minOccur = min;
        this.maxOccur = max;
    }

    public static final class CharSymbol extends Symbol {

        private final char value;

        public CharSymbol(final char value) {
            super(1, 1);
            this.value = value;
        }

        public char getValue() {
            return value;
        }

    }

    public static final class AnyDigitSymbol extends Symbol {

        public AnyDigitSymbol() {
            super(1, 1);
        }
    }



    public static final class GroupSymbol extends Symbol {

        private final int groupIndex;
        private final List<Symbol> children;

        public GroupSymbol(final int groupIndex, final List<Symbol> children) {
            super(1, 1);
            this.groupIndex = groupIndex;
            this.children = children;
        }

        public List<Symbol> getChildren() {
            return children;
        }

        public int getGroupIndex() {
            return groupIndex;
        }

    }


}

