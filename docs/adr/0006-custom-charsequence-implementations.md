# ADR-0006: Keep the custom CharSequence implementations; make `CompoundCharSequence.charAt()` cache-friendly

- **Status:** Accepted
- **Date:** 2026-07-15
- **Area:** Querqy core — `querqy.CompoundCharSequence` / `querqy.ComparableCharSequence` family
- **Relates to:** querqy/querqy#148, branch `issue148`

## Context

Issue #148 (2021) raised two open questions about querqy-core's custom `CharSequence`
implementations (`CompoundCharSequence`, `LowerCaseCharSequence`, `ComparableCharSequenceWrapper`,
etc., all implementing `ComparableCharSequence` with `equals`/`hashCode`/`compareTo`):

1. Are `equals`/`hashCode`/`compareTo` actually needed? `TrieMap` only consumes plain
   `CharSequence` and never calls them; `hashCode()` isn't cached the way `String`'s is, so calling
   it repeatedly is more expensive than it looks. Where (outside tests) are they actually used?
2. The other stated motivation is avoiding array copies when splitting/concatenating text
   (tokenization, prefixes, shingle-building). But the issue argued this should only be needed
   around building `querqy.model.Term`, and suggested moving that logic into `TermBuilder` instead
   of keeping it spread across standalone classes. A follow-up comment from `@JohannesDaniel` added
   that `CompoundCharSequence` carries real per-instance metadata overhead (3 `int` fields plus an
   `int[]` and a `CharSequence[]`) and, at the time, was "used only a very few times in query-time
   contexts" — concluding the classes could likely be removed with little difficulty.

We re-examined both questions against the *current* codebase rather than the 2021 snapshot, since
five years of feature work is enough for either conclusion to have gone stale.

**On question 1 (are `equals`/`hashCode` load-bearing?):** they are, in production code, not just
tests. Two rule-parsing paths deduplicate `CharSequence` values through a `HashSet<CharSequence>`:

- `DeleteInstruction.charSequencesToDelete` (`querqy-core/src/main/java/querqy/rewriter/commonrules/model/DeleteInstruction.java:40,92`)
- `ReplaceRewriterParser.checkForDuplicateInput` (`querqy-core/src/main/java/querqy/rewriter/replace/ReplaceRewriterParser.java:77,114-143`)

Both sets can hold a mix of plain `String`s and `CompoundCharSequence`s. A `HashSet` only catches
duplicates if unrelated concrete types that represent the same text compare equal and hash
identically — exactly what `CharSequenceUtil`-backed `equals`/`hashCode` provide across this whole
family. Without them, a rule using `String` in one place and a `CompoundCharSequence` for the same
logical value in another would silently evade the duplicate-input check. So the issue's premise
that `TrieMap` doesn't need these methods is correct, but the conclusion that they're otherwise
unused doesn't hold today. The "not cached like `String`" critique is still accurate — every
`hashCode()` call recomputes from scratch — but that is a separate, independent piece of work
(see Consequences) and out of scope for this ADR.

**On question 2 (is the array-copy-avoidance motivation still valid, and does it belong in
`TermBuilder`?):** usage has both grown and diversified well past "a very few times". We found
34 non-test construction sites for `CompoundCharSequence` across 22 files in `querqy-core` and
`querqy-lucene` today — word-break morphology generators, number/shingle concatenation, replace-rule
instructions, suffix-wildcard and TrieMap lookup keys, German noun normalization, and the two
dedup-key usages above. Tracing every one of them end-to-end, **none ever materializes a `String`**
(no `.toString()`/`String.valueOf()` on the resulting value anywhere downstream, in this repo's
main source): they terminate in `TrieMap` traversal, `HashSet` dedup, or — via `LuceneQueryBuilder`
— Lucene's tokenizer and `BytesRef` construction, all of which consume `CharSequence` char-by-char.
Several of the busiest call sites (the dedup sets above, the `TrieMap`/suffix-wildcard lookup keys,
`GermanNounNormalizer`'s intermediate candidates) have nothing to do with building a `Term` at all,
so relocating this logic into `TermBuilder` as the issue suggested would not match how these types
are actually used — it would leave the non-`Term` call sites needing the exact same machinery
somewhere else.

We benchmarked the actual trade-off (JMH isn't available in this offline environment, so this is a
hand-rolled `nanoTime` microbenchmark: warmup + 5 trials + an accumulator to block dead-code
elimination — directionally solid given how large the measured effects are, though not JMH-grade):

- **Construction** is genuinely ~3.3x cheaper for `CompoundCharSequence` than eager `String`
  concatenation (5.6–5.8 ns/op vs. 18–20 ns/op for two short parts) — the zero-copy-at-construction
  claim holds up.
- **Per-character access** (`charAt`), in isolation, was *more* expensive than a plain `String`: at
  2 segments (this codebase's most common case) about 2.4x slower; the gap widened with segment
  count under the original linear-scan `getPartsIndex()` (up to 7.9x slower at 32 segments). The
  class's own long-standing `// TODO: avoid calls to this.charAt(i) to make comparison faster` on
  `compareTo()` was an accurate, if informal, flag of this cost.
- In the **real end-to-end pattern** every call site actually uses — build a `CompoundCharSequence`
  and immediately feed it into a single `TrieMap.get()` — the picture flips: at 2–4 segments (the
  range this codebase produces), `TrieMap` traversal cost over a realistic dictionary dominates the
  per-character access overhead, so `CompoundCharSequence` came out 2–6% *faster* end-to-end than
  building a `String` first, purely on the strength of the cheaper construction.

So the original motivation is real but was incompletely characterized in 2021: it's not a
uniform win, it's construction-time savings traded against per-access cost, and it nets out
positive specifically because every real consumer here only ever walks these sequences
char-by-char and never needs a `String` back. That last part is a codebase discipline worth
naming explicitly, since it's the precondition the whole trade-off depends on.

## Decision

1. **Keep the custom `ComparableCharSequence` family** — do not remove it as the issue tentatively
   proposed. Usage has grown substantially since 2021, `equals`/`hashCode` have real non-test
   callers, and no downstream `.toString()` cost is being paid anywhere today.
2. **Do not relocate concatenation/splitting logic into `TermBuilder`.** Most current usage has
   nothing to do with building a `Term`; the boundary the issue proposed doesn't match current call
   sites.
3. **Fix the one concrete, measured inefficiency instead:** change `CompoundCharSequence.charAt()`'s
   segment resolution from an unconditional, from-scratch linear scan (`getPartsIndex()`, still used
   as-is by `subSequence()`) to a cache of the most recently resolved segment index
   (`lastPartsIndex`), re-validated — never blindly trusted — on every call via a short
   forward/backward walk from the cached position.
   - We compared this against binary search over `indexOffsets` before settling on it. Binary
     search only beats the original linear scan starting around 8 segments (roughly a wash at 4,
     ~22% faster at 16, ~49% faster at 32); at the 2–4 segments this codebase actually produces, it's
     a wash or slightly worse than doing nothing, since its fixed per-step overhead outweighs
     searching a 2–4 element array.
   - The cache wins at every segment count ≥ 4 against *both* alternatives (12% faster than linear
     scan at 4 segments, 27% at 8, 50% at 16, 71% at 32; each of those also beats binary search at
     the same size) because it is the only approach that exploits the actual access pattern —
     every real consumer walks `charAt()` in ascending order — instead of just changing the
     complexity of a from-scratch search that ignores the previous call entirely. At 2 segments —
     this codebase's single most common case — the cache measured about 1% faster than the
     original linear scan, which is within measurement noise for this benchmark; treat that case
     as a wash, not a proven win, rather than reading anything into the small margin.
   - The cache is a plain mutable `int` field with no synchronization. It's safe without one:
     `charAt()` always re-validates the cached index against the requested position before
     returning a character, so a stale read under concurrent access on the same instance can only
     cost a few extra loop iterations — it can never produce a wrong character.
4. The "`hashCode()` isn't cached" critique from the issue stands, but we checked whether caching it
   would actually pay off anywhere it's called today and found it wouldn't, so it's deliberately
   **not** done here (see Consequences for the concrete reasoning, not just a deferral).

## Consequences

- `CompoundCharSequence` gains one mutable field (`lastPartsIndex`). It is no longer a fully
  immutable value type at the field level, though `equals`/`hashCode`/`compareTo` and all
  externally observable behavior are unchanged.
- Net effect on the `charAt`-only microbenchmark: the `CompoundCharSequence`-vs-`String`
  per-character overhead at 32 segments drops from 7.9x (original linear scan) to about 4.4x with
  the cache — a real reduction, though a fixed gap remains that no segment-resolution strategy can
  close (bounds checks, absolute-index arithmetic, and polymorphic dispatch to
  `parts[partsIndex].charAt(...)` vs. a direct array read for `String`). At the realistic 2–4
  segment range, the improvement is small in absolute terms and further muted in the end-to-end
  `TrieMap.get()` timing, which is dominated by trie-node traversal rather than segment resolution.
- No behavior change for any existing caller. Full `querqy-core` suite (915 tests) plus a new
  `CompoundCharSequenceTest#testCharAtOutOfOrderAccess` (deliberately non-monotonic access, to
  exercise the cache's correction path) pass.
- **`hashCode()` caching considered and rejected for now.** The same
  "mutable-field-is-safe-without-synchronization" reasoning used for `lastPartsIndex` would apply
  equally to a `String`-style lazily-computed, cached hash field. But caching only pays off for an
  instance whose `hashCode()` is read more than once, and we found exactly one such call site in
  production: `ReplaceRewriterParser`'s duplicate-input check
  (`checkForDuplicateInput.contains(seq)` immediately followed by `checkForDuplicateInput.add(seq)`
  on the same `seq`), where `HashSet.contains()` and `.add()` each independently hash their
  argument — a genuine 2x `hashCode()` computation on one instance. That check, however, runs at
  **rule-parse time** (once per rule file load), not per search query. `DeleteInstruction`'s
  `isToBeDeleted()` (`DeleteInstruction.java:92`) *does* run at query time, but it builds a **fresh**
  `CompoundCharSequence` per call via `getLookupCharSequence(term)`, used for exactly one
  `HashSet.contains()` check and then discarded — `hashCode()` is computed once regardless of
  caching. So, unlike the `charAt()` fix (which paid off on the per-query `TrieMap`/tokenizer path),
  there is currently no query-time call site where caching `hashCode()` would save any work — only a
  parse-time one, where the saving is real but runs once per rule-set load rather than once per
  search request. Revisit if a future usage repeatedly hashes the same instance on a hot path.
- Issue #148's removal question is resolved as "no, keep them" by this ADR; the issue should be
  closed with a reference to this decision once merged.

## References

- GH issue #148 — "[Housekeeping] Rethink usage of custom CharSequence implementations", including
  `@JohannesDaniel`'s 2021 follow-up comment on `CompoundCharSequence`'s metadata overhead and
  contemporary query-time usage frequency.
- `querqy-core/src/main/java/querqy/CompoundCharSequence.java` — `charAt()` / `getPartsIndex()`.
- `querqy-core/src/main/java/querqy/rewriter/commonrules/model/DeleteInstruction.java:40,92` and
  `querqy-core/src/main/java/querqy/rewriter/replace/ReplaceRewriterParser.java:77,114-143` —
  production (non-test) `HashSet<CharSequence>` dedup relying on `equals`/`hashCode`.
- `querqy-core/src/test/java/querqy/CompoundCharSequenceTest.java` — existing coverage plus the new
  out-of-order access test.
