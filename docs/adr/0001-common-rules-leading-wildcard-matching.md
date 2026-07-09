# 1. Matching strategy for leading wildcards in the Common Rules Rewriter

Date: 2026-07-09

## Status

Accepted

## Context

[Issue #1126](https://github.com/querqy/querqy/issues/1126) asks for leading wildcard
support in the Common Rules Rewriter's rule input, e.g.:

```
*shirt =>
SYNONYM: $1hemd
```

so that `baumwollshirt` matches and expands to `baumwollhemd`, capturing the wildcarded
prefix into `$1`. This mirrors the trailing wildcard that already exists (`shirt*`), but
on the other side of the term, and — per requirements gathered for this issue — the
wildcard-bearing term should be usable at any position within a multi-term rule input
(e.g. `abc *hemd`), not just the first/last term.

Two existing implementations were surveyed before deciding on an approach:

1. **Common Rules Rewriter** already supports a trailing wildcard via `PrefixTerm`, but
   only when the `*` is the very last character of the entire rule input (i.e. only the
   last term may carry it), and it captures one-or-more characters (never zero).
   `TrieMapRulesCollectionBuilder` populates a single, shared, character-level
   `TrieMap<InstructionsSupplier>` by concatenating all terms of a rule (space-delimited)
   into one key (unchanged by this decision). The actual query-time lookup against that
   trie is done by `querqy.rewrite.lookup.triemap.TrieMapLookupQueryVisitor` (with
   `TrieMapSequenceLookup` and `TrieMapMatchCollector`), which visits the query's
   `BooleanQuery` tree directly and threads trie `States` across terms, calling
   `states.getPrefixes()` to collect *all* trailing-wildcard matches (not just the
   longest). Note: `RulesCollection`/`TrieMapRulesCollection.collectRewriteActions` is a
   `@Deprecated`, no-longer-wired-up predecessor of this lookup mechanism (superseded by
   the `TrieMapLookupQueryVisitor` visitor) and must not be used as an integration point —
   both `SimpleCommonRulesRewriterFactory` (production) and `AbstractCommonRulesTest`
   (tests, e.g. the existing prefix-wildcard tests in `SynonymInstructionTest`) build the
   rewriter via `TrieMapLookupQueryVisitorFactory.of(builder.getTrieMap())`, never through
   `RulesCollection`.
2. **ReplaceRewriter** (`querqy.rewriter.replace`), a separate, newer rewriter, already
   supports both `foo*` and `*foo` via `SequenceLookup`, which is backed by
   `PrefixTrieMap` and `SuffixTrieMap`. `SuffixTrieMap` implements leading-wildcard
   ("ends with X") matching by reversing the character sequence
   (`ReverseComparableCharSequence`) and delegating to an ordinary forward `TrieMap` —
   i.e. a reverse trie. However, `SequenceLookup` only supports wildcard rules whose
   input is a **single term**; it explicitly rejects combining a wildcard term with
   other fixed terms in the same rule.

Neither existing implementation supports mixing a leading-wildcard term with adjacent
fixed terms in one rule, which this issue requires.

### Rejected alternative: embed the wildcard as a transition in the shared `TrieMap`

One alternative considered was extending the core trie (`querqy.trie.Node`/`TrieMap`)
itself with a special wildcard transition (e.g. a self-loop node consuming "any
character"), handled natively while stepping through the trie.

This was rejected:

- `Node.get(seq, index)` today is a strictly deterministic, single-branch recursion:
  each node has exactly one outgoing edge per character, and every recursive step
  advances `index` by exactly one. A wildcard-then-fixed-suffix pattern requires, at
  every character position while "in wildcard mode", simultaneously trying two live
  branches: "keep consuming as wildcard" and "does the fixed suffix start matching from
  here". The current `Node`/`State`/`States` model returns one deterministic outcome per
  node and isn't built to carry two parallel candidate branches — supporting this would
  mean reworking the shape of the core recursion itself.
- No efficiency benefit: since the agreed scope allows only one wildcard per rule (no
  chained/repeated wildcards, no Kleene-star semantics), a fork-per-position
  implementation is O(term length) — exactly the same complexity class as reversing the
  string once and doing one ordinary trie walk (what `SuffixTrieMap` already does). There
  is no performance win to justify the added complexity.
- Blast radius: `Node`, `TrieMap`, `State`, and `States` are shared foundational classes
  used by other trie consumers beyond Common Rules (e.g. `ReplaceRewriter`'s prefix map).
  Changing their core recursion to support forking risks regressions across all of them.
- (Initially raised, but not actually a blocker on inspection: infinite recursion is not
  a structural risk either way, because `Node.get()`'s recursion always strictly advances
  the input index per step, and the wildcard's semantics are "one or more", never zero —
  so a correctly-modeled self-loop transition, consuming one character per step, cannot
  loop indefinitely. The decisive factors against this alternative are the two points
  above, not this one.)

## Decision

Implement leading-wildcard matching as a **separate, additive structure**, without
modifying the shared `TrieMap`/`Node` core:

- Add a new rule-input term type (mirroring `PrefixTerm`) representing a term with a
  fixed suffix and a wildcarded prefix.
- Extend `LineParser` to accept `*` at the start of a term, at any position within the
  rule input (not restricted to the first/last term), reusing existing escaping (`\*`)
  and validation conventions. Exactly one wildcard-bearing term is allowed per rule
  (leading or trailing, never both), matching the existing `$1`-only placeholder limit.
  The existing trailing-wildcard restriction (only allowed on the last term) is left
  unchanged.
- For matching, keep the existing shared `TrieMap<InstructionsSupplier>` (built by
  `TrieMapRulesCollectionBuilder`) and `TrieMapSequenceLookup`'s forward character walk
  completely untouched. Add a second, isolated lookup structure for rules that contain a
  leading-wildcard term, built on the same proven mechanism `SuffixTrieMap` already uses
  (reverse the character sequence, do an ordinary forward trie walk) — but using the
  lower-level reverse-trie plus an "all matching prefixes" style traversal (like
  `states.getPrefixes()`), not the higher-level `SuffixTrieMap.getBySuffix()` convenience
  method, which collapses to a single longest match. Common Rules semantics are
  cumulative (multiple rules can fire for the same term), unlike `ReplaceRewriter`'s
  single-replacement semantics, so all matches must be collected — mirroring what
  `TrieMapMatchCollector.addPrefixMatches()` already does for the existing trailing
  wildcard.
- A rule such as `abc *hemd def` has three parts, each matched differently:
  - `abc` (fixed terms before the wildcard) is purely literal, so it gets its own small,
    separate forward `TrieMap` (not mixed into the main rule trie, to preserve the
    isolation goal above), reusing the same proven forward-trie mechanism.
  - `*hemd` (the wildcard term) uses the reverse-trie lookup described above.
  - `def` (fixed terms after the wildcard) is checked directly, term-by-term, against the
    query once a candidate wildcard match is in flight, rather than via a third trie —
    this path only runs for the rare in-flight candidate right after a suffix match, so a
    trie's sublinear-in-rule-count advantage isn't needed there.
  - `requiresLeftBoundary`/`requiresRightBoundary` fold into the same left/right-term
    matching for free: the existing engine already represents "start/end of query" as a
    literal sentinel `Term` (`TrieMapLookupQueryVisitor.BOUNDARY_TERM`) fed through the
    normal term-visiting path, so boundaries are modelled as that same sentinel
    prepended/appended to the left/right term lists, instead of separate boundary-specific
    logic.
- All of this new bookkeeping is encapsulated in one new, independently-testable class
  (a suffix-wildcard matcher) rather than rewriting `TrieMapLookupQueryVisitor`'s
  internals. The visitor (the live query-time lookup — not the deprecated
  `RulesCollection`/`TrieMapRulesCollection` path) gets three small, additive delegation
  calls to this new class: once per visited term, once per position refresh (mirroring
  its existing `refreshSequenceLists()` cycle), and once when assembling final results.
  `TrieMapLookupQueryVisitor` is scoped to Common Rules lookup only (not shared with
  `ReplaceRewriter` or other trie consumers), so extending it carries none of the
  shared-blast-radius risk that ruled out touching `Node`/`TrieMap`; keeping the change to
  a few narrow, additive call sites (rather than reworking its per-term/per-position state
  machine) further limits the risk of regressing existing trailing-wildcard/exact-match
  behavior.
- Reuse `TermMatch`, `TermMatches`, and `Term.fillPlaceholders` as-is for `$1`
  substitution; they are already generic enough (storing a "wildcard-matched" char
  sequence plus a flag) to support a leading-wildcard capture without modification.

## Consequences

- No regression risk to the existing, performance-critical all-fixed-term matching path;
  the new feature is purely additive.
- Leading-wildcard rules are matched via a second data structure/lookup step rather than
  a single unified trie, which is a bit less uniform architecturally, but keeps the
  change isolated and low-risk.
- The shared `Node`/`TrieMap` core remains deterministic and untouched, preserving its
  simplicity and avoiding shared blast radius across its other consumers.
- If a future requirement needs multiple independent wildcards per rule, or wildcards
  with more general (e.g. Kleene-star) semantics, this decision should be revisited, as
  the complexity/performance trade-offs that ruled out the embedded-trie approach here
  may no longer hold.

## Example: two rules, two structures, one query

Given both

```
abc =>
SYNONYM: klm
```

and

```
abc *hemd def =>
SYNONYM: $1shirt
```

registered, and the query `abc xyzhemd def`, both rules fire — one match comes from the
main trie, the other from the new suffix-wildcard structure, and they are found
independently, at different terms, then merged.

**At rule-load time**, `TrieMapRulesCollectionBuilder.addOrMergeInstructionsSupplier`
detects the `SuffixTerm` in the second rule's input and routes it entirely to
`SuffixWildcardRulesBuilder` (early return, never touching the main trie). So after
loading: the first rule's `abc` is a key in the main, shared
`TrieMap<InstructionsSupplier>`; the second rule lives only in `SuffixWildcardRules` —
`abc` as a key in its small left-context forward trie, `hemd` (reversed) as a key in its
reverse suffix trie, and `def` recorded as a required right-context term.

**At query time**, `TrieMapLookupQueryVisitor.visit(Term term)` calls into *both*
structures for every visited term, unconditionally — there is no branching logic that
picks one or the other:

```java
public Void visit(final Term term) {
    visitSingleTerm(term);                      // main trie: fresh start
    if (!previousSequences.isEmpty()) {
        visitTermWithPreviousSequences(term);    // main trie: extend a pending match
    }
    suffixWildcardMatcher.visitTerm(term);        // the new, separate structure
    return null;
}
```

Walking through the three query positions:

1. **`abc`** — Main trie: `evaluateTerm("abc")` hits the exact-match node, so rule 1
   fires immediately (the `klm` synonym). Suffix matcher: `checkSuffixMatch` finds
   nothing (nothing is registered with `abc` as a *suffix*), but `advanceLeftContext`
   looks `abc` up in the small left-context trie and finds it completes rule 2's
   left-context requirement exactly — rule 2 is recorded in `nextReadyForWildcard`,
   available starting at the *next* position once `nextPosition()` swaps it in.
2. **`xyzhemd`** — Main trie: no hit. Suffix matcher: `checkSuffixMatch` reverses
   `xyzhemd`, walks the reverse trie, and finds `hemd` as a registered suffix. It checks
   whether that rule needs left context — yes, and it's sitting in `readyForWildcard`
   from step 1 — so the match is admitted, capturing `$1 = xyz`. Rule 2 also has a
   non-empty right context (`def`), so this doesn't fire yet; it's parked as a pending
   right-context candidate for the next position.
3. **`def`** — Suffix matcher's `advanceRightContext` checks the pending candidate's
   expected next term against `def` — it matches, and since it was the last required
   right-context term, the match fires now, completing rule 2.

**Merging:** `lookupAndCollect()` does
`Stream.concat(matchCollector.getMatches(), suffixWildcardMatcher.getMatches())` — rule
1's match (from the main trie) and rule 2's match (from the suffix structure) both end
up in the same result list, each becoming an `Action` whose instructions get applied.
Rule 2's `SynonymInstruction` then adds `xyzshirt` to all three positions it spans (one
instruction contributing to every position of its match is pre-existing `Action`/
instruction-application behavior, unrelated to this decision). That is why `abc` ends up
with contributions from both rules: rule 1's match is centered there, and rule 2's match
(found via `xyzhemd`, gated on `abc` as left context) spans back to include it. See
`SynonymInstructionTest#testThatPlainRuleAndLeadingWildcardRuleBothApplyToSameTerm` for
the executable version of this example.
