/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020 Querqy Contributors
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
