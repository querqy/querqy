package querqy.lucene.contrib.rewrite.wordbreak;

import java.util.Optional;

public class WordGeneratorAndWeight {

    public final WordGenerator generator;
    public final float weight;

    public WordGeneratorAndWeight(final WordGenerator generator, final float weight) {
        this.generator = generator;
        this.weight = weight;
    }

    public Optional<BreakSuggestion> breakSuggestion(final CharSequence reducedModifier) {
        final Optional<CharSequence> modifier = generator.generateModifier(reducedModifier);
        return modifier.map(charSequence -> new BreakSuggestion(new CharSequence[]{charSequence}, weight));
    }

}
