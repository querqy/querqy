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
package querqy.lucene.contrib.rewrite.wordbreak;

import org.junit.Assert;
import org.junit.Test;
import querqy.trie.TrieMap;

import java.util.Arrays;
import java.util.Collections;

public class ClassicWordBreakCompoundRewriterFactoryTest {

    @Test
    public void testThatTriggerWordsAreTurnedToLowerCaseForFlagLowerCaseInputTrue() {
        final ClassicWordBreakCompoundRewriterFactory factory = new ClassicWordBreakCompoundRewriterFactory("w1", () -> null,
                "field1", true, 1, 2, 1, Arrays.asList("Word1", "word2"), false, 2, false, Collections.emptyList());

        final TrieMap<Boolean> triggerWords = factory.getReverseCompoundTriggerWords();
        Assert.assertTrue(triggerWords.get("word1").getStateForCompleteSequence().isFinal());
        Assert.assertTrue(triggerWords.get("word2").getStateForCompleteSequence().isFinal());

    }

    @Test
    public void testThatTriggerWordsAreTurnedToLowerCaseForFlagLowerCaseInputFalse() {
        final ClassicWordBreakCompoundRewriterFactory factory = new ClassicWordBreakCompoundRewriterFactory("w2", () -> null,
                "field1", false, 1, 2, 1, Arrays.asList("Word1", "word2"), false, 2, false, Collections.emptyList());

        final TrieMap<Boolean> triggerWords = factory.getReverseCompoundTriggerWords();
        Assert.assertFalse(triggerWords.get("word1").getStateForCompleteSequence().isFinal());
        Assert.assertTrue(triggerWords.get("Word1").getStateForCompleteSequence().isFinal());
        Assert.assertTrue(triggerWords.get("word2").getStateForCompleteSequence().isFinal());

    }

    @Test
    public void testThatProtectedWordsAreMatchedCaseInsensitiveForFlagLowerCaseInputTrue() {
        final ClassicWordBreakCompoundRewriterFactory factory = new ClassicWordBreakCompoundRewriterFactory("w1", () -> null,
                "field1", true, 1, 2, 1, Collections.emptyList(), false, 2, false,
                Collections.singletonList("Protected"));

        final TrieMap<Boolean> protectedWords = factory.getProtectedWords();
        Assert.assertTrue(protectedWords.get("protected").getStateForCompleteSequence().isFinal());

    }

    @Test
    public void testThatProtectedWordsAreMatchedCaseSensitiveForFlagLowerCaseInputFalse() {
        final ClassicWordBreakCompoundRewriterFactory factory = new ClassicWordBreakCompoundRewriterFactory("w1", () -> null,
                "field1", false, 1, 2, 1, Collections.emptyList(), false, 2, false,
                Collections.singletonList("Protected"));

        final TrieMap<Boolean> protectedWords = factory.getProtectedWords();
        Assert.assertTrue(protectedWords.get("Protected").getStateForCompleteSequence().isFinal());
        Assert.assertFalse(protectedWords.get("protected").getStateForCompleteSequence().isFinal());

    }
}
