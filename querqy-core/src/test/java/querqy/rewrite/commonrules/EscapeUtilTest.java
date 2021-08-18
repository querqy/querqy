package querqy.rewrite.commonrules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static querqy.rewrite.commonrules.EscapeUtil.unescape;

import org.junit.Test;

public class EscapeUtilTest {

    @Test
    public void testUnescape() {
        assertThat(unescape("\\*")).isEqualTo("*");
        assertThat(unescape("\\\"")).isEqualTo("\"");
        assertThat(unescape("\\#")).isEqualTo("#");
        assertThat(unescape("\\\\")).isEqualTo("\\");
        assertThat(unescape("\\\\*")).isEqualTo("\\*");
        assertThatThrownBy(() -> unescape("\\1")).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(unescape("a\\\\")).isEqualTo("a\\");
        assertThat(unescape("a\\*")).isEqualTo("a*");
        assertThat(unescape("\\")).isEqualTo("");
    }

}
