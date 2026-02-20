package querqy.regex;

import java.util.List;

public abstract class Symbol {

    protected int minOccur = 1;
    protected int maxOccur = 1; // Integer.MAX_VALUE = infinity

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
            this.value = value;
        }

        public char getValue() {
            return value;
        }

    }



    static class CharClassSymbol extends Symbol {
        private final CharPredicate predicate;

        CharClassSymbol(CharPredicate predicate) {
            this.predicate = predicate;
        }

        boolean matches(char c) {
            return predicate.matches(c);
        }
    }

    final static CharPredicate ANY_CHAR_PREDICATE = c -> true;

    public static final class AnyCharSymbol extends CharClassSymbol {
        AnyCharSymbol() {
            super(ANY_CHAR_PREDICATE);
        }
    }

    public static final class AnyDigitSymbol extends CharClassSymbol {
        AnyDigitSymbol () {
            super(Character::isDigit);
        }
    }

    public static final class AlternationSymbol extends Symbol {
        public final List<List<Symbol>> alternatives;

        public AlternationSymbol(final List<List<Symbol>> alternatives) {
            this.alternatives = alternatives;
        }
    }

    public static final class GroupSymbol extends Symbol {

        private final int groupIndex;
        private final List<Symbol> children;

        public GroupSymbol(final int groupIndex, final List<Symbol> children) {
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

