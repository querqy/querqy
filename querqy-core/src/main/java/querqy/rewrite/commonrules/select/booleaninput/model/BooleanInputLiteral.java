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
package querqy.rewrite.commonrules.select.booleaninput.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BooleanInputLiteral {

    private final List<String> terms;
    private final List<Reference> references = new ArrayList<>();

    public BooleanInputLiteral(final List<String> terms) {
        this.terms = terms;
    }

    public List<String> getTerms() {
        return this.terms;
    }

    public void addReference(final Reference reference) {
        references.add(reference);
    }

    public List<Reference> getReferences() {
        return this.references;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final BooleanInputLiteral that = (BooleanInputLiteral) o;
        return Objects.equals(terms, that.terms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(terms);
    }
}


