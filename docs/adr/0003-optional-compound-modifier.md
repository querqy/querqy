# ADR-0003: Optional compound modifier for the WordBreakCompoundRewriter

- **Status:** Accepted
- **Date:** 2026-07-11
- **Area:** Querqy `WordBreakCompoundRewriter` — `querqy.rewriter.wordbreak`
- **Relates to:** [ADR-0002](0002-dutch-decompounding-morphology.md) (Dutch decompounding morphology) — same rewriter, orthogonal concern.
- **GitHub issue:** [#1125 — Word Break Rewriter: add a way to make modifier optional](https://github.com/querqy/querqy/issues/1125)

## Context

For a query token like `gasgrill`, `WordBreakCompoundRewriter.decompound()` currently expands the token into an alternative that requires **both** decompounded parts to match: `+gas +grill`. A user searching for `gasgrill` (a gas grill) might reasonably also want to see other grills (charcoal, electric, ...), which only share the head term `grill` with the query, not the modifier `gas`. Issue #1125 asks for a configurable way to make the modifier optional — `gas +grill` — so the head alone is enough to match, while documents that also match the modifier can be scored higher.

### How the existing machinery is shaped (the constraint that drives this decision)

- `MorphologicalWordBreaker.breakWord()` returns a `List<CharSequence[]>`. Tracing it through `Collector.collect()`, every element is constructed as `new Suggestion(new CharSequence[]{left, right}, score)` — **always exactly two entries**: `[0]` is the modifier (the part morphology strips a linker from / reconstructs), `[1]` is the head (matched verbatim against the dictionary). Nothing in `Morphology`/`SuffixGroupMorphology` recurses to produce more than one split point, so this shape is a real invariant of the current implementation, not just today's test data.
- Both morphologies implemented so far — `GermanDecompoundingMorphology` and `DutchDecompoundingMorphology` — are right-headed (see ADR-0002 §"Context"), so the head is always the *last* array element and the modifier always the *first*. This matches the issue's own framing ("for Germanic languages, it would be the first part").
- `WordBreakCompoundRewriter.decompound()` builds, per split candidate, a `BooleanQuery` (SHOULD, generated — an alternative to the original token) containing one `DisjunctionMaxQuery` per part, each wrapping a `Term` with `Occur.MUST`. Making a part "optional" means giving its `DisjunctionMaxQuery` `Occur.SHOULD` instead.
- `BoostedTerm` (a `Term` subclass carrying a `float boost`) already exists and is exactly the "generated term with a custom weight" mechanism `SynonymInstruction` uses (`createTermFrom`: use `BoostedTerm` only if `boost != 1f`, otherwise a plain `Term`). Reusing it here needs no new query-model class and is already handled by `LuceneQueryBuilder`.

## Decision

### 1. New enum `OptionalModifierPosition { NONE, FIRST, LAST }`

Lives in `querqy.rewriter.wordbreak`. `NONE` is the default and preserves today's behavior (both parts mandatory). `FIRST`/`LAST` say which index of the (always 2-element, but the code does not hard-assume this) decompounded array becomes optional:

- `FIRST` — covers the Germanic case from the issue (`gas` optional, `grill` mandatory).
- `LAST` — included even though no shipped morphology is left-headed today, because the issue explicitly asks for "first or last part", not just the Germanic case. This is genuine forward-compatibility for a future left-headed morphology, not speculative scope creep — it costs one extra enum value and one extra branch.

Rejected alternative: a plain `boolean makeModifierOptional` covering only `FIRST`. Simpler, but would need a breaking API change the day a left-headed language is added, to encode something the issue already asked for in words.

### 2. New `OptionalModifierConfig` record bundling position + boost, validated on construction

```java
public record OptionalModifierConfig(OptionalModifierPosition position, float boost) {
    public OptionalModifierConfig {
        if (position == OptionalModifierPosition.NONE && Float.compare(boost, 1f) != 0) {
            throw new IllegalArgumentException(
                "optionalModifierBoost must be 1.0 when optionalModifierPosition is NONE, got " + boost);
        }
    }

    public static final OptionalModifierConfig DISABLED = new OptionalModifierConfig(OptionalModifierPosition.NONE, 1f);
}
```

A bare `(OptionalModifierPosition, float)` pair of constructor params would allow an invalid combination — `NONE` with a non-neutral boost — to reach the rewriter. Bundling both into one record with a compact canonical constructor makes that combination unconstructable rather than something each caller (or the rewriter itself) has to remember to check. This is the same record style already used in this package (`WordBreak.java`).

Default boost is `1.0f` (neutral): enabling the feature does not by itself change scoring beyond "this clause is now optional instead of required" — a document matching both parts still scores higher than one matching only the head, simply because one more clause contributes. Bumping the boost is opt-in, for deployments that want the both-parts-match case to stand out more strongly. This mirrors `SynonymInstruction`'s existing `boost == 1f` ⇒ plain `Term`, else `BoostedTerm` convention exactly.

Rejected alternative for the default: a higher default (e.g. `2.0f`). Rejected because it would make enabling the feature silently change scores by a magic factor with no explicit user choice — inconsistent with every other boost-bearing construct in the codebase, which defaults to neutral.

### 3. Threading through the constructors

- `WordBreakCompoundRewriter`: one new trailing constructor param, `OptionalModifierConfig optionalModifierConfig`. The previous constructor signature is kept as an overload delegating with `OptionalModifierConfig.DISABLED`, so existing callers keep compiling.
- `WordBreakCompoundRewriterFactory`: two new trailing constructor params, `String optionalModifierPosition, float optionalModifierBoost` — consistent with how `decompoundMorphologyName`/`compoundMorphologyName` are already passed as strings and parsed inside the factory. The factory parses the string (case-insensitive, `null`/empty ⇒ `NONE`, unknown value ⇒ `IllegalArgumentException`) and constructs the `OptionalModifierConfig` internally — so an invalid combination coming from YAML/JSON config surfaces as an `IllegalArgumentException` at factory-construction time, before anything reaches the rewriter. This is the same shape as `buildWordLookup`, which already turns primitive `List<String>` config into a richer `TrieMap<Boolean>` inside the factory. The previous factory signature is likewise kept as a delegating overload with `("NONE", 1.0f)`.

This follows the existing precedent of `MorphologicalWordBreaker`, which has both a legacy constructor and a fuller one taking `weightMorphologicalPattern`.

### 4. Scope: decompounding only

`compound()` (combining two adjacent query tokens into a single compound term, and the reverse-compound-trigger path) is untouched. The issue is specifically about splitting a compound query token and softening the modifier requirement; compounding is the opposite direction and has no equivalent "modifier" to make optional in the same sense.

## Consequences

### Shipped in this change

- `OptionalModifierPosition.java` — the new enum.
- `OptionalModifierConfig.java` — the validating record described above.
- `WordBreakCompoundRewriter.decompound()` — builds the per-part `Occur` and, for the optional part, a plain `Term` or `BoostedTerm` depending on `optionalModifierConfig.boost()`.
- `WordBreakCompoundRewriter`/`WordBreakCompoundRewriterFactory` — new trailing constructor param(s) as described above, with backward-compatible overloads.
- Tests covering: `NONE`/`DISABLED` (unchanged behavior), `FIRST` and `LAST` on a 2-element split, boost `1.0f` (plain `Term`) vs. a custom boost (`BoostedTerm`), the `NONE` + non-neutral-boost combination being rejected by `OptionalModifierConfig`, and that `compound()`/reverse-compounds are unaffected.

### Trade-offs / known limitations

- The enum encodes *position in the split array*, not "modifier" vs. "head" as a first-class concept — deliberately, since headedness is a property of the morphology (documented per-language in prose, e.g. ADR-0002), not something the rewriter tracks per candidate. A user configuring `LAST` against a right-headed morphology would make the *head* optional and the modifier mandatory, which is a legitimate (if unusual) configuration, not a bug — the rewriter has no way to know which morphology is "in charge" of headedness beyond what the operator configures.
- If a future morphology ever produces splits with more than two parts, `FIRST`/`LAST` still resolve unambiguously (index `0` / index `length - 1`), but there is no way to make an interior part optional. Not addressed here since no such morphology exists yet.

## References

- [ADR-0002 — Dutch decompounding morphology](0002-dutch-decompounding-morphology.md)
- `SynonymInstruction.createTermFrom` (`querqy-core/src/main/java/querqy/rewriter/commonrules/model/SynonymInstruction.java`) — precedent for the `boost == 1f` ⇒ plain `Term` else `BoostedTerm` convention.
