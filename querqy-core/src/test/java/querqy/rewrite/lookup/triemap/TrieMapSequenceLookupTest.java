/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2023 Querqy Contributors
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
package querqy.rewrite.lookup.triemap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import querqy.model.Term;
import querqy.rewrite.lookup.LookupConfig;
import querqy.rewrite.lookup.preprocessing.LookupPreprocessor;
import querqy.trie.TrieMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class TrieMapSequenceLookupTest {

    @Mock
    LookupPreprocessor preprocessor;
    @Mock TrieMap<String> trieMap;

    @Captor ArgumentCaptor<CharSequence> charSequenceCaptor;

    TrieMapSequenceLookup<String> trieMapSequenceLookup;

    @Before
    public void prepare() {
        trieMapSequenceLookup = new TrieMapSequenceLookup<>(
                trieMap,
                LookupConfig.builder()
                        .preprocessor(preprocessor)
                        .build()
        );
    }

    @Test
    public void testThat_preprocessedTermIsPassedToMap_forGivenPreprocessor() {
        when(preprocessor.process(any())).thenReturn("b");

        trieMapSequenceLookup.evaluateTerm(term("a"));

        verify(trieMap).get(charSequenceCaptor.capture());
        assertThat(charSequenceCaptor.getValue()).isEqualTo("b");

    }

    private Term term(final String term) {
        return new Term(null, term);
    }

}
