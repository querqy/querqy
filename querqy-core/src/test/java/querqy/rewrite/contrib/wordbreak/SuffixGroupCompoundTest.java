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

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import querqy.rewrite.contrib.wordbreak.*;

public class SuffixGroupCompoundTest {
    private static final float WEIGHT_PATTERN = 0f;

    @Test
    public void nullSuffix_noop_WordGenerator() {
        final SuffixGroup suffixGroup = new SuffixGroup(null,
                Collections.singletonList(
                        new WordGeneratorAndWeight(NoopWordGenerator.INSTANCE, WEIGHT_PATTERN)
                ));

        final List<Suggestion> breakSuggestions = suffixGroup.generateCompoundSuggestions("word1", "word2");
        assertThat(breakSuggestions, hasSize(1));
        assertThat(breakSuggestions.get(0).sequence, is(new CharSequence[]{"word1word2"}));
    }

    @Test
    public void nullSuffix_Plus_S_WordGenerator() {
        final SuffixGroup suffixGroup = new SuffixGroup(null,
                Collections.singletonList(
                        new WordGeneratorAndWeight(new SuffixWordGenerator("s"), WEIGHT_PATTERN)
                ));

        final List<Suggestion> breakSuggestions = suffixGroup.generateCompoundSuggestions("word1_", "_word2");
        assertThat(breakSuggestions, hasSize(1));
        assertThat(breakSuggestions.get(0).sequence, is(new CharSequence[]{"word1_s_word2"}));
    }

    @Test
    public void en_Suffix_noop_WordGenerator() {
        final SuffixGroup suffixGroup = new SuffixGroup("en",
                Collections.singletonList(
                        new WordGeneratorAndWeight(NoopWordGenerator.INSTANCE, WEIGHT_PATTERN)
                ));

        final List<Suggestion> breakSuggestions = suffixGroup.generateCompoundSuggestions("word1_en", "_word2");
        assertThat(breakSuggestions, hasSize(1));
        assertThat(breakSuggestions.get(0).sequence, is(new CharSequence[]{"word1__word2"}));
    }

    @Test
    public void um_Suffix_en_WordGenerator() {
        final SuffixGroup suffixGroup = new SuffixGroup("um",
                Collections.singletonList(
                        new WordGeneratorAndWeight(new SuffixWordGenerator("en"), WEIGHT_PATTERN)
                ));

        final List<Suggestion> breakSuggestions = suffixGroup.generateCompoundSuggestions("word1_um", "_word2");
        assertThat(breakSuggestions, hasSize(1));
        assertThat(breakSuggestions.get(0).sequence, is(new CharSequence[]{"word1_en_word2"}));
    }

    @Test
    public void noSuggestions_whenRemovableSuffixLargerThanWord() {
        final SuffixGroup suffixGroup = new SuffixGroup("big_suffix",
                Collections.singletonList(
                        new WordGeneratorAndWeight(NoopWordGenerator.INSTANCE, WEIGHT_PATTERN)
                ));

        final List<Suggestion> breakSuggestions = suffixGroup.generateCompoundSuggestions("word1", "word2");
        assertThat(breakSuggestions, hasSize(0));
    }

}