/**
 * 
 */
package querqy;

import java.util.List;

/**
 * A {@link ComparableCharSequence} that is composed of one or more parts (sub-sequences).
 *
 * @author René Kriegler, @renekrie
 * @author Johannes Peter, @JohannesDaniel
 *
 */

public class CompoundCharSequence implements ComparableCharSequence {
    
    private final CharSequence[] parts;
    private final int[] indexOffsets;

    private final int length;
    private final int seqOffset;
    private final int partsOffset;

    public CompoundCharSequence(final List<? extends CharSequence> parts) {
       this(null, parts);
    }

    public CompoundCharSequence(final CharSequence separator, final List<? extends CharSequence> parts) {
        this(separator, parts.toArray(new CharSequence[parts.size()]));
    }

    /**
     *
     * @param separator A separator that is placed between the parts. Can be null.
     * @param parts The parts to combine.
     */
    public CompoundCharSequence(final CharSequence separator, final CharSequence... parts) {
        if (parts == null || parts.length == 0) {
           throw new IllegalArgumentException("Expecting one or more parts");
        }

        final int arrayLength;

        if (parts.length == 1 || separator == null || separator.length() == 0) {

            arrayLength = parts.length;
            indexOffsets = new int[arrayLength];
            this.parts = parts;

            for (int i = 0; i < arrayLength; i++) {
                this.indexOffsets[i] = i == 0 ? 0 : this.indexOffsets[i - 1] + this.parts[i - 1].length();
            }

        } else {

            arrayLength = parts.length * 2 - 1;
            indexOffsets = new int[arrayLength];
            this.parts = new CharSequence[arrayLength];

            for (int i = 0, j = 0, k = 0; i < arrayLength; i++, j += k, k = -k + 1) {
                this.parts[i] = i % 2 == 0 ? parts[j] : separator;
                this.indexOffsets[i] = i == 0 ? 0 : this.indexOffsets[i - 1] + this.parts[i - 1].length();
            }
        }

        this.length = this.indexOffsets[arrayLength - 1] + this.parts[arrayLength - 1].length();
        this.partsOffset = 0;
        this.seqOffset = 0;
    }

    private CompoundCharSequence(CharSequence[] parts, int[] indexOffsets, int length, int seqOffset, int partsOffset) {

        this.parts = parts;
        this.indexOffsets = indexOffsets;
        this.length = length;
        this.seqOffset = seqOffset;
        this.partsOffset = partsOffset;
    }

    /*
    * (non-Javadoc)
    * 
    * @see java.lang.CharSequence#length()
    */
    @Override
    public int length() {
      return this.length;
    }

    /*
    * (non-Javadoc)
    *
    * @see java.lang.CharSequence#charAt(int)
    */
    @Override
    public char charAt(final int relativeIndex) {

        if (relativeIndex < 0 || relativeIndex >= this.length) {
            throw new ArrayIndexOutOfBoundsException(relativeIndex);
        }

        final int absoluteIndex = this.seqOffset + relativeIndex;

        if (parts.length == 1) {
            return parts[0].charAt(absoluteIndex);
        }

        final int partsIndex = getPartsIndex(absoluteIndex);
        final int baseOffset = indexOffsets[partsIndex];

        return parts[partsIndex].charAt(absoluteIndex - baseOffset);
    }

    private int getPartsIndex(final int index) {

        int partsIndex = 1 + this.partsOffset;
        while (partsIndex < indexOffsets.length) {
            if (index >= indexOffsets[partsIndex]) {
                partsIndex++;
            } else {
                break;
            }
        }

        return partsIndex - 1;
    }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.CharSequence#subSequence(int, int)
    */
    @Override
    public ComparableCharSequence subSequence(final int start, final int end) {
        if (start < 0 || start > end) {
            throw new ArrayIndexOutOfBoundsException(start);
        }

        if (end > this.length) {
            throw new ArrayIndexOutOfBoundsException(end);
        }

        if (start == end) {
            return ComparableCharSequenceWrapper.EMPTY_SEQUENCE;
        }

        final int newLength = end - start;
        final int newSeqOffset = this.seqOffset + start;
        final int newArrayOffset = this.parts.length == 1 ? 0 : getPartsIndex(seqOffset);

        return new CompoundCharSequence(this.parts, this.indexOffsets, newLength, newSeqOffset, newArrayOffset);
    }

    @Override
    public int compareTo(final CharSequence other) {

        // TODO: avoid calls to this.charAt(i) to make comparison faster
        final int length = length();
        for (int i = 0, len = Math.min(length, other.length()); i < len; i++) {
            final char ch1 = charAt(i);
            final char ch2 = other.charAt(i);
            if (ch1 != ch2) {
                return ch1 - ch2;
            }
        }

        return length - other.length();

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

        final StringBuilder buf = new StringBuilder();
        for (int i = 0; i < this.length; i ++) {
            buf.append(this.charAt(i));
        }

        return buf.toString();
    }
}
