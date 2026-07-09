/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2026 Querqy Contributors
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
package querqy.rewriter.commonrules.model;

import java.util.Collection;
import java.util.List;

/**
 * A term with a leading wildcard, e.g. <code>*hemd</code>. The wildcard matches one or more arbitrary
 * characters at the start of a query term, while {@link #value} holds the fixed suffix that must follow
 * (just the suffix characters, without any wildcard marker).
 *
 * @author René Kriegler, @renekrie
 */
public class SuffixTerm extends Term {

    /**
     *
     * @param value The suffix (just the suffix characters, without any wildcard marker)
     * @param start The offset into the value array
     * @param length The number of characters in the suffix
     * @param fieldNames The field names of this term
     */
    public SuffixTerm(char[] value, int start, int length, List<String> fieldNames) {
        super(value, start, length, fieldNames);
    }

    public boolean isSuffixOfCharSequence(final CharSequence other) {
        final int otherLength = other.length();
        if (length > otherLength) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (value[start + length - 1 - i] != other.charAt(otherLength - 1 - i)) {
                return false;
            }
        }
        // one or more characters must be matched by the wildcard
        return length < otherLength;
    }

    public boolean isSuffixOf(final Term other) {

        if (isSuffixOfCharSequence(other)) {

            if (fieldNames == other.fieldNames) {
                return true;
            } else {
                if (other.fieldNames != null && fieldNames != null) {
                    for (String name : fieldNames) {
                        if (other.fieldNames.contains(name)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public boolean isSuffixOf(final querqy.model.Term other) {

        if (isSuffixOfCharSequence(other)) {

            final String otherFieldname = other.getField();

            if (fieldNames == null) {
                return true;
            } else if (otherFieldname != null && fieldNames.contains(otherFieldname)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Term findFirstMatch(final Collection<? extends Term> haystack) {

        for (Term h : haystack) {

            if (isSuffixOf(h)) {
                return h;
            }

        }

        return null;

    }
}
