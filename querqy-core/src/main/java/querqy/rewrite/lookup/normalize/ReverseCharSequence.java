package querqy.rewrite.lookup.normalize;

import querqy.CharSequenceUtil;

// Leaving this class package visible for now as we don't know yet whether we want to encourage using it.
class ReverseCharSequence implements CharSequence {

    private final CharSequence original;
    private final int length;

    ReverseCharSequence(final CharSequence original) {
        this.original = original;
        length = original.length();
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(final int index) {
        return original.charAt(length - 1 - index);
    }

    @Override
    public CharSequence subSequence(final int start, final int end) {
        return new ReverseCharSequence(original.subSequence(length - end, length - start));
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(charAt(i));
        }
        return sb.toString();
    }

    @Override
    public boolean equals(final Object other) {
        return CharSequenceUtil.equals(this, other);
    }

    @Override
    public int hashCode() {
        return CharSequenceUtil.hashCode(this);
    }
}
