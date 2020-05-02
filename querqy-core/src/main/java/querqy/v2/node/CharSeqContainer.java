package querqy.v2.node;

// TODO: make fully abstract
public abstract class CharSeqContainer implements CharSequence {

    protected final CharSequence seq;

    public CharSeqContainer(final CharSequence seq) {
        this.seq = seq;
    }

    public abstract CharSequence getCharSeq();

    @Override
    public int length() {
        return seq.length();
    }

    @Override
    public char charAt(int index) {
        return seq.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return seq.subSequence(start, end);
    }

    @Override
    public String toString() {
        return seq.toString();
    }



}
