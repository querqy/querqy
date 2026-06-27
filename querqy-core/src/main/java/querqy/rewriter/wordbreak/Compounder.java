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
package querqy.rewriter.wordbreak;

import querqy.model.Term;

import java.io.IOException;
import java.util.List;

public interface Compounder {

    List<CompoundTerm> combine(Term[] terms, TermCorpus termCorpus, boolean reverse) throws IOException;

    class CompoundTerm {

        public final CharSequence value;
        public final Term[] originalTerms;

        public CompoundTerm(final CharSequence value, final Term[] originalTerms) {
            this.value = value;
            this.originalTerms = originalTerms;
        }
    }
}
