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
public class ComparableCharSequenceWrapper implements ComparableCharSequence {
    
    public static final ComparableCharSequence EMPTY_SEQUENCE = new ComparableCharSequenceWrapper("");

    final CharSequence sequence;

    public ComparableCharSequenceWrapper(final CharSequence sequence) {
        this.sequence = sequence;
    }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.CharSequence#length()
    */
   @Override
   public int length() {
      return sequence.length();
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.CharSequence#charAt(int)
    */
   @Override
   public char charAt(final int index) {
      return sequence.charAt(index);
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.CharSequence#subSequence(int, int)
    */
   @Override
   public ComparableCharSequence subSequence(final int start, final int end) {
       // TODO: do subSequence as view in new wrapper
       return new ComparableCharSequenceWrapper(sequence.subSequence(start, end));
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.Comparable#compareTo(java.lang.Object)
    */
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
      return sequence.toString();
   }

}
