/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2014 Querqy Contributors
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
package querqy;

/**
 * @author René Kriegler, @renekrie
 */
public class CharSequenceUtil {

    public static boolean equals(final CharSequence seq1, final Object obj) {
        if (seq1 == obj)
            return true;

        if (seq1 == null || obj == null)
            return false;

        if (!CharSequence.class.isAssignableFrom(obj.getClass()))
            return false;

        final CharSequence seq2 = (CharSequence) obj;
        final int length = seq1.length();

        if (length != seq2.length())
            return false;

        for (int i = 0; i < length; i++) {
            final char ch1 = seq1.charAt(i);
            final char ch2 = seq2.charAt(i);
            if (ch1 != ch2) {
                return false;
            }
        }

        return true;

    }

    public static int compare(final CharSequence seq1, final CharSequence seq2) {
        for (int i = 0, len = Math.min(seq1.length(), seq2.length()); i < len; i++) {
            final char ch1 = seq1.charAt(i);
            final char ch2 = seq2.charAt(i);
            if (ch1 != ch2) {
                return ch1 - ch2;
            }
        }

        return seq1.length() - seq2.length();
    }

    public static int hashCode(final CharSequence seq) {

        final int prime = 31;
        int result = 1;

        for (int i = 0, len = seq.length(); i < len; i++) {
            result = prime * result + seq.charAt(i);
        }

        return result;
    }
}
