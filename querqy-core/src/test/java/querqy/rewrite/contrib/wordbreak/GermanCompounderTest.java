/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2022 Querqy Contributors
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
package querqy.rewrite.contrib.wordbreak;

import org.junit.Test;
import querqy.model.Term;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GermanCompounderTest {

    @Test
    public void testWithEmptyCorpus() throws IOException {
        final String field = "f1";
        final TsvTermCorpus corpus = TsvTermCorpus.builder()
                .reader(new StringReader(""))
                .hashFunctions(7)
                .numDocs(0)
                .build();

        final MorphologicalCompounder compounder =
                new MorphologicalCompounder(new MorphologyProvider().get("GERMAN").get(), true, 1);

        final Term left  = new Term(null, field, "left",  false);
        final Term right = new Term(null, field, "left",  false);

        final List<Compounder.CompoundTerm> sequences = compounder.combine(new Term[]{left, right}, corpus, false);

        assertNotNull(sequences);
        assertTrue(sequences.isEmpty());
    }
}
