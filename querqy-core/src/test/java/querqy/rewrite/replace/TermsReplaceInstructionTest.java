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
package querqy.rewrite.replace;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TermsReplaceInstructionTest {


    private List<CharSequence> list(String... seqs) {
        return new ArrayList<>(Arrays.asList(seqs));
    }

    private List<String> strlist(List<CharSequence> seqs) {
        return seqs.stream().map(CharSequence::toString).collect(Collectors.toList());
    }

    @Test
    public void testRemoveOneTerm() {
        TermsReplaceInstruction replaceInstruction = new TermsReplaceInstruction(list());

        List<CharSequence> input = list("a", "b");
        replaceInstruction.apply(input, 1, 1);
        assertThat(strlist(input)).containsExactly("a");
    }

    @Test
    public void testRemoveManyTerms() {
        TermsReplaceInstruction replaceInstruction = new TermsReplaceInstruction(list());

        List<CharSequence> input = list("a", "b", "c");
        replaceInstruction.apply(input, 1, 2);
        assertThat(strlist(input)).containsExactly("a");
    }

    @Test
    public void testReplaceOneTermByOne() {
        TermsReplaceInstruction replaceInstruction = new TermsReplaceInstruction(list("c"));

        List<CharSequence> input = list("a", "b");
        replaceInstruction.apply(input, 1, 1);
        assertThat(strlist(input)).containsExactly("a", "c");
    }

    @Test
    public void testReplaceOneTermByMany() {

        TermsReplaceInstruction replaceInstruction =
                new TermsReplaceInstruction(list("c", "d"));

        List<CharSequence> input = list("a", "b");
        replaceInstruction.apply(input, 0, 1);
        assertThat(strlist(input)).containsExactly("c", "d", "b");

        input = list("a", "b");
        replaceInstruction.apply(input, 1, 1);
        assertThat(strlist(input)).containsExactly("a", "c", "d");

        input = list("a", "b", "e");
        replaceInstruction.apply(input, 1, 1);
        assertThat(strlist(input)).containsExactly("a", "c", "d", "e");
    }

    @Test
    public void testReplaceManyTermsByMany() {

        TermsReplaceInstruction replaceInstruction =
                new TermsReplaceInstruction(list("d", "e", "f"));

        List<CharSequence> input = list("a", "b", "c");
        replaceInstruction.apply(input, 0, 2);
        assertThat(strlist(input)).containsExactly("d", "e", "f", "c");

        input = list("a", "b", "c");
        replaceInstruction.apply(input, 1, 2);
        assertThat(strlist(input)).containsExactly("a", "d", "e", "f");
    }



}
