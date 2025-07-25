package querqy.lucene.contrib.rewrite.wordbreak;

import java.util.Optional;

public interface WordGenerator {

    /**
     * <p>Generate the modifier word from the reduced modifier. The reduced modifier input parameter will be the
     * modifier word as it occurs in the compound with the suffix from the compound form already stripped off.
     * For example, in 'blatt + wald = blätterwald' this method would be passed 'blätt' as the reduced modifier and is
     * expected to return 'blatt'. In 'kirche + hof = kirchhof', it would be passed 'kirch' and would have to return
     * 'kirche'</p>
     *
     * <p>As a word break strategy might be inapplicable to a given input, the return parameter is optional</p>
     *
     * @param reducedModifier - the modifier in its compound form without a suffix
     *
     * @return An optional modifier word.
     */
    Optional<CharSequence> generateModifier(CharSequence reducedModifier);


}
