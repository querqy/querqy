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
 * A ComparableCharSequence is a CharSequence that
 * <ul>
 * <li>is comparable (following the rules in {@link String#compareTo(String)}</li>
 * <li>defines a contract for hashCode() and equals() - where two CharSequences
 * must have the same hashCode if they have the same sequence of characters, and
 * where two CharSequences are equals if they have the same sequence of
 * characters</li>
 * </ul>
 * 
 * @author rene
 * 
 */
public interface ComparableCharSequence extends CharSequence, Comparable<CharSequence> {

    @Override
    int hashCode();

    @Override
    boolean equals(Object obj);

    @Override
    ComparableCharSequence subSequence(int start, int end);
}
