# ADR-0005: Move NumberUnitRewriter config parsing into querqy-core

- **Status:** Accepted
- **Date:** 2026-07-14
- **Area:** Querqy core — `querqy.rewriter.numberunit`
- **Relates to:** querqy/querqy#1046, branch `issue1046`

## Context

`NumberUnitRewriter` is configured via a JSON document (a list of unit/field/boost/filter
definitions, plus a `scaleForLinearFunctions` setting). `querqy-core` only ever received the
already-parsed result — `NumberUnitRewriterFactory`'s constructor took a
`List<NumberUnitDefinition>` and a ready-made `NumberUnitQueryCreator`. Turning the raw JSON into
that list (applying defaults, validating it, producing error messages) was left to each
search-engine module.

We checked all four consumers (`querqy-solr`, `querqy-elasticsearch`, `querqy-opensearch`,
`querqy-unplugged`, checked out locally as sibling repos) and found the parsing code almost
entirely duplicated:

- `querqy-elasticsearch` and `querqy-opensearch`'s `NumberUnitConfigObject` DTOs are
  byte-for-byte identical (only package names differ).
- Their `NumberUnitRewriterFactory.parseConfig()`/`parseNumberUnitDefinition()`/
  `parseUnitDefinitions()`/`parseFieldDefinitions()` methods are identical, including the same
  ~12 hardcoded default constants (`DEFAULT_BOOST_MAX_SCORE_FOR_EXACT_MATCH = 200`,
  `DEFAULT_FILTER_PERCENTAGE_LOWER_BOUNDARY = 20`, etc.) and the same validation rules (at least
  one unit and one field per definition, non-blank `term`/`fieldName`, no duplicate unit terms
  within a definition).
- `querqy-solr`'s version is the same logic again; only the glue that extracts the JSON string
  from Solr's config map differs.
- `querqy-unplugged`'s version duplicates the logic once more, this time exposing the default
  constants as overridable fields on a builder (`NumberUnitRulesDefinition`) — but the default
  *values* it ships are identical to the other three.

All four modules receive the config as a JSON **string** (the value of a `"config"` property) and
hand it to a per-module `NumberUnitConfigObject` DTO via Jackson before mapping it onto core's
model classes. None of them receive a pre-parsed `Map<String, Object>` for this rewriter today.
`querqy-core` already depends on `jackson-databind` (used by
`querqy.rewriter.commonrules.rules.property.skeleton.PropertySkeletonParser`), so reusing it here
adds no new dependency — the JDK has no built-in JSON parser, so the realistic alternative was
hand-rolling one for no dependency savings.

There's already a precedent in core for a rewriter factory owning its config parsing outright:
`RegexReplaceRewriterFactory` takes a raw `InputStreamReader` and parses the entire rules format
internally via `RegexReplaceRewriterRulesParser`, throwing `IOException` on malformed input —
callers don't pre-parse anything for it.

## Decision

Move config parsing fully into `NumberUnitRewriterFactory` itself, mirroring the
`RegexReplaceRewriterFactory` precedent, rather than adding a parallel public parser class next to
an unchanged factory:

- **Breaking change** to `NumberUnitRewriterFactory`'s constructor: it now takes
  `(String rewriterId, String config, IntFunction<NumberUnitQueryCreator> queryCreatorFactory)`
  instead of `(String rewriterId, List<NumberUnitDefinition> numberUnitDefinitions, NumberUnitQueryCreator numberUnitQueryCreator)`.
  The old constructor is removed, not deprecated — no core test exercised it (core's
  `NumberUnitRewriterTest` builds `NumberUnitRewriter` directly against a hand-built `TrieMap`,
  bypassing the factory entirely), so there is nothing in this repo to preserve compatibility
  with. The four downstream repos adapt in their own follow-up PRs once they adopt a querqy-core
  release containing this change.
- The constructor parses `config` internally (new package-private
  `NumberUnitRewriterConfigParser` + `NumberUnitConfigObject` DTO, alongside the factory in
  `querqy.rewriter.numberunit`, same layout as `regexreplace`), applying the default constants
  previously duplicated per-module, and throws `IOException` for malformed JSON or
  `IllegalArgumentException` for semantic errors (missing unit/field definitions, blank
  term/fieldName) — same split as the code it replaces.
- `queryCreatorFactory` exists because `NumberUnitQueryCreator` construction is engine-specific
  (e.g. `NumberUnitQueryCreatorSolr`) and needs `scaleForLinearFunctions`, which only becomes known
  once the config has been parsed. Callers pass a factory function (e.g.
  `scale -> new NumberUnitQueryCreatorSolr(scale)`); the constructor parses the config first, then
  invokes the function with the resolved scale.
- A new static `NumberUnitRewriterFactory.validateConfiguration(String config)` returns
  `List<String>` error messages without throwing, for the pre-flight validation step search
  engines run before actually constructing the rewriter (ported from the existing
  `validateConfiguration()` methods, including the duplicate-unit-term check that `parse()` itself
  does not perform).

### Explicitly deferred, not bundled into this change

- **Updating `querqy-solr`, `querqy-elasticsearch`, `querqy-opensearch`, `querqy-unplugged`** to
  delete their duplicated DTOs/parsing code and call the new constructor. Those are separate
  repositories with their own release cadences; each gets its own follow-up PR once a
  `querqy-core` release containing this change is available.
- **`querqy-unplugged`'s per-call-site override of default constants** (via
  `NumberUnitRulesDefinition`). If that flexibility is still needed once `querqy-unplugged`
  migrates, it would need to be re-added as an explicit follow-up (e.g. an optional overrides
  parameter), scoped to that migration rather than guessed at here.

## Consequences

### Shipped in this change

- `querqy.rewriter.numberunit.NumberUnitConfigObject` and
  `querqy.rewriter.numberunit.NumberUnitRewriterConfigParser` added to `querqy-core`.
- `NumberUnitRewriterFactory`'s public constructor changed (breaking); its `createRewriter()`/
  `getCacheableGenerableTerms()` behavior is unchanged.
- Test coverage (full/minimal/invalid config fixtures, `validateConfiguration()` cases) ported
  from the existing per-module `*ParseConfigTest` suites into `querqy-core`.

### Trade-offs

- Until the four downstream repos adopt this constructor, the duplication issue #1046 describes
  isn't actually eliminated yet — this change only makes the shared implementation available and
  removes the old core-side entry point they'd otherwise keep building against.
- The default constants are now fixed in core rather than overridable per call site, narrowing
  `querqy-unplugged`'s current flexibility (see deferred item above).

## References

- `querqy-elasticsearch/src/main/java/querqy/elasticsearch/rewriter/NumberUnitRewriterFactory.java`
  and `.../numberunit/NumberUnitConfigObject.java` — primary source ported from.
- `querqy-solr/querqy-solr/src/main/java/querqy/solr/rewriter/numberunit/NumberUnitRewriterFactory.java`
- `querqy-unplugged/library/src/main/java/querqy/rewriter/builder/NumberUnitRulesFactorBuilder.java`
- `querqy-core/src/main/java/querqy/rewriter/regexreplace/RegexReplaceRewriterFactory.java` and
  `RegexReplaceRewriterRulesParser.java` — the existing precedent for a rewriter factory owning
  its own config parsing in core, followed here for package layout and exception style.
