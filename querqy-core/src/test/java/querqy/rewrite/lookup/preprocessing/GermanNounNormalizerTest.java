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
package querqy.rewrite.lookup.preprocessing;


import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

import static org.junit.Assert.fail;
import static querqy.TestUtil.list;
import static querqy.TestUtil.resource;

public class GermanNounNormalizerTest {

    @Test
    public void testThatMinInputLengthIsApplied() {
        unchanged("xs");
        unchanged("los");
        match("ass", "asse");
    }

    @Test
    public void testThatStringsContainingDigitsRemainUnchanged() {
        unchanged("93287");
        unchanged("abc7");
        unchanged("ab1c");
        unchanged("8abc");
    }

    @Test
    public void testPairsThatMustNotMatch() throws IOException {

        final List<String> errors = new ArrayList<>();

        for (String line : list(resource("lookup/no-match.txt"))) {

            final int posComment = line.indexOf('#');
            if (posComment > -1) {
                line = line.substring(0, posComment);
            }
            line = line.trim();

            if (line.length() > 0) {
                final String[] parts = line.split(",");
                if (parts.length != 2) {
                    throw new IOException("Invalid input format: " + line);
                }
                final String token1 = parts[0].trim();
                if (token1.isEmpty()) {
                    throw new IOException("Invalid input format: " + line);
                }
                final String token2 = parts[1].trim();
                if (token2.isEmpty()) {
                    throw new IOException("Invalid input format: " + line);
                }

                if (isMatch(token1, token2)) {
                    errors.add(line);
                }

            }
        }

        if (!errors.isEmpty()) {
            fail(errors.size() + " unexpected match(es): " + String.join("; ", errors));
        }
    }


    @Test
    public void testPairsThatMustMatch() throws IOException {

        final List<String> errors = new ArrayList<>();

        for (String line : list(resource("lookup/must-match.txt"))) {

            final int posComment = line.indexOf('#');
            if (posComment > -1) {
                line = line.substring(0, posComment);
            }
            line = line.trim();

            if (line.length() > 0) {
                final String[] parts = line.split(",");
                if (parts.length != 2) {
                    throw new IOException("Invalid input format: " + line);
                }
                final String token1 = parts[0].trim();
                if (token1.isEmpty()) {
                    throw new IOException("Invalid input format: " + line);
                }
                final String token2 = parts[1].trim();
                if (token2.isEmpty()) {
                    throw new IOException("Invalid input format: " + line);
                }

                if (!isMatch(token1, token2)) {
                    errors.add(line);
                }

            }
        }

        if (!errors.isEmpty()) {
            fail(errors.size() + " missing match(es): " + String.join("; ", errors));
        }
    }

    @Test
    public void testSZLigatureCandidates() {
        final GermanNounNormalizer normalizer = new GermanNounNormalizer();
        assertTrue(normalizer.getSZLigatureVariants("").isEmpty());
        assertTrue(normalizer.getSZLigatureVariants("s").isEmpty());
        assertTrue(normalizer.getSZLigatureVariants("Nothing to see").isEmpty());
        assertThat(normalizer.getSZLigatureVariants("masse")).containsExactlyInAnyOrder("maße");
        assertThat(normalizer.getSZLigatureVariants("ss")).containsExactlyInAnyOrder("ß");
        assertThat(normalizer.getSZLigatureVariants("strassenmasse"))
                .containsExactlyInAnyOrder("straßenmasse", "straßenmaße", "strassenmaße");
    }

    private void match(final String token1, final String token2) {
        assertTrue("Not matching: " + token1 + " vs " + token2,  isMatch(token1, token2));
    }

    private void unchanged(final String token) {
        assertTrue(isUnchanged(token));
    }


    private boolean isMatch(final String token1, final String token2) {
        return new GermanNounNormalizer().process(token1)
                .equals(new GermanNounNormalizer().process(token2));
    }

    private boolean isUnchanged(final String token) {
        return new GermanNounNormalizer().process(token).equals(token);
    }

}
