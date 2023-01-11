package querqy.rewrite.lookup.normalize;


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
        unchanged("los"); // TODO: add test for 4 chars that gets changed
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
        return new GermanNounNormalizer().normalize(token1)
                .equals(new GermanNounNormalizer().normalize(token2));
    }

    private boolean isUnchanged(final String token) {
        return new GermanNounNormalizer().normalize(token).equals(token);
    }

}