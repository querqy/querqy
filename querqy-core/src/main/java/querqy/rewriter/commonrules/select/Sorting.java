/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Querqy Contributors
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
package querqy.rewriter.commonrules.select;

import querqy.rewriter.commonrules.model.Instructions;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * @author René Kriegler, @renekrie
 */
public interface Sorting {

    enum SortOrder {

        ASC(1), DESC(-1);

        public final int factor;

        SortOrder(final int factor) {
            this.factor = factor;
        }

        public static SortOrder fromString(final String s) {
            switch (s) {
                case "asc": return ASC;
                case "desc": return DESC;
                default:
                    throw new IllegalArgumentException("Invalid sort order: " + s);
            }
        }

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }


    Comparator<Instructions> DEFAULT_COMPARATOR = new Sorting.ConfigOrderComparator(SortOrder.ASC);

    Sorting DEFAULT_SORTING = new Sorting() {

        private final List<Comparator<Instructions>> comparators = Collections.singletonList(DEFAULT_COMPARATOR);

        @Override
        public List<Comparator<Instructions>> getComparators() {
            return comparators;
        }

    };


    List<Comparator<Instructions>> getComparators();


    class ConfigOrderComparator implements Comparator<Instructions> {

        private final int factor;

        ConfigOrderComparator(final SortOrder sortOrder) {
            this.factor = sortOrder.factor;
        }


        @Override
        public int compare(final Instructions instructions1, final Instructions instructions2) {
            return (instructions1.getOrd() - instructions2.getOrd()) * factor;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ConfigOrderComparator)) return false;
            ConfigOrderComparator that = (ConfigOrderComparator) o;
            return factor == that.factor;
        }

        @Override
        public int hashCode() {
            return Objects.hash(factor);
        }

    }

}
