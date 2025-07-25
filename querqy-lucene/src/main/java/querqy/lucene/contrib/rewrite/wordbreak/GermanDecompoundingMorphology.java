package querqy.lucene.contrib.rewrite.wordbreak;

import java.util.Collections;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

/**
 * <p>Morphological compounding patterns for German.</p>
 * <p>The patterns where taken from: S. Langer. 1998. Zur Morphologie und Semantik von Nominalkomposita. Tagungsband
 * der 4. Konferenz zur Verarbeitung natürlicher Sprache (KONVENS).</p>
 *
 * <p>Also see {@link Collector} and {@link SuffixGroup}</p>
 *
 * @author renekrie
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

    static final WordGenerator GENERATOR_NOOP = NoopWordGenerator.INSTANCE;
    static final WordGenerator GENERATOR_A = new SuffixWordGenerator("a");
    static final WordGenerator GENERATOR_E = new SuffixWordGenerator("e");
    static final WordGenerator GENERATOR_EN = new SuffixWordGenerator("en");
    static final WordGenerator GENERATOR_ON = new SuffixWordGenerator("on");
    static final WordGenerator GENERATOR_US = new SuffixWordGenerator("us");
    static final WordGenerator GENERATOR_UM = new SuffixWordGenerator("um");
    static final WordGenerator GENERATOR_S = new SuffixWordGenerator("s");
    static final WordGenerator GENERATOR_N = new SuffixWordGenerator("n");
    static final WordGenerator GENERATOR_NEN = new SuffixWordGenerator("nen");
    static final WordGenerator GENERATOR_ES = new SuffixWordGenerator("es");
    static final WordGenerator GENERATOR_ER = new SuffixWordGenerator("er");
    static final WordGenerator GENERATOR_IEN = new SuffixWordGenerator("ien");
    static final WordGenerator GENERATOR_I = new SuffixWordGenerator("i");
    static final WordGenerator GENERATOR_UMLAUT = new GermanUmlautWordGenerator();
    static final WordGenerator GENERATOR_COMPOUND_UMLAUT_PLUS_E = new GermanUmlautCompoundingWordGenerator("e");
    static final WordGenerator GENERATOR_COMPOUND_UMLAUT_PLUS_ER = new GermanUmlautCompoundingWordGenerator("er");

    public static SuffixGroup createCompoundingMorphemes(final float weight) {
        return new SuffixGroup(null,

                asList(
                        // 0
                        new WordGeneratorAndWeight(GENERATOR_NOOP, (float) Math.pow(PRIOR_0,
                                weight)),
                        // +s
                        new WordGeneratorAndWeight(GENERATOR_S, (float) Math.pow(PRIOR_PLUS_S,
                                weight)),
                        // +n
                        new WordGeneratorAndWeight(GENERATOR_N, (float) Math.pow(PRIOR_PLUS_N,
                                weight)),
                        // +e
                        new WordGeneratorAndWeight(GENERATOR_E, (float) Math.pow(PRIOR_PLUS_E,
                                weight)),
                        // +en
                        new WordGeneratorAndWeight(GENERATOR_EN, (float) Math.pow(PRIOR_PLUS_EN,
                                weight)),
                        // +nen
                        new WordGeneratorAndWeight(GENERATOR_NEN, (float) Math.pow(PRIOR_PLUS_NEN,
                                weight)),
                        // +es
                        new WordGeneratorAndWeight(GENERATOR_ES, (float) Math.pow(PRIOR_PLUS_ES,
                                weight)),
                        // +er
                        new WordGeneratorAndWeight(GENERATOR_ER, (float) Math.pow(PRIOR_PLUS_ER,
                                weight)),
                        // +ien
                        new WordGeneratorAndWeight(GENERATOR_IEN, (float) Math.pow(PRIOR_PLUS_IEN,
                                weight)),

                        // " (umlaut) +e
                        new WordGeneratorAndWeight(GENERATOR_COMPOUND_UMLAUT_PLUS_E, (float) Math.pow(PRIOR_PLUS_UMLAUT_E, weight)),

                        // " (umlaut) +er
                        new WordGeneratorAndWeight(GENERATOR_COMPOUND_UMLAUT_PLUS_ER, (float) Math.pow(PRIOR_PLUS_UMLAUT_ER, weight))

                ),
                // -us
                new SuffixGroup("us", singletonList(
                        // +en
                        new WordGeneratorAndWeight(GENERATOR_EN, (float) Math.pow(PRIOR_MINUS_US_PLUS_EN, weight))
                )),
                // -um
                new SuffixGroup("um", asList(
                        // +en
                        new WordGeneratorAndWeight(GENERATOR_EN, (float) Math.pow(PRIOR_MINUS_UM_PLUS_EN, weight)),
                        // +a
                        new WordGeneratorAndWeight(GENERATOR_A, (float) Math.pow(PRIOR_MINUS_UM_PLUS_A, weight))
                )),

                //-e
                new SuffixGroup("e", asList(
                        new WordGeneratorAndWeight(NoopWordGenerator.INSTANCE,
                                (float) Math.pow(PRIOR_MINUS_E, weight)),
                        //+i
                        new WordGeneratorAndWeight(GENERATOR_I, (float) Math.pow(PRIOR_MINUS_E_PLUS_I, weight))
                )),
                //-en
                new SuffixGroup("en", asList(
                        new WordGeneratorAndWeight(NoopWordGenerator.INSTANCE,
                                (float) Math.pow(PRIOR_MINUS_EN, weight)),
                        //+i
                        new WordGeneratorAndWeight(GENERATOR_I, (float) Math.pow(PRIOR_MINUS_E_PLUS_I, weight))
                )),
                //-a
                new SuffixGroup("a", singletonList(
                        //+en
                        new WordGeneratorAndWeight(GENERATOR_EN, (float) Math.pow(PRIOR_MINUS_A_PLUS_EN, weight))
                )),
                //-on
                new SuffixGroup("on", asList(
                        //+en
                        new WordGeneratorAndWeight(GENERATOR_EN, (float) Math.pow(PRIOR_MINUS_ON_PLUS_EN, weight)),
                        //+a
                        new WordGeneratorAndWeight(GENERATOR_A, (float) Math.pow(PRIOR_MINUS_ON_PLUS_A, weight))
                ))
        );
    }

    public static SuffixGroup createDecompoundingMorphemes(final float weightMorphologicalPattern) {
        return new SuffixGroup(null, // suffix to strip from the term, when null then not suffix removed

                asList(
                        // 0
                        new WordGeneratorAndWeight(GENERATOR_NOOP, (float) Math.pow(PRIOR_0,
                                weightMorphologicalPattern)),

                        // -e
                        new WordGeneratorAndWeight(GENERATOR_E, (float) Math.pow(PRIOR_MINUS_E,
                                weightMorphologicalPattern)),

                        // -en
                        new WordGeneratorAndWeight(GENERATOR_EN, (float) Math.pow(PRIOR_MINUS_EN,
                                weightMorphologicalPattern)) // append suffix, and assign priority score

                ), // list of generators

                new SuffixGroup("s",
                        singletonList(
                                // +s
                                new WordGeneratorAndWeight(GENERATOR_NOOP, (float) Math.pow(PRIOR_PLUS_S,
                                        weightMorphologicalPattern))
                        ),
                        new SuffixGroup("es",
                                singletonList(
                                        // +es
                                        new WordGeneratorAndWeight(GENERATOR_NOOP,
                                                (float) Math.pow(PRIOR_PLUS_ES, weightMorphologicalPattern))
                                )
                        )

                ),

                new SuffixGroup("n",
                        singletonList(
                                // +n
                                new WordGeneratorAndWeight(GENERATOR_NOOP,
                                        (float) Math.pow(PRIOR_PLUS_N, weightMorphologicalPattern))
                        ),
                        new SuffixGroup("en",
                                asList(
                                        // +en
                                        new WordGeneratorAndWeight(GENERATOR_NOOP,
                                                (float) Math.pow(PRIOR_PLUS_EN, weightMorphologicalPattern)),
                                        // -us +en
                                        new WordGeneratorAndWeight(GENERATOR_US,
                                                (float) Math.pow(PRIOR_MINUS_US_PLUS_EN, weightMorphologicalPattern)),
                                        // -um +en
                                        new WordGeneratorAndWeight(GENERATOR_UM,
                                                (float) Math.pow(PRIOR_MINUS_UM_PLUS_EN, weightMorphologicalPattern)),
                                        // -a +en
                                        new WordGeneratorAndWeight(GENERATOR_A,
                                                (float) Math.pow(PRIOR_MINUS_A_PLUS_EN, weightMorphologicalPattern)),
                                        // -on +en
                                        new WordGeneratorAndWeight(GENERATOR_ON,
                                                (float) Math.pow(PRIOR_MINUS_ON_PLUS_EN, weightMorphologicalPattern))
                                ),
                                new SuffixGroup("nen",
                                        singletonList(
                                                // +nen
                                                new WordGeneratorAndWeight(GENERATOR_NOOP,
                                                        (float) Math.pow(PRIOR_PLUS_NEN, weightMorphologicalPattern))
                                        )
                                ),
                                new SuffixGroup("ien",
                                        singletonList(
                                                // +ien
                                                new WordGeneratorAndWeight(GENERATOR_NOOP,
                                                        (float) Math.pow(PRIOR_PLUS_IEN, weightMorphologicalPattern))
                                        )
                                )
                        )

                ),
                new SuffixGroup("a",
                        asList(
                                // -um +a
                                new WordGeneratorAndWeight(GENERATOR_UM,
                                        (float) Math.pow(PRIOR_MINUS_UM_PLUS_A, weightMorphologicalPattern)),
                                // -on +a
                                new WordGeneratorAndWeight(GENERATOR_ON,
                                        (float) Math.pow(PRIOR_MINUS_ON_PLUS_A, weightMorphologicalPattern))
                        )

                ),
                new SuffixGroup("e",
                        asList(
                                // +e
                                new WordGeneratorAndWeight(GENERATOR_NOOP,
                                        (float) Math.pow(PRIOR_PLUS_E, weightMorphologicalPattern)),
                                // +" +e
                                new WordGeneratorAndWeight(GENERATOR_UMLAUT,
                                        (float) Math.pow(PRIOR_PLUS_UMLAUT_E, weightMorphologicalPattern))
                        )

                ),
                new SuffixGroup("r",
                        Collections.emptyList(),
                        new SuffixGroup("er",
                                asList(
                                        // +er
                                        new WordGeneratorAndWeight(GENERATOR_NOOP,
                                                (float) Math.pow(PRIOR_PLUS_ER, weightMorphologicalPattern)),
                                        // +" +er
                                        new WordGeneratorAndWeight(GENERATOR_UMLAUT,
                                                (float) Math.pow(PRIOR_PLUS_UMLAUT_ER, weightMorphologicalPattern))
                                )

                        )

                ),
                new SuffixGroup("i",
                        singletonList(
                                // -e +i
                                new WordGeneratorAndWeight(GENERATOR_E,
                                        (float) Math.pow(PRIOR_MINUS_E_PLUS_I, weightMorphologicalPattern))
                        )

                )


        );
    }


}
