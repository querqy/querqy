# ADR-0004: Relocate the common-rules text parser under querqy.rewriter.commonrules

- **Status:** Accepted
- **Date:** 2026-07-13
- **Area:** Querqy core — `querqy.rewrite.rules` → `querqy.rewriter.commonrules.rules`
- **Relates to:** the removal of the dead `LineParser`/`SimpleCommonRulesParser` legacy parsing path in the same housekeeping effort (branch `housekeeping202607`).

## Context

While auditing `@Deprecated` code in `querqy-core`, we found that `SimpleCommonRulesRewriterFactory` — the live production factory for the Common Rules rewriter — has built its rules from text via a package called `querqy.rewrite.rules` (note: `rewrite`, not `rewriter`) since 3.13.0, not via the older line-by-line `LineParser`/`SimpleCommonRulesParser` path, which we removed as dead code in this same effort.

`querqy.rewrite.rules` sits at the top level, as a sibling of `querqy.rewriter` (which holds `commonrules`, `replace`, `numberunit`, `wordbreak`, etc.). That placement implies it is a general, cross-cutting concept in querqy-core — something other rewriters might plausibly use, or a "rules" abstraction independent of any one rewriter. We checked whether that's actually true before deciding where it belongs.

### What's actually in the package

30 main source files, 9 test files. The genuine object-model pieces are thin — `Rule`, `RuleSkeleton`, `InstructionSkeleton`, `PropertySkeletonInput`, `InstructionType` — everything else (`RulesParser`, `RuleParser`, `InstructionParser`, `QuerqyQueryParser`, `TermsParser`, `PropertyParser`, the skeleton parsers, the factories and configs) is the parsing pipeline that turns rules.txt-style text into those objects and ultimately into a `TrieMap<InstructionsSupplier>`.

### Does it generalize beyond the Common Rules rewriter?

No, checked from three angles:

- **Who uses it today:** grepping all of `querqy-core/src/main` for imports of `querqy.rewrite.rules.*` from outside that package turns up exactly four files, all inside `querqy.rewriter.commonrules`: `SimpleCommonRulesRewriterFactory`, `InputTermParser`, `RulesCollectionBuilder`, `TrieMapRulesCollectionBuilder`. None of the other rewriters (`replace`, `numberunit`, `wordbreak`, `regexreplace`, `explain`) reference it. None of the sibling repos (`querqy-solr`, `querqy-elasticsearch`, `querqy-opensearch`, `querqy-unplugged`) reference it either.
- **What it depends on:** everything `querqy.rewrite.rules.*` imports from outside itself is either fully generic (`querqy.model`, `querqy.parser`, `querqy.trie`) or specific to the Common Rules model (`Instruction`, `Term`, `BoostInstruction`, `BooleanInputParser`, `BooleanInputElement`, `RulesCollectionBuilder`, `InstructionsSupplier`). There is no design room for another rewriter to plug into this pipeline without dragging in the entire Common Rules instruction/term model.
- **Could the closest-looking candidate use it?** `querqy.rewriter.replace.ReplaceRewriterParser` is the other rule-line-based, wildcard-aware parser in the codebase, so it's the natural test case. It doesn't fit: Replace's rule format is `input => output` with no rule IDs, no properties/tags, no multi-line JSON blocks, and — critically — no equivalent of "one input, N typed instructions attached to it" (SYNONYM/UP/DOWN/FILTER/DELETE/DECORATE). Its RHS parsing (`parseQuery()`) runs text through `QuerqyParser` only to flatten it into a `LinkedList<String>` of literal terms, discarding query structure entirely, so it has no use for `QuerqyQueryParser`'s raw-query/parametrized-query handling. Its final structure is `SequenceLookup<ReplaceInstruction>`, not `TrieMap<InstructionsSupplier>`. Every layer of `querqy.rewrite.rules` exists to serve Common Rules' "one input → multiple typed instructions + properties" shape; Replace has none of that shape to serve, and forcing it through this pipeline would mean adopting a data model it doesn't need, not simplifying anything. Consistent with this, `ReplaceRewriterParser` was written independently and reuses none of `querqy.rewrite.rules` today, despite both having coexisted in the codebase for years.

So the package is not a general "rules" facility that happens to live at the top level — it is private implementation detail of one rewriter, misleadingly placed as if it were a sibling of `querqy.rewriter` itself. Its current name also carries the `rewrite`/`rewriter` naming split left over from the partial package reorg in #1122, which renamed `querqy.rewrite.commonrules` to `querqy.rewriter.commonrules` but never touched this tree.

## Decision

Move `querqy.rewrite.rules.*` to `querqy.rewriter.commonrules.rules.*` — a new sub-package of `commonrules`, alongside its existing `model` and `select` sub-packages.

This is a pure package rename: every sub-package keeps its internal structure (`rules.factory`, `rules.factory.config`, `rules.input`, `rules.input.skeleton`, `rules.instruction`, `rules.instruction.skeleton`, `rules.property`, `rules.property.skeleton`, `rules.query`, `rules.rule`, `rules.rule.skeleton`) — only the `querqy.rewrite` prefix changes to `querqy.rewriter.commonrules`. No class is renamed, no logic changes, no public method signatures change beyond their fully-qualified names.

### Explicitly deferred, not bundled into this move

- **`rule.skeleton.LineParser`** (an interface, implemented by `SingleLineParser`/`MultiLineParser`) shares its simple name with the now-deleted `querqy.rewriter.commonrules.LineParser` class from the legacy path. Renaming it to remove the collision is worth doing but is a separate, distinct change from a package relocation — keeping this move mechanical (rename only, no renames-while-moving) makes it trivially reviewable.
- **Flattening the sub-package nesting.** Now that this tree is package-private to `commonrules` rather than a top-level concept, some of its internal nesting (e.g. `rules.instruction.skeleton`) may be more granular than it needs to be. Left for a future pass, if wanted.

### External impact

`querqy.rewrite.rules.factory.RulesParserFactory` and `querqy.rewrite.rules.factory.config.{RuleParserConfig, RulesParserConfig, TextParserConfig}` are public API. Any external consumer importing them directly (e.g. an in-progress SMUI change) needs to update those import paths to `querqy.rewriter.commonrules.rules.factory...` once it adopts a querqy-core release containing this move.

## Consequences

### Shipped in this change

- All 30 main + 9 test files under `querqy.rewrite.rules` moved to `querqy.rewriter.commonrules.rules`, with matching package declarations and import updates in the four consuming files (`SimpleCommonRulesRewriterFactory`, `InputTermParser`, `RulesCollectionBuilder`, `TrieMapRulesCollectionBuilder`) and within the moved tree itself.
- No behavior change. Verified by a full `querqy-core` test run before and after.

### Trade-offs

- Fully-qualified references from within `commonrules` are now one segment longer (e.g. `querqy.rewriter.commonrules.rules.factory.RulesParserFactory`). Acceptable: this package is referenced directly by only four files today.
- This move alone does not fix the `LineParser` name collision or the sub-package granularity — both remain open, tracked above as explicitly deferred rather than silently dropped.

## References

- `querqy-core/src/main/java/querqy/rewriter/commonrules/SimpleCommonRulesRewriterFactory.java` — the production entry point that drove this investigation.
- `querqy-core/src/main/java/querqy/rewriter/replace/ReplaceRewriterParser.java` — the closest-looking candidate that was checked and found not to fit.
- Commit removing the dead `LineParser`/`SimpleCommonRulesParser` legacy path, same branch (`housekeeping202607`).
