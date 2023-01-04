package querqy.rewrite.lookup.preprocessing;

import querqy.CompoundCharSequence;
import querqy.SimpleComparableCharSequence;

import java.util.ArrayList;
import java.util.List;

public class GermanNormalizationPreprocessor implements Preprocessor {

    private static final char A = 'a';
    private static final char O = 'o';
    private static final char U = 'u';

    private static final char E = 'e';

    private static final CharSequence AE = "ä";
    private static final CharSequence OE = "ö";
    private static final CharSequence UE = "ü";

    private static final char SHARP_S = 'ß';
    private static final CharSequence SS = "ss";


    // private constructor


    @Override
    public CharSequence process(final CharSequence charSequence) {
        final CharSequenceNormalizer charSequenceNormalizer = new CharSequenceNormalizer(charSequence);
        return charSequenceNormalizer.normalize();
    }

    public static GermanNormalizationPreprocessor create() {
        return new GermanNormalizationPreprocessor();
    }

    private static class CharSequenceNormalizer {
        private final CharSequence charSequence;
        private final int length;

        private int start = 0;
        private int offset = 0;

        private StringBuilder stringBuilder = null;

        private CharSequenceNormalizer(final CharSequence charSequence) {
            this.charSequence = charSequence;
            this.length = charSequence.length();
        }

        public CharSequence normalize() {
            while (offset < length) {
                final char c = charSequence.charAt(offset);

                if (c == E && offset > 0) {
                    normalizeUmlauts();

                } else if (c == SHARP_S) {
                    normalizeSharpS();
                }

                offset++;
            }

            return build();
        }

        private void normalizeUmlauts() {
            if (isPrependedBy(A)) {
                normalizeUmlaut(AE);

            } else if (isPrependedBy(O)) {
                normalizeUmlaut(OE);

            } else if (isPrependedBy(U) && !isAdditionallyPrependedByA()) {
                normalizeUmlaut(UE);
            }
        }

        private boolean isPrependedBy(final char c) {
            return charSequence.charAt(offset - 1) == c;
        }

        private boolean isAdditionallyPrependedByA() {
            return offset > 1 && charSequence.charAt(offset - 2) == A;
        }

        private void normalizeUmlaut(final CharSequence umlaut) {
            append(charSequence.subSequence(start, offset - 1));
            append(umlaut);
            start = offset + 1;
        }

        private void normalizeSharpS() {
            append(charSequence.subSequence(start, offset));
            append(SS);
            start = offset + 1;
        }

        private void append(final CharSequence charSequence) {
            if (stringBuilder == null) {
                stringBuilder = new StringBuilder();
            }

            stringBuilder.append(charSequence);
        }

        private CharSequence build() {
            if (stringBuilder != null) {
                stringBuilder.append(charSequence.subSequence(start, offset));
                return stringBuilder.toString();

            } else {
                return charSequence;
            }
        }
    }
}
