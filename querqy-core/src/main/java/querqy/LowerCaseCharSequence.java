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
 *
 */
public class LowerCaseCharSequence implements ComparableCharSequence {

    final CharSequence delegate;
    
    public LowerCaseCharSequence(final CharSequence delegate) {
        this.delegate = delegate;
    }

    @Override
    public char charAt(final int index) {
        final char ch = delegate.charAt(index);
        return Character.isLowerCase(ch) ? ch : Character.toLowerCase(ch);
    }
    
    @Override
    public ComparableCharSequence subSequence(final int start, final int end) {
        return new LowerCaseCharSequence(delegate.subSequence(start, end));
    }
    
    @Override
    public String toString() {
        return delegate.toString().toLowerCase();
    }

    @Override
    public int length() {
        return delegate.length();
    }

    @Override
    public int compareTo(final CharSequence other) {
        return CharSequenceUtil.compare(this, other);
    }
    
    @Override
    public boolean equals(final Object obj) {
        return CharSequenceUtil.equals(this, obj);
    }
    
    @Override
    public int hashCode() {
        return CharSequenceUtil.hashCode(this);
    }
}
