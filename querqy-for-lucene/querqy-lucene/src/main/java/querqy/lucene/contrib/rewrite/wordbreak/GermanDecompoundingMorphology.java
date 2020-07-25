package querqy.lucene.contrib.rewrite.wordbreak;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import java.util.Collections;

/**
 * S. Langer. 1998. Zur Morphologie und Semantik von Nominalkomposita. Tagungsband der 4. Konferenz zur Verarbeitung
 *   natürlicher Sprache (KONVENS).
 */
public abstract class GermanDecompoundingMorphology {

    /**
     * We use the frequencies of the compounding strategies [Langer 1998] to create a 'prior weight' for each strategy.
     * The prior can be updated by df-based calculations to rank the de-compounded candidates.
     */

    // count of the most frequent strategy (no morphological change)
    final static float NORM_PRIOR = 22759f;

    final static float PRIOR_0 = 1f;

    final static float PRIOR_PLUS_E = 87f / NORM_PRIOR;
    final static float PRIOR_PLUS_N = 5307f / NORM_PRIOR;
    final static float PRIOR_PLUS_S = 9637f / NORM_PRIOR;
    final static float PRIOR_PLUS_UMLAUT_E = 73f / NORM_PRIOR;

    final static float PRIOR_PLUS_EN = 4316f / NORM_PRIOR;
    final static float PRIOR_PLUS_ER = 25f / NORM_PRIOR;
    final static float PRIOR_PLUS_ES = 43f / NORM_PRIOR;

    final static float PRIOR_PLUS_UMLAUT_ER = 38f / NORM_PRIOR;

    final static float PRIOR_PLUS_NEN = 2610f / NORM_PRIOR;
    final static float PRIOR_PLUS_IEN = 19f / NORM_PRIOR;

    final static float PRIOR_MINUS_E = 122f / NORM_PRIOR;
    final static float PRIOR_MINUS_EN = 33f / NORM_PRIOR;

    // -us +en, -um +en, -a +en, -on +en, -on +a
    final static float PRIOR_MINUS_US_PLUS_EN = 618f / NORM_PRIOR;
    final static float PRIOR_MINUS_UM_PLUS_EN = 348f / NORM_PRIOR;
    final static float PRIOR_MINUS_A_PLUS_EN = 95f / NORM_PRIOR;
    final static float PRIOR_MINUS_ON_PLUS_EN = 59f / NORM_PRIOR;
    final static float PRIOR_MINUS_ON_PLUS_A = 28f / NORM_PRIOR;

    // -e +i
    final static float PRIOR_MINUS_E_PLUS_I = 11f / NORM_PRIOR;
    // -us +a
    final static float PRIOR_MINUS_UM_PLUS_A = 255f / NORM_PRIOR;

    static final WordGenerator GENERATOR_NULL = NullWordGenerator.INSTANCE;
    static final WordGenerator GENERATOR_A = new SuffixWordGenerator("a");
    static final WordGenerator GENERATOR_E = new SuffixWordGenerator("e");
    static final WordGenerator GENERATOR_EN = new SuffixWordGenerator("en");
    static final WordGenerator GENERATOR_ON = new SuffixWordGenerator("on");
    static final WordGenerator GENERATOR_US = new SuffixWordGenerator("us");
    static final WordGenerator GENERATOR_UM = new SuffixWordGenerator("um");
    static final WordGenerator GENERATOR_UMLAUT = new GermanUmlautWordGenerator();

    public static SuffixGroup createMorphemes(final float weightStrategy) {
        return new SuffixGroup(null,

                asList(
                        // 0
                        new WordGeneratorAndWeight(GENERATOR_NULL, (float) Math.pow(PRIOR_0, weightStrategy)),

                        // -e
                        new WordGeneratorAndWeight(GENERATOR_E, (float) Math.pow(PRIOR_MINUS_E, weightStrategy)),

                        // -en
                        new WordGeneratorAndWeight(GENERATOR_EN, (float) Math.pow(PRIOR_MINUS_EN, weightStrategy))

                ),

                new SuffixGroup("s",
                    singletonList(
                            // +s
                            new WordGeneratorAndWeight(GENERATOR_NULL, (float) Math.pow(PRIOR_PLUS_S, weightStrategy))
                    ),
                    new SuffixGroup("es",
                            singletonList(
                                // +es
                                new WordGeneratorAndWeight(GENERATOR_NULL,
                                        (float) Math.pow(PRIOR_PLUS_ES, weightStrategy))
                            )
                    )

                ),

                new SuffixGroup("n",
                        singletonList(
                                // +n
                                new WordGeneratorAndWeight(GENERATOR_NULL,
                                        (float) Math.pow(PRIOR_PLUS_N, weightStrategy))
                        ),
                        new SuffixGroup("en",
                                asList(
                                        // +en
                                        new WordGeneratorAndWeight(GENERATOR_NULL,
                                                (float) Math.pow(PRIOR_PLUS_EN, weightStrategy)),
                                        // -us +en
                                        new WordGeneratorAndWeight(GENERATOR_US,
                                                (float) Math.pow(PRIOR_MINUS_US_PLUS_EN, weightStrategy)),
                                        // -um +en
                                        new WordGeneratorAndWeight(GENERATOR_UM,
                                                (float) Math.pow(PRIOR_MINUS_UM_PLUS_EN, weightStrategy)),
                                        // -a +en
                                        new WordGeneratorAndWeight(GENERATOR_A,
                                                (float) Math.pow(PRIOR_MINUS_A_PLUS_EN, weightStrategy)),
                                        // -on +en
                                        new WordGeneratorAndWeight(GENERATOR_ON,
                                                (float) Math.pow(PRIOR_MINUS_ON_PLUS_EN, weightStrategy))
                                ),
                                new SuffixGroup("nen",
                                        singletonList(
                                                // +nen
                                                new WordGeneratorAndWeight(GENERATOR_NULL,
                                                        (float) Math.pow(PRIOR_PLUS_NEN, weightStrategy) )
                                        )
                                ),
                                new SuffixGroup("ien",
                                        singletonList(
                                                // +ien
                                                new WordGeneratorAndWeight(GENERATOR_NULL,
                                                        (float) Math.pow(PRIOR_PLUS_IEN, weightStrategy) )
                                        )
                                )
                        )

                ),
                new SuffixGroup("a",
                        asList(
                                // -um +a
                                new WordGeneratorAndWeight(GENERATOR_UM,
                                        (float) Math.pow(PRIOR_MINUS_UM_PLUS_A, weightStrategy)),
                                // -on +a
                                new WordGeneratorAndWeight(GENERATOR_ON,
                                        (float) Math.pow(PRIOR_MINUS_ON_PLUS_A, weightStrategy))
                        )

                ),
                new SuffixGroup("e",
                        asList(
                                // +e
                                new WordGeneratorAndWeight(GENERATOR_NULL,
                                        (float) Math.pow(PRIOR_PLUS_E, weightStrategy)),
                                // +" +e
                                new WordGeneratorAndWeight(GENERATOR_UMLAUT,
                                        (float) Math.pow(PRIOR_PLUS_UMLAUT_E, weightStrategy))
                        )

                ),
                new SuffixGroup("r",
                        Collections.emptyList(),
                        new SuffixGroup("er",
                                asList(
                                        // +er
                                        new WordGeneratorAndWeight(GENERATOR_NULL,
                                                (float) Math.pow(PRIOR_PLUS_ER, weightStrategy)),
                                        // +" +er
                                        new WordGeneratorAndWeight(GENERATOR_UMLAUT,
                                                (float) Math.pow(PRIOR_PLUS_UMLAUT_ER, weightStrategy))
                                )

                        )

                ),
                new SuffixGroup("i",
                        singletonList(
                                // -e +i
                                new WordGeneratorAndWeight(GENERATOR_E,
                                        (float) Math.pow(PRIOR_MINUS_E_PLUS_I, weightStrategy))
                        )

                )


        );
    }


}
