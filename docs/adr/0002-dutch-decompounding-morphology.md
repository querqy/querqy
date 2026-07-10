# ADR-0002: Dutch decompounding morphology for the WordBreakCompoundRewriter

- **Status:** Accepted
- **Date:** 2026-06-28
- **Area:** Querqy `WordBreakCompoundRewriter` — `querqy.rewriter.wordbreak`
- **Supersedes / relates to:** the existing `GERMAN` morphology (Langer 1998) and the `DEFAULT` (zero-linker) morphology.
- **GitHub issue:** [#1045 — Word Break Rewriter: Add rules for Dutch (De)Compounding](https://github.com/querqy/querqy/issues/1045)

## Context

`WordBreakCompoundRewriter` decompounds a query token by trying every split position, applying a set of morphological patterns to the left part, and keeping a split only if both parts resolve against a dictionary (`TermCorpus`). Two morphologies exist today:

- **`DEFAULT`** — assumes bare juxtaposition (zero linker).
- **`GERMAN`** — the linking-element patterns and frequency priors from S. Langer, *Zur Morphologie und Semantik von Nominalkomposita* (KONVENS 1998).

We want the same quality of decompounding for **Dutch**, which is structurally close to German (right-headed, highly productive N+N compounding) but has a much smaller linker inventory and a different set of orthographic side effects.

### How the existing machinery is shaped (the constraint that drives this decision)

Adding a language is **data, not control flow**:

- `Morphology` is an interface with two methods: `suggestCompounds(left, right)` and `suggestWordBreaks(word, minBreakLength)`.
- `SuffixGroupMorphology` is a generic implementation, parameterised by two factory functions that each return a `SuffixGroup` tree (one for decompounding, one for compounding).
- A `SuffixGroup` is a trie node carrying: a suffix to strip from the left part, a list of `WordGenerator`s that reconstruct candidate base forms from the stripped remainder, and nested `next` groups for shared-suffix lookup.
- A `WordGenerator.generateModifier(reduced)` receives the left part **with the linker already stripped** and returns the dictionary lookup form (or `Optional.empty()` if inapplicable). Multiple generators can sit in one group and each emits one candidate — this is how German runs umlaut-reversal next to no-op.
- Languages are registered by key in `MorphologyProvider` (`"default"`, `"german"`, …).

So "add Dutch" = a `DutchDecompoundingMorphology` (a decompounding + a compounding `SuffixGroup` factory), a small number of Dutch-specific `WordGenerator`s, and one registration line.

## Decision

### 1. Decompounding is **not** linker prediction

The well-known Dutch literature on linking elements (Krott, Baayen & Schreuder and successors) models *which* linker a novel compound should take — a **generation** problem solved with analogical "constituent families". We deliberately **do not** use that here. Because every candidate split is validated against `TermCorpus`, decompounding only needs to *enumerate* the possible linkers and let the dictionary filter. The analogical model is relevant only to the compounding direction and to setting priors, not to the splitter.

### 2. Linker inventory

Implement these linking elements as suffix groups: **∅, -s-, -e-, -en-, -er-**. Keep both `-e-` and `-en-`: the 1995/2005 spelling reform shifted the schwa linker toward `-en-`, but older indexed data and queries still contain `-e-` forms.

Structural nesting (mirrors German's `r → er`): `-en-` is nested under a generator-less `n` group and `-er-` under a generator-less `r` group, so `SuffixGroup`'s shared-suffix early-exit applies. Bare `-n-` and `-r-` are **not** Dutch linkers and carry `Collections.emptyList()`.

The `-er-` linker covers the archaic genitive/plural examples from issue #1045 — `kinderarts` (`kind` + `-er-` + `arts`) and `rundergehakt` (`rund` + `-er-` + `gehakt`) — both resolve with the plain `NOOP` generator under `-er-`: stripping `-er-` leaves `kind` and `rund` unchanged, so no orthographic alternation is needed.

### 3. Reverse the schwa-linker orthographic alternations as generators

The `-e-`/`-en-` linker triggers open-syllable spelling changes on the modifier that must be undone to recover the dictionary form:

| Alternation | Example compound | After stripping linker | Target base | Generator |
|---|---|---|---|---|
| Degemination | `pannenkoek` | `pann` | `pan` | `DutchDegeminationWordGenerator` |
| Vowel lengthening | `schapenvlees` | `schap` | `schaap` | `DutchVowelLengtheningWordGenerator` |
| Final devoicing | `duivenhok` | `duiv` | `duif` | `DutchDevoicingWordGenerator` |
| Lengthening + devoicing | `slavenhandel` (also `gravenstraat`) | `slav` | `slaaf` | `DutchLengtheningDevoicingWordGenerator` |

`DutchLengtheningDevoicingWordGenerator` delegates to the devoicing and lengthening generators in sequence (`flatMap`), so it only produces a candidate when a stem needs *both* alternations — a stem needing just one (`duiv → duif`, `schap → schaap`) is left to the corresponding single generator. `slavenhandel` (slave trade) is common, everyday vocabulary and was the deciding evidence for implementing this rather than leaving it as a "TODO if data warrants" case; `gravenstraat` — the initially considered example — leans more toward a place name than a productive common-noun compound.

Each is an independent `WordGenerator` returning a single candidate; they sit alongside `NOOP` under the `-e-` and `-en-` groups. Over-generation (e.g. lengthening `kat → kaat`) is acceptable because `TermCorpus` rejects non-words — the dictionary is the arbiter.

**Degemination rule:** if the stripped remainder ends in a doubled *doublable* consonant (`b d f g k l m n p r s t`), drop the duplicate. `v`/`z` are excluded because they never surface as written geminates in Dutch (they appear as `f`/`s` and are the devoicing generator's job). Returns `Optional.empty()` otherwise, so it never duplicates the `NOOP` candidate. This is exactly the pattern issue #1045 asked for: `paddenpoel` (`pad` + `-en-` + `poel`, strip `-en-` → `padd` → degeminate → `pad`) and `zonnebril` (`zon` + `-e-` + `bril`, strip `-e-` → `zonn` → degeminate → `zon`).

### 4. Priors are calibrated from CompoundPiece

We do not have CELEX access. Instead, `PRIOR_*` was calibrated from the CompoundPiece dataset (Minixhofer, Pfeiffer & Vulić, EMNLP 2023 Findings) — specifically the Dutch subset of its `wiktionary` split, which gives, per real compound, both `norm` (the dictionary lemma of each part) and `segmentation` (the part's actual compound-internal spelling). That pairing lets us classify each entry directly: run the modifier lemma through the real compounding-direction `WordGenerator`s and check which one's output exactly matches the gold surface form — no dictionary lookup or `TermCorpus` involved, so no over-generation ambiguity in the classification step itself. Counts were combined across `train.csv` and `valid.csv` (checked first for distributional consistency: the zero-linker share was 83.4% vs. 83.9%, `-s-` 8.8% vs. 8.6%, `-en-` 3.8% vs. 3.7%, matching within each pattern) to get a larger, more stable sample: 12,692 classified 2-part compounds in total, zero ambiguous (no entry matched more than one generator).

| Linker | Count | `PRIOR_*` |
|---|---|---|
| ∅ (zero linker) | 10,591 | `PRIOR_0 = 1f` (defines `NORM_PRIOR`) |
| `-s-` | 1,114 | `PRIOR_PLUS_S = 1114/10591 ≈ 0.1052` |
| `-e-` (all alternations combined) | 123 | `PRIOR_PLUS_E = 123/10591 ≈ 0.0116` |
| `-en-` (all alternations combined) | 800 | `PRIOR_PLUS_EN = 800/10591 ≈ 0.0755` |
| `-er-` | 64 | `PRIOR_PLUS_ER = 64/10591 ≈ 0.0060` |

Counts for `-e-`/`-en-` sum across every alternation within that linker (plain, degeminated, shortened, voiced, combined), matching the granularity our `SuffixGroup` tree actually uses (one shared weight per linker node, not per alternation). Because candidates are still dictionary-validated in production, calibration only affects suggestion **order**, not correctness — but the real distribution is much more skewed toward the zero linker than the original placeholder guesses assumed.

The ~14% of entries with no matching generator were excluded rather than force-fit, and this is *not* a gap in the generators. Almost all of them are deverbal nouns, e.g. `roven → roof`: CompoundPiece's `norm` traces the noun back to the verb infinitive it derives from via ablaut (`roven` "to rob" → `roof` "plunder"; the same pattern as English `to bear` → `birth`), which is derivation, not linking — no generator here is *supposed* to produce it. Our morphology only ever needs the noun `roof` itself in `TermCorpus`, never a relationship back to `roven`: decompounding `roofbouw` still resolves correctly at the split `roof`|`bouw` via the plain zero-linker `NOOP` candidate (verified directly against the code), regardless of `roof`'s own etymology. The remaining unmatched entries are a handful of irregular stems (`schip → scheeps`) and a few dataset hyphenation artifacts.

## Consequences

### Shipped in this change

- `DutchDecompoundingMorphology.java` — decompounding tree (∅, -s-, -e-, -en-, -er-) with `NOOP` and all four orthographic generators wired under the schwa groups; compounding tree with gemination, vowel-shortening and voicing alternatives alongside plain `+e`/`+en`.
- `DutchDegeminationWordGenerator.java`, `DutchVowelLengtheningWordGenerator.java`, `DutchDevoicingWordGenerator.java`, `DutchLengtheningDevoicingWordGenerator.java` — all four alternation reversals from the table above (decompounding direction).
- `DutchGeminationCompoundingWordGenerator.java`, `DutchVowelShorteningCompoundingWordGenerator.java`, `DutchVoicingCompoundingWordGenerator.java`, `DutchShorteningVoicingCompoundingWordGenerator.java` — the compounding-direction inverses of degemination, lengthening, devoicing, and the combined case, analogous to `GermanUmlautCompoundingWordGenerator`. Each sits alongside the plain `SuffixWordGenerator`, not replacing it, since `TermCorpus` disambiguates the same way it does on the decompounding side (confirmed: `MorphologicalCompounder.combine` filters compounding suggestions by `termCorpus.docFreq` too). The voicing generator's alternation is lexically arbitrary (not predictable from spelling, e.g. `duif → duiven` but `vis → vissen`), so it always fires for `f`/`s`-final modifiers and relies entirely on `TermCorpus` to discard the wrong hypothesis — same tolerance as `kat → kaat` on the decompounding side. The combined generator composes the shortening and voicing generators (`flatMap`, shortening first with an empty intermediate suffix, then voicing appends the real suffix) the same way `DutchLengtheningDevoicingWordGenerator` composes its decompounding-direction counterparts, so it only fires when a stem needs both alternations (`slaaf → slaven`, `graaf → graven`).
- `DutchMorphologyDecompoundingTableTest.java` — parameterised table test (13 rows: ∅, -s-, plain -en-, each of the four alternations under -e- and/or -en-, -er- ×2), in the style of `GermanMorphologyDecompoundingTableTest`. Tests candidate generation only; no `TermCorpus` is consulted.
- `DutchMorphologyCompoundingTableTest.java` — parameterised table test (12 rows) for the compounding direction, in the style of `GermanMorphologyCompoundingTableTest`: gemination (`paddenpoel`, `zonnebril`, `kippensoep`), vowel shortening (`schapenvlees`, `pereboom`), voicing (`duivenhok`, `huizenblok`), combined shortening+voicing (`slavenhandel`, `gravenstraat`), plus cases where gemination must not misfire (zero linker, digraph guard, already-long vowel).
- Registered `"dutch"` in `MorphologyProvider`, alongside `"default"` and `"german"`.

### Required follow-ups before production use

1. ~~Register the language in `MorphologyProvider`.~~ Done.
2. ~~Calibrate priors~~ Done — see Decision §4.
3. ~~Implement the remaining generators (vowel lengthening, devoicing, combined case).~~ Done.
4. ~~Compounding direction: geminating, vowel-shortening, voicing, and combined shortening+voicing compounding generators.~~ Done.
5. ~~Evaluate against a held-out gold set.~~ Done for both directions — full pipeline this time (real `MorphologicalWordBreaker`/`MorphologicalCompounder` backed by a real dictionary, not just generator classification as in Decision §4). Dictionary: the OpenTaal wordlist (~414k Dutch word forms, BSD/CC BY 3.0) merged with hermitdave/FrequencyWords Dutch frequencies (`docFreq = count + 1`, add-1 smoothed, matching `Collector.java`'s existing technique for missing terms) via a `TsvDfTermCorpus`. Since neither source has document-level co-occurrence information, `TsvDfTermCorpus.isCollationSupported()` is `false` and both runs used `verifyCollation=false`.

   **Decompounding** (splitting a compound query token into dictionary-resolvable parts), 14,786 2-part positive Dutch entries, train+valid combined: 70.6% top-1 / 81.7% any-rank recall overall. "In scope" is defined the same way as the classification in Decision §4: a generator must reproduce the gold `segmentation` from the gold `norm` exactly; entries where none does (14.2% of the set) require a transformation this morphology was never designed to model (derivation, a different word class, ...; see misses below) rather than a failure of the implementation. Restricted to the 85.8% that are in scope: **82.3% top-1 / 94.3% any-rank recall** — the fair measure of the morphology's own performance.

   **Compounding** (combining two base words into a suggested compound, e.g. the reverse-compound trigger path), same entries, dictionary, and in-scope definition: 67.4% top-1 / 68.4% any-rank recall overall; restricted to in-scope entries: 77.9% top-1 / 79.0% any-rank recall. Lower than the decompounding numbers mainly because `MorphologicalCompounder` additionally requires the *resulting compound* to pass `minSuggestionFrequency` in the dictionary — only 82.8% of gold compounds are present there at all — on top of needing the same in-scope linker/alternation match.

   Misses in both directions are, on inspection, overwhelmingly the same out-of-scope categories already documented (deverbal nouns — confirmed on the head side too, e.g. `schatzoeker` → gold head lemma `zoeken` vs. actual indexed noun `zoeker`; different word class entirely, e.g. `grootschalig` is an adjective, not an N+N noun compound; double suffixation, e.g. `woensdagsmorgens` stacks a linker `-s-` with a second, adverbial `-s`).

   One entry, `witlof` (chicory), is worth flagging separately rather than folding into those categories. The decompounding run initially looked like a lexical homonym collision — gold wants `wit|loof` (vowel-lengthening reversal), but `wit|lof` ranks first since `lof` ("praise") is itself a common, unrelated dictionary word. But the compounding run surfaces the same entry from the other side: CompoundPiece's gold treats the *head*'s lemma as `loof`, i.e. alternating to `lof` in the compound — which contradicts this model's core assumption (and standard Dutch morphology) that only the modifier alternates, never the head. That makes it more likely CompoundPiece's `norm` annotation for this specific entry is itself an artifact of automated lemmatization (folk-etymologically relating `lof` to the unrelated `loof` by surface similarity) than a real linguistic phenomenon — in which case our system's `wit`+`lof` answer isn't wrong, the gold label is questionable. Treat this one as inconclusive rather than as confirmed evidence that `verifyCollation` would fix it.

### Trade-offs / known limitations

- **Derivation is out of scope on both constituents — the morphology reverses *linking* only, never derivation.** In particular, the diminutive (`-je`/`-jes` and allomorphs `-tje`, `-etje`, `-pje`, `-kje`) forms a new lexeme and is not a linking element, so it gets no suffix group. This is deliberate on each side:
  - *Modifier:* a diminutive modifier already surfaces correctly via the `-s` linker (`koekjesfabriek` → strip `-s` → `koekje`), provided the diminutive lexeme is in `TermCorpus`. Adding a `-jes` rule would overshoot to the bare base (`koekje → koek`), discarding meaning and conflating distinct terms (`koekje ≠ koek`, `meisje ≠ meid`).
  - *Head:* the head is matched verbatim, never reduced, so `tafellampje` → `tafel | lampje` preserves the diminutive for free. Because Dutch is right-headed, a head diminutive scopes over the whole compound (`tafellampje` = diminutive of `tafellamp`), so reducing it to `lamp` would strip the compound's own meaning.
  - If diminutive→base bridging is ever wanted for recall (e.g. `tafellampje` queries reaching `tafellamp` documents), it is a separate, lossy normalization step (a diminutive-aware stemmer or synonym/expansion rule), kept out of the decompounding morphology. The corresponding corpus-side fix for a missing diminutive lexeme is to index it, not to add a stripping rule.
  - *Deverbal nouns:* the same principle applies to nouns formed from a verb via ablaut (e.g. `roven` "to rob" → `roof` "plunder", surfaced during prior calibration — see Decision §4). `roof` is matched as its own dictionary entry; nothing needs to relate it back to `roven`.
- Irregular stem alternations (e.g. `schip → scheeps-`, learned Latin/Greek plurals) are out of scope; they will simply fail to split unless added explicitly.

## References

- S. Langer. *Zur Morphologie und Semantik von Nominalkomposita.* KONVENS 1998. (Basis of the existing `GERMAN` morphology and the priors approach.)
- G. Booij. *The Morphology of Dutch.* Oxford University Press. (Linker inventory and the spelling alternations to reverse.)
- G. Booij. *Compounding in Dutch.* 1992. (Linked directly from issue #1045 as a starting point; earlier and narrower than the OUP monograph above, same author.)
- Taalportaal, "Linking elements" topic pages — taalportaal.org. (Descriptive reference.)
- CELEX Dutch lexical database (DMW/DML, via LDC). (Would have been the preferred prior-calibration source; not accessible to us — see Decision §4 for the CompoundPiece-based alternative actually used.)
- B. Minixhofer, J. Pfeiffer & I. Vulić. *CompoundPiece: Evaluating and Improving Decompounding Performance of Language Models.* EMNLP 2023 (Findings). (Multilingual decompounding dataset incl. Dutch; used for prior calibration in Decision §4, and still the natural gold set for the full-pipeline evaluation in Consequences item 5.)
- OpenTaal Dutch Hunspell `nl.aff`/`nl.dic`. (Affix rules encoding degemination / vowel-doubling / voicing — directly mirrors what the generators must do.)
- A. Krott, R. H. Baayen & R. Schreuder. *Analogy in morphology: modeling the choice of linking morphemes in Dutch.* Linguistics 39 (2001). (Linker *prediction*; relevant to compounding and priors, not to decompounding — see Decision §1.)
