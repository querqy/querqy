package querqy.rewrite.lookup.preprocessing;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GermanNormalizationPreprocessorTest {

    @Test
    public void testThat_sharpSIsReplaced_atTheBeginning() {
        final GermanNormalizationPreprocessor preprocessor = GermanNormalizationPreprocessor.create();
        assertThat(preprocessor.process("ßh"))
                .isEqualTo("ssh");
    }

    @Test
    public void testThat_sharpSIsReplaced_atTheEnd() {
        final GermanNormalizationPreprocessor preprocessor = GermanNormalizationPreprocessor.create();
        assertThat(preprocessor.process("hß"))
                .isEqualTo("hss");
    }

    @Test
    public void testThat_sharpSIsReplaced_inTheMiddle() {
        final GermanNormalizationPreprocessor preprocessor = GermanNormalizationPreprocessor.create();
        assertThat(preprocessor.process("hßh"))
                .isEqualTo("hssh");
    }

    @Test
    public void testThat_aeIsReplaced_atTheBeginning() {
        final GermanNormalizationPreprocessor preprocessor = GermanNormalizationPreprocessor.create();
        assertThat(preprocessor.process("aerger"))
                .isEqualTo("ärger");
    }

    @Test
    public void testThat_aeIsReplaced_atTheEnd() {
        final GermanNormalizationPreprocessor preprocessor = GermanNormalizationPreprocessor.create();
        assertThat(preprocessor.process("mariae"))
                .isEqualTo("mariä");
    }

    @Test
    public void testThat_aeIsReplaced_inTheMiddle() {
        final GermanNormalizationPreprocessor preprocessor = GermanNormalizationPreprocessor.create();
        assertThat(preprocessor.process("maehne"))
                .isEqualTo("mähne");
    }

    @Test
    public void testThat_aeIsReplaced_multipleTimes() {
        final GermanNormalizationPreprocessor preprocessor = GermanNormalizationPreprocessor.create();
        assertThat(preprocessor.process("aehaehae"))
                .isEqualTo("ähähä");
    }

    @Test
    public void testThat_oeIsReplaced_atTheBeginning() {
        final GermanNormalizationPreprocessor preprocessor = GermanNormalizationPreprocessor.create();
        assertThat(preprocessor.process("oeh"))
                .isEqualTo("öh");
    }

    @Test
    public void testThat_oeIsReplaced_atTheEnd() {
        final GermanNormalizationPreprocessor preprocessor = GermanNormalizationPreprocessor.create();
        assertThat(preprocessor.process("hoe"))
                .isEqualTo("hö");
    }

    @Test
    public void testThat_oeIsReplaced_inTheMiddle() {
        final GermanNormalizationPreprocessor preprocessor = GermanNormalizationPreprocessor.create();
        assertThat(preprocessor.process("hoeh"))
                .isEqualTo("höh");
    }

    @Test
    public void testThat_oeIsReplaced_multipleTimes() {
        final GermanNormalizationPreprocessor preprocessor = GermanNormalizationPreprocessor.create();
        assertThat(preprocessor.process("oehoehoe"))
                .isEqualTo("öhöhö");
    }

    @Test
    public void testThat_ueIsReplaced_atTheBeginning() {
        final GermanNormalizationPreprocessor preprocessor = GermanNormalizationPreprocessor.create();
        assertThat(preprocessor.process("ueh"))
                .isEqualTo("üh");
    }

    @Test
    public void testThat_ueIsReplaced_atTheEnd() {
        final GermanNormalizationPreprocessor preprocessor = GermanNormalizationPreprocessor.create();
        assertThat(preprocessor.process("hue"))
                .isEqualTo("hü");
    }

    @Test
    public void testThat_ueIsReplaced_inTheMiddle() {
        final GermanNormalizationPreprocessor preprocessor = GermanNormalizationPreprocessor.create();
        assertThat(preprocessor.process("hueh"))
                .isEqualTo("hüh");
    }

    @Test
    public void testThat_ueIsReplaced_multipleTimes() {
        final GermanNormalizationPreprocessor preprocessor = GermanNormalizationPreprocessor.create();
        assertThat(preprocessor.process("uehuehue"))
                .isEqualTo("ühühü");
    }

    @Test
    public void testThat_ueIsNotReplaced_ifPrependedByA() {
        final GermanNormalizationPreprocessor preprocessor = GermanNormalizationPreprocessor.create();
        assertThat(preprocessor.process("uehaue"))
                .isEqualTo("ühaue");
    }

    @Test
    public void testThat_aouAreNotReplaced_ifNotFollowedByE() {
        final GermanNormalizationPreprocessor preprocessor = GermanNormalizationPreprocessor.create();
        assertThat(preprocessor.process("aouhahohu"))
                .isEqualTo("aouhahohu");
    }

    @Test
    public void testThat_nothingIsThrown_forEAtDifferentPositions() {
        final GermanNormalizationPreprocessor preprocessor = GermanNormalizationPreprocessor.create();
        assertThat(preprocessor.process("eee"))
                .isEqualTo("eee");
    }

}
