package querqy.lucene.contrib.rewrite.wordbreak;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;

public class GermanUmlautCompoundingWordGeneratorTest {
    @Test
    public void replaceUmlaut_NoSuffixAdding() {
        final GermanUmlautCompoundingWordGenerator generator = new GermanUmlautCompoundingWordGenerator();
        final Optional<CharSequence> res = generator.generateModifier("gans");
        assertThat(res.get(), Matchers.is("gäns"));
    }

    @Test
    public void replaceUmlaut_AddSuffix() {
        final GermanUmlautCompoundingWordGenerator generator = new GermanUmlautCompoundingWordGenerator("e");
        final Optional<CharSequence> res = generator.generateModifier("gans");
        assertThat(res.get(), Matchers.is("gänse"));
    }

    @Test
    public void noUmlautsToReplace() {
        final GermanUmlautCompoundingWordGenerator generator = new GermanUmlautCompoundingWordGenerator("e");
        final Optional<CharSequence> res = generator.generateModifier("finger");
        assertFalse(res.isPresent());
    }
}