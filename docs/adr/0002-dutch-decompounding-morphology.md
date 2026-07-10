# ADR-0002: Dutch decompounding morphology for the WordBreakCompoundRewriter

- **Status:** Proposed
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

### 4. Priors are placeholders

The `PRIOR_*` constants currently encode only a rough ordering of linker frequency. They must be calibrated against Dutch data before the ranking can be trusted: compute `count(linker) / count(∅)` from CELEX Dutch (DMW/DML) or from our own index, exactly as the German class divides Langer's counts by the most frequent strategy (22759). Because candidates are dictionary-validated, miscalibrated priors only affect suggestion **order**, not correctness.

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
2. **Calibrate priors** from CELEX or our index (see Decision §4). The current `PRIOR_*` constants in `DutchDecompoundingMorphology` are placeholders — a rough ordering, not calibrated frequencies.
3. ~~Implement the remaining generators (vowel lengthening, devoicing, combined case).~~ Done.
4. ~~Compounding direction: geminating, vowel-shortening, voicing, and combined shortening+voicing compounding generators.~~ Done.
5. **Evaluate** against a held-out gold set — the CompoundPiece Dutch split set is the natural choice — via a larger parameterised harness, rather than only the hand-checked rows.

### Trade-offs / known limitations

- **Derivation is out of scope on both constituents — the morphology reverses *linking* only, never derivation.** In particular, the diminutive (`-je`/`-jes` and allomorphs `-tje`, `-etje`, `-pje`, `-kje`) forms a new lexeme and is not a linking element, so it gets no suffix group. This is deliberate on each side:
  - *Modifier:* a diminutive modifier already surfaces correctly via the `-s` linker (`koekjesfabriek` → strip `-s` → `koekje`), provided the diminutive lexeme is in `TermCorpus`. Adding a `-jes` rule would overshoot to the bare base (`koekje → koek`), discarding meaning and conflating distinct terms (`koekje ≠ koek`, `meisje ≠ meid`).
  - *Head:* the head is matched verbatim, never reduced, so `tafellampje` → `tafel | lampje` preserves the diminutive for free. Because Dutch is right-headed, a head diminutive scopes over the whole compound (`tafellampje` = diminutive of `tafellamp`), so reducing it to `lamp` would strip the compound's own meaning.
  - If diminutive→base bridging is ever wanted for recall (e.g. `tafellampje` queries reaching `tafellamp` documents), it is a separate, lossy normalization step (a diminutive-aware stemmer or synonym/expansion rule), kept out of the decompounding morphology. The corresponding corpus-side fix for a missing diminutive lexeme is to index it, not to add a stripping rule.
- Irregular stem alternations (e.g. `schip → scheeps-`, learned Latin/Greek plurals) are out of scope; they will simply fail to split unless added explicitly.
- Without calibrated priors, ambiguous tokens may rank plausible-but-wrong splits above the intended one even though all surface in the candidate list.

## References

- S. Langer. *Zur Morphologie und Semantik von Nominalkomposita.* KONVENS 1998. (Basis of the existing `GERMAN` morphology and the priors approach.)
- G. Booij. *The Morphology of Dutch.* Oxford University Press. (Linker inventory and the spelling alternations to reverse.)
- G. Booij. *Compounding in Dutch.* 1992. (Linked directly from issue #1045 as a starting point; earlier and narrower than the OUP monograph above, same author.)
- Taalportaal, "Linking elements" topic pages — taalportaal.org. (Descriptive reference.)
- CELEX Dutch lexical database (DMW/DML, via LDC). (Compound segmentations + frequencies for prior calibration and a gold set.)
- B. Minixhofer, J. Pfeiffer & I. Vulić. *CompoundPiece: Evaluating and Improving Decompounding Performance of Language Models.* EMNLP 2023 (Findings). (Multilingual decompounding dataset incl. Dutch; evaluation gold standard.)
- OpenTaal Dutch Hunspell `nl.aff`/`nl.dic`. (Affix rules encoding degemination / vowel-doubling / voicing — directly mirrors what the generators must do.)
- A. Krott, R. H. Baayen & R. Schreuder. *Analogy in morphology: modeling the choice of linking morphemes in Dutch.* Linguistics 39 (2001). (Linker *prediction*; relevant to compounding and priors, not to decompounding — see Decision §1.)
