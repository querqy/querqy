# ADR-0007: Abstract JSON parsing/serialization behind a `JsonProvider` SPI

- **Status:** Accepted
- **Date:** 2026-10-15
- **Area:** Querqy core — `querqy.json`, `SearchEngineRequestAdapter`; also touches `querqy-lucene`
  (`QueryParsingController`, `LuceneSearchEngineRequestAdapter`)
- **Relates to:** querqy/querqy#1158, branch `issue1158`

## Context

`querqy-core` and `querqy-lucene` depended directly on Jackson (`jackson-databind`/`jackson-core`/
`jackson-annotations`) for JSON handling in three places:

1. `PropertySkeletonParser` (CommonRules "property" block parsing) used a lenient, directly-configured
   Jackson `ObjectMapper` (`ALLOW_UNQUOTED_FIELD_NAMES`, `ALLOW_YAML_COMMENTS`, `ALLOW_SINGLE_QUOTES`,
   `STRICT_DUPLICATE_DETECTION`) to parse the DSL's lenient property syntax, plus a second, plain
   `ObjectMapper` for serializing properties back to text.
2. `NumberUnitRewriterConfigParser` used a plain `ObjectMapper` to bind the NumberUnit rewriter's JSON
   config into `NumberUnitConfigObject`.
3. `querqy-lucene`'s `QueryParsingController` used a hardcoded Jackson `ObjectMapper` (with
   `NON_NULL`/`NON_EMPTY` inclusion) to convert rewrite-chain debug/logging objects into generic
   `Map`/`List` structures for the host engine to serialize.

This repeatedly caused real problems downstream: every host integration (Solr, Elasticsearch,
OpenSearch) bundles its own Jackson version for its own purposes, so Querqy's own Jackson dependency
either duplicates an already-present library (wasted size, two copies on the classpath) or — worse —
conflicts with it. Concretely, Amazon OpenSearch Service's plugin-upload validation rejects a plugin
that bundles a different `jackson-core` than the one the server's own `lib/` already ships (an
install-time "jar hell" check), so `querqy-opensearch`'s AWS-installable `core` module could never
actually fix a Jackson CVE by bumping its own bundled version — the only real fix was to not bundle
Jackson at all. Jackson's own CVE history also meant periodic forced version-bump commits in every
downstream repo just to keep passing CVE scans, for a dependency those repos don't need beyond basic
JSON (de)serialization.

A fourth, initially-undiscovered Jackson touch point was found during implementation:
`PropertyParser`/`InstructionsProperties` (CommonRules property **filter-matching**, via
`com.jayway.jsonpath`) explicitly configured JsonPath with `JacksonJsonProvider`/
`JacksonMappingProvider` — a completely separate SPI (JsonPath's own `JsonProvider`/`MappingProvider`,
unrelated to this ADR's new one) that also needed addressing.

## Decision

Introduce `querqy.json.JsonProvider` (`readMap(String)`, `<T> T readValue(String, Class<T>)`,
`writeValueAsString(Object)`) as the one interface `querqy-core`/`querqy-lucene` production code is
allowed to use for JSON handling, with **no built-in implementation and no hidden default** — callers
must supply one explicitly, the same way `QuerqyParserFactory` is already threaded through
`RulesParserConfig`/`RuleParserConfig`. Implementations **must not** support polymorphic/default-typing
deserialization (no `@JsonTypeInfo` equivalent) — the same constraint a `forbidden-apis` check
(`forbidden-apis/jackson-signatures.txt`) had already been enforcing against Jackson's own gadget-chain
RCE class of vulnerability, now stated as a contract on the SPI itself so any implementation (not just a
Jackson-backed one) stays safe. That `forbidden-apis` check was later removed once Jackson stopped being
a dependency of either module at all (nothing left for it to scan against) — the contract now lives only
in this Javadoc, which is why it's documented here rather than relying solely on a build-time check that
can't exist anymore.

Per touch point:

- **`PropertySkeletonParser`**: the lenient-syntax concern is fully separated from JSON parsing itself.
  A new `LenientJsonNormalizer` (single-pass, dependency-free text transform) converts the dialect's
  three leniency features — `#`-to-end-of-line comments, single-quoted strings, unquoted object keys —
  into strict JSON text *before* handing it to the injected `JsonProvider`. (`STRICT_DUPLICATE_DETECTION`
  is not a syntax leniency but a semantic rejection, so it stays as `PropertySkeletonParser`'s own
  existing duplicate-key check.) `create()` becomes `create(JsonProvider)` (required, no default),
  threaded through `TextParserConfig`/`RulesParserConfig`/`TextParserFactory`/`RulesParserFactory`/
  `SimpleCommonRulesRewriterFactory`, mirroring `QuerqyParserFactory`'s existing threading.
- **`NumberUnitRewriterConfigParser`/`NumberUnitRewriterFactory`**: take a required `JsonProvider`
  parameter instead of constructing an `ObjectMapper` directly (breaking constructor change — see
  Consequences).
- **`PropertyParser`/`InstructionsProperties`**: new, genuinely dependency-free
  `querqy.json.jsonpath.QuerqyJsonPathProvider`/`QuerqyJsonPathMappingProvider` replace the
  Jackson-backed JsonPath configuration. `com.jayway.jsonpath`'s own `AbstractJsonProvider` already
  implements every `Map`/`List`-walking SPI method purely in terms of plain Java collections, so the new
  provider only needs to supply `createArray()`/`createMap()` (plain `ArrayList`/`LinkedHashMap`); the
  codebase's actual usage never calls `JsonProvider.parse(String)` (it only ever calls
  `JsonPath.using(config).parse(Map)` on an already-built property map) or a typed
  `MappingProvider.map(...)`, confirmed by exhaustively grepping every call site, so those methods
  intentionally throw `UnsupportedOperationException` rather than implement an unexercised code path.
  This is **not** a full Jackson-to-`querqy.json.JsonProvider` delegation (deliberately, see Deferred
  below) — it needs no delegate at all for what this codebase actually does with JsonPath.
- **`querqy-lucene`'s `QueryParsingController`**: a new `getJsonProvider()` method (required, no default —
  breaking interface change) supplies the provider; the two Jackson `convertValue`-style conversions
  become a `writeValueAsString` + `readMap`/`readValue` round trip through the injected provider (the
  same thing Jackson's `convertValue` already does internally). The method was initially added to
  `LuceneSearchEngineRequestAdapter`, then moved up to the shared `SearchEngineRequestAdapter`
  (`querqy-core`) once it became clear non-Lucene host integrations (e.g. Querqy Unplugged) need a
  `JsonProvider` too and shouldn't have to implement the Lucene-specific interface to get one. This is a
  host-integration-facing interface with no in-repo implementer (confirmed - only downstream repos like
  `querqy-opensearch` implement it), so the breaking change only affects those downstream repos' own
  adapter classes, not anything in this repo.
- **`pom.xml` (both modules)**: Jackson is not a dependency of either module at all, at any scope —
  `jackson-databind`/`jackson-core`(/`jackson-annotations` in `querqy-lucene`) were initially moved to
  `<scope>test</scope>` to back a test-only reference implementation (`JacksonJsonProvider`), then
  removed entirely once it turned out dependency scanners like Dependabot don't exempt test-scope
  dependencies from CVE alerts — keeping Jackson around test-only didn't actually reduce scanner noise,
  only reduce what actually shipped. The test-only reference implementations are now `JsonSmartJsonProvider`
  (backed by `net.minidev:json-smart`, already a mandatory transitive dependency of `json-path` — adds no
  new dependency footprint) and the hand-rolled, fully dependency-free `ToyJsonProvider`; the existing
  `PropertySkeletonParserTest`/`NumberUnitRewriterConfigParserTest` suites run against both, proving the
  SPI boundary is real rather than a wrapper only ever exercised by one specific library. `maven-jar-plugin`'s
  `addMavenDescriptor` is disabled on both modules: Maven embeds a full copy of a module's `pom.xml`
  inside the jar it builds by default, *regardless of dependency scope* — a scanner reading that embedded
  copy's declared dependencies without honoring `<scope>` would still see test-only dependencies listed as
  if bundled. This was found to matter in practice: it was the exact reason AWS's plugin-upload scan kept
  flagging Jackson CVEs even when Jackson was (at the time) test-scoped and not actually bundled anywhere
  in the plugin zip; the setting is now a general precaution against the same class of false positive for
  any test-only dependency (e.g. `json-smart`, `mockito`), not specifically about Jackson.

### Explicitly deferred, not bundled into this change

- **A production-quality `querqy.json.JsonProvider` implementation shipped from `querqy-core` itself.**
  There is deliberately none — every implementation lives either in test sources (`JsonSmartJsonProvider`,
  `ToyJsonProvider`) or in a downstream host integration (`querqy-opensearch`'s
  `XContentJsonProvider`, backed by OpenSearch's own `XContent`). Each host engine is expected to supply
  its own, reusing whatever JSON-capable infrastructure it already has (XContent for
  OpenSearch/Elasticsearch, Jackson directly for Solr/Querqy Unplugged, per the issue's own proposal).
- **Removing `net.minidev:json-smart`/`accessors-smart` from the dependency graph entirely.** Attempted
  during the downstream `querqy-opensearch` integration and reverted after an empirical
  `NoClassDefFoundError` reproduction: `com.jayway.jsonpath`'s own filter-expression grammar
  (`com.jayway.jsonpath.internal.filter.ValueNode`, core JsonPath code exercised regardless of which
  `JsonProvider`/`MappingProvider` is configured) has a second, non-pluggable hard dependency on
  `net.minidev.json.parser.JSONParser` to recognize array/object-literal tokens inside predicates (e.g.
  the `in [...]` operator, used by `InstructionsPropertiesTest`'s own `testInOperatorMatches` and
  reachable via CommonRules selection-strategy filters). `json-smart`/`accessors-smart` therefore remain
  a real (if now much smaller) part of the dependency graph: json-path's two now-dead provider classes
  (`JsonSmartJsonProvider`/`JsonSmartMappingProvider`) are no longer referenced by this codebase, but the
  library itself cannot be dropped while using JsonPath's filter-expression engine at all. A future ADR
  would be needed if this is revisited (e.g. by replacing `com.jayway.jsonpath` itself for the
  property-filter-matching use case).
- **Migrating `querqy-solr`/`querqy-elasticsearch`/`querqy-unplugged`** to supply their own
  `JsonProvider` for the now-required parameters on `NumberUnitRewriterFactory`,
  `SimpleCommonRulesRewriterFactory`, and `SearchEngineRequestAdapter.getJsonProvider()`. Each is a
  separate repository with its own release cadence (same precedent as ADR-0005) — this ships the
  capability; those repos adopt it in their own follow-up PRs once they pull in a `querqy-core`/
  `querqy-lucene` release containing this change. `querqy-opensearch` is the one integration already
  migrated and verified end-to-end (including a successful AWS OpenSearch Service deployment with the
  resulting Jackson-free plugin bundle). `getJsonProvider()` living on `SearchEngineRequestAdapter`
  rather than the Lucene-specific `LuceneSearchEngineRequestAdapter` means `querqy-unplugged` (which
  doesn't depend on `querqy-lucene`) can adopt it too.

## Consequences

### Shipped in this change

- New `querqy.json.JsonProvider` SPI and `querqy.json.jsonpath.QuerqyJsonPathProvider`/
  `QuerqyJsonPathMappingProvider`, plus `LenientJsonNormalizer`, all in `querqy-core` production code,
  with zero Jackson (or any other third-party JSON library) dependency of their own.
- Breaking constructor/interface changes: `PropertySkeletonParser.create(JsonProvider)` (was
  no-arg), `NumberUnitRewriterFactory`'s two constructors and `validateConfiguration` gain a trailing
  `JsonProvider` parameter, `SimpleCommonRulesRewriterFactory`'s constructor gains a trailing
  `JsonProvider` parameter, `RulesParserFactory`'s convenience `textParser(...)` overload gains a
  trailing `JsonProvider` parameter, and `SearchEngineRequestAdapter` (`querqy-core`) gains a new
  required `getJsonProvider()` method (initially added to `LuceneSearchEngineRequestAdapter`, later
  moved up — see Decision above). None of these have an in-repo caller outside this change's own tests
  (confirmed per call site before changing each), so nothing in this repo needed a compatibility shim —
  downstream repos adapt when they adopt the release.
- Jackson is not a dependency of either module at all, at any scope (initially test-scope-only, then
  removed entirely — see Decision above); `maven-jar-plugin`'s embedded-POM descriptor is disabled on
  both as a general precaution against the same class of scanner false positive for any remaining
  test-only dependency.
- Full existing test suites (974 tests in `querqy-core`, 113 in `querqy-lucene`) pass, run twice where
  it matters (`PropertySkeletonParserTest`/`NumberUnitRewriterConfigParserTest`, parameterized across
  `JsonSmartJsonProvider` and `ToyJsonProvider`) to prove the abstraction. A security review of this
  change also found and fixed a real regression (duplicate JSON object keys silently accepted instead
  of rejected — see `DuplicateJsonKeyChecker`) and added test coverage for a previously-untested path
  (`QueryParsingController`'s rewrite-chain debug/logging round trip, see `QueryParsingControllerTest`).

### Trade-offs

- Every host integration must now supply its own `JsonProvider` via `SearchEngineRequestAdapter.getJsonProvider()`
  — one extra small class per integration, in exchange for no longer carrying a third-party JSON library
  as a hard production dependency.
- The CommonRules property-filter-matching path (`InstructionsProperties.matches(String)`) still
  pulls in `net.minidev:json-smart`/`accessors-smart` transitively via `com.jayway.jsonpath` itself,
  for reasons unrelated to and unfixable by this SPI (see Deferred above) — this change removes
  Querqy's own Jackson usage but does not make `querqy-core` fully JSON-library-free; it trades one
  dependency (Jackson, actively problematic) for a narrower, already-present one (json-smart, not
  independently a known problem at the version in use).
- **The SPI makes no guarantee about null/empty-field inclusion in serialized output**, which changes
  the shape of `QueryParsingController`'s rewrite-chain debug/logging JSON. Before this SPI existed,
  that class's hardcoded Jackson `ObjectMapper` was explicitly configured with `NON_NULL`/`NON_EMPTY`
  inclusion to omit null/empty fields; `querqy.json.JsonProvider#writeValueAsString` carries no such
  contract, so an implementation is free to include them instead. Confirmed concretely:
  `JsonSmartJsonProvider` (`json-smart`'s `JSONValue.toJSONString`) does include nulls (e.g.
  `{"message":"...","match":null,"instructions":null}`), making this debug/logging output more verbose
  than before. **This is not restored automatically by a host integration choosing a Jackson-backed
  `JsonProvider` either** — `NON_NULL`/`NON_EMPTY` was a deliberate configuration choice on that one old
  `ObjectMapper`, not Jackson's own default (Jackson's default, like `json-smart`'s, is to include
  nulls/empties). Any host integration wanting the old compact shape back must configure its own
  `JsonProvider` implementation to do so explicitly; this repo can no longer even verify that behavior
  itself, since Jackson isn't on its classpath at all anymore. Documented on `JsonProvider`,
  `JsonSmartJsonProvider`, and `QueryParsingController.toGenericList` directly.

## References

- `querqy-core/src/main/java/querqy/json/JsonProvider.java` — the SPI itself.
- `querqy-core/src/main/java/querqy/rewrite/SearchEngineRequestAdapter.java` — declares
  `getJsonProvider()` (moved here from `LuceneSearchEngineRequestAdapter` so non-Lucene host
  integrations can supply one too).
- `querqy-core/src/main/java/querqy/json/jsonpath/QuerqyJsonPathProvider.java`,
  `QuerqyJsonPathMappingProvider.java` — the dependency-free JsonPath SPI implementation.
- `querqy-core/src/main/java/querqy/rewriter/commonrules/rules/property/skeleton/LenientJsonNormalizer.java`
  — the lenient-dialect-to-strict-JSON preprocessor.
- `querqy-core/src/main/java/querqy/rewriter/commonrules/rules/property/skeleton/DuplicateJsonKeyChecker.java`
  — rejects duplicate keys within a single JSON object literal, independent of whichever `JsonProvider`
  is wired in (found missing, and added, during a later security review of this change).
- `querqy-core/src/test/java/querqy/json/JsonSmartJsonProvider.java`, `ToyJsonProvider.java`,
  `ReflectiveBeanBinder.java` — the two test-only reference implementations (`JsonSmartJsonProvider`
  replaced the original `JacksonJsonProvider` once Jackson was dropped entirely), and the reflection-based
  POJO binder they share.
- `querqy-lucene/src/main/java/querqy/lucene/QueryParsingController.java`,
  `LuceneSearchEngineRequestAdapter.java` — the `querqy-lucene` side of this change.
- `querqy-lucene/src/test/java/querqy/lucene/QueryParsingControllerTest.java` — test coverage for the
  rewrite-chain debug/logging round trip (added during the later security review; this path had none
  initially).
- `querqy-opensearch`'s `core/src/main/java/querqy/opensearch/json/XContentJsonProvider.java` — the
  first downstream `JsonProvider` implementation, backed by OpenSearch's own `XContent`.