package querqy;

import java.util.stream.Collector;
import java.util.stream.IntStream;

public class ReverseComparableCharSequence implements ComparableCharSequence {

    private final CharSequence sequence;
    private final int startIndexRev;
    private final int length;

    public ReverseComparableCharSequence(final CharSequence sequence) {
        this.sequence = sequence;
        this.startIndexRev = sequence.length() - 1;
        this.length = sequence.length();
    }

    @Override
    public int length() {
      return this.length;
   }

    @Override
    public char charAt(final int index) {
        return sequence.charAt(startIndexRev - index);
    }

    @Override
    public ComparableCharSequence subSequence(final int start, final int end) {
        if (start < 0 || start > end || end > length) {
            throw new StringIndexOutOfBoundsException(String.format("begin %s, end %s, length %s", start, end, length));
        }

        return new ReverseComparableCharSequence(sequence.subSequence(this.length - end, this.length - start));
    }

    @Override
    public int compareTo(final CharSequence other) {
        return CharSequenceUtil.compare(this, other);
    }

    @Override
    public int hashCode() {
        return CharSequenceUtil.hashCode(this);
    }

    @Override
    public boolean equals(final Object obj) {
        return CharSequenceUtil.equals(this, obj);
    }

    @Override
    public String toString() {
        return IntStream.range(0, this.length())
                .boxed()
                .map(this::charAt)
                .collect(Collector.of(
                        StringBuilder::new,
                        StringBuilder::append,
                        StringBuilder::append,
                        StringBuilder::toString));
    }

}
