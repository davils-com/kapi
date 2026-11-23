# AGENTS.md

Binding rules for every agent and every person working in this repository. kapi is the API toolkit of Davils: it
reads OpenAPI and AsyncAPI documents strictly, checks them with located diagnostics, validates JSON Schema 2020-12,
and generates typed Kotlin clients, servers, models, mocks, webhooks and WebSocket channels on top of Ktor. The client
runtime runs on the JVM and on WebAssembly (`wasmJs`); the server runtime, the generator and the Gradle plugin run on
the JVM. kapi reports through events on [konfetti](https://github.com/davils-com/konfetti) topics and meters and spans
in [kollect](https://github.com/davils-com/kollect), parses YAML and JSON with [kaml](https://github.com/davils-com/kaml),
uses [krypt](https://github.com/davils-com/krypt) for digests, JOSE and mutual TLS and
[kontinuity](https://github.com/davils-com/kontinuity) for resilience, and is built on
[Kore](https://github.com/davils-com/kore). It is published under the Apache License 2.0 and is built for enterprise
and critical infrastructure, with security and correctness first.

## Repository

| Module | Artifact | Purpose |
|---|---|---|
| `kapi-spec/kapi-spec-core` | `com.davils:kapi-spec-core` | What every API document shares: sources, loading through kaml within limits, `$ref` resolution with confinement, resolvers, located diagnostics, the severity policy, overlays and writing |
| `kapi-spec/kapi-spec-schema` | `com.davils:kapi-spec-schema` | JSON Schema 2020-12 with the dialects of OpenAPI 3.0 and 3.1: a bounded validator over YAML nodes and JSON elements and format assertions, matching patterns with the linear matcher of `kapi-runtime-core` |
| `kapi-spec/kapi-spec-openapi` | `com.davils:kapi-spec-openapi` | OpenAPI 3.0, 3.1 and 3.2: the model, the strict reader, the upgrade of Swagger 2.0, the rules, the structure check against the official schemas and the comparison of two versions |
| `kapi-spec/kapi-spec-asyncapi` | `com.davils:kapi-spec-asyncapi` | AsyncAPI 3: the model, the strict reader and the rules, with the WebSocket binding |
| `kapi-spec/kapi-spec-testing` | `com.davils:kapi-spec-testing` | Test support for readers of documents: a corpus of OpenAPI documents, hostile documents and an in-memory resolver |
| `kapi-runtime/kapi-runtime-core` | `com.davils:kapi-runtime-core` | What generated code runs on: codecs, wire types, parameter styles, headers, content negotiation, credentials, validation with a linear pattern matcher, problem details, HTTP semantics and the destination policy |
| `kapi-runtime/kapi-runtime-client` | `com.davils:kapi-runtime-client` | Calls over the Ktor `HttpClient` of the application: credentials and OAuth 2, streams, server-sent events, pages, resilience, telemetry and WebSocket sessions |
| `kapi-runtime/kapi-runtime-server` | `com.davils:kapi-runtime-server` | The Ktor server plugin, for the JVM only: requests read and checked as a whole, negotiation, streaming, authorization, problems and WebSocket channels |
| `kapi-runtime/kapi-runtime-testing` | `com.davils:kapi-runtime-testing` | Test support for generated code: fake credentials, mock engines and contract assertions |
| `kapi-codegen/kapi-codegen-core` | `com.davils:kapi-codegen-core` | The generator, for the JVM only: audits a document, names what it declares and emits Kotlin with KotlinPoet, with the provenance of every file |
| `kapi-codegen/kapi-codegen-gradle` | `com.davils:kapi-codegen-gradle` | The Gradle plugin `com.davils.kapi`: specifications, generation in an isolated worker, lint, compatibility checks and reports |
| `kapi-bom` | `com.davils:kapi-bom` | The bill of materials of every module |
| `build-logic` | - | One convention plugin per concern, composed by `kapi.kotlin-library` for a library module, `kapi.jvm-library` for a module only the JVM offers, `kapi.gradle-plugin` for the Gradle plugin, `kapi.bom` for the BOM and `kapi.root` for the root project; they hold nothing but `plugins { }` and `kreate { }` |

The code is ported from the OpenAPI stack of an internal Davils project and hardened and extended on the way.

The modules are grouped by role: `kapi-spec` reads and checks documents, `kapi-runtime` is what applications and
generated code depend on, and `kapi-codegen` turns documents into Kotlin. Every module carries the name of its group,
and its Gradle path is `:<group>:<module>`, such as `:kapi-runtime:kapi-runtime-client`. Types carry no prefix of the
specification they belong to: the package says where they come from, such as `com.davils.kapi.spec.openapi.model.path.Operation`.
Each specification has exactly one entry point, such as `OpenApi`, `AsyncApi` and `JsonSchema`.

The package of a module is `com.davils.kapi.<module>` with every dash a dot, such as `com.davils.kapi.runtime.client`,
and its Java module has the same name. It is declared in `src/jvmMain/java/module-info.java` of a multiplatform module
and in `src/main/java/module-info.java` of a JVM module, which exports only packages with public API, never an
`internal` package.

## Targets

Every module of `kapi-spec` and `kapi-runtime-core`, `kapi-runtime-client` and `kapi-runtime-testing` are Kotlin
Multiplatform with two targets: `jvm` and `wasmJs`. The Wasm tests run under Node.js. `kapi-runtime-server` and the
modules of `kapi-codegen` are for the JVM only.

- Code lives in `src/commonMain/kotlin`. What differs per platform hides behind an `internal expect fun` in
  `<Role>.kt` with its `actual` in `<Role>.jvm.kt` and `<Role>.wasmJs.kt`, and a parity test proves that both targets
  behave the same. kapi declares no `expect` or `actual` class: they are a Beta language feature.
- I/O is `suspend`. kapi never chooses a dispatcher and never starts a thread or a timer of its own; it runs in the
  scope of the caller or of Ktor.

## Verify

```bash
./gradlew check
```

Nothing is done until it is green. `check` compiles with warnings as errors (Kotlin with progressive mode and extra
warnings, javac with `-Xlint:all -Werror`), runs Detekt with type resolution, the Kreate rule set and `allRules` over
the main and the test sources, the tests on the JVM and on Wasm, Kover verification at 97 % of lines, branches and
instructions, the API and semver check, the license header check, the changelog check, every Trivy scan, the
standards audit and the Kreate assurance gate, which blocks on every finding. The gate reads OSV.dev and deps.dev and
Trivy downloads its database, so `check` needs the network. The build runs on JDK 25; toolchains are never downloaded.

## Code rules

Enforced by Detekt with the Kreate rule set, the `libraries` rule set, `allRules` and warnings as errors, without a
baseline, for main and test sources alike. Build scripts and `build-logic` follow the same rules.

- **License header first.** Every `.kt`, `.kts`, `.java`, `.properties` and YAML file starts with the Apache 2.0
  header, byte for byte as in the existing files.
- **No comments.** No `//`, no `/* */` other than the license header. Names, types and tests carry the intent.
- **KDoc on public API only**, with `@param`, `@return`, `@throws` and `@since`. A new declaration gets the version it
  first ships in; existing `@since` values never change. KDoc states behaviour on invalid input, the limits that apply,
  thread safety and every security consideration.
- **No `else`.** Return early; a `when` over a sealed type or an enum is exhaustive without `else ->`. At most two
  `return` statements per function.
- **At most two chained calls.** Name intermediate results and computed conditions.
- **Named arguments** for every call with more than three arguments or with several arguments of one type.
- **One top-level type per file**, named after it; a file of functions is named after its role.
- **No package for a single file**, except the root package and `internal` packages.
- **No `!!`, no `lateinit`.**
- **Explicit API.** Everything is `internal` unless a consumer needs it. What another kapi module or generated code
  needs but a consumer does not is public and annotated with Kore's `@DavilsInternal`.
- **Names.** No type, file, function or property carries the name of the library as a prefix: `Document`, never
  `KapiDocument`. The ids of the Gradle plugins keep their `kapi.` prefix, the published plugin is `com.davils.kapi`,
  and the extension keywords of documents are `x-kapi-*`.
- **Immutable values.** Public types are immutable. No public `data class`: a public value writes `equals`,
  `hashCode` and `toString` by hand and is listed in the `UseDataClass` excludes of `detekt.yaml`; `value class` stays
  allowed.
- **Kotlin first.** Suspending calls are the primary API, configuration is a DSL of `ValidatingBuilder`s, and typed
  results are `Outcome`s of Kore or sealed results with sealed failures. Atomics come from `kotlin.concurrent.atomics`
  through a local `@OptIn(ExperimentalAtomicApi::class)`; experimental API never appears in a public signature.
- **Nothing global.** No mutable singletons, no static configuration, no global registry, no global caches, no
  threads or timers of the library's own.
- **No logging.** kapi has no logger and no log types. Telemetry runs only through events on konfetti topics and
  meters and spans in kollect.

## Document rules

- **No network and no file system in the library.** Documents are read through suspending resolvers the caller
  implements. A remote reference is refused unless the caller configures a resolver for it; the file resolver lives in
  the JVM generator of `kapi-codegen-core` and stays confined to its root.
- **Every finding is located.** A diagnostic names its code, its severity, the source, the JSON Pointer and the line
  and column, and shows untrusted text through `SafeText`. Codes are stable API.
- **Nothing is guessed.** What a document leaves open, kapi reports instead of assuming; what the generator cannot
  express faithfully, it refuses with a diagnostic instead of emitting something weaker.

## Generator rules

- **Generated code is deterministic**: the same document, options and generator version yield the same bytes on every
  machine, with the provenance of every file.
- **Generated code is safe**: every name from a document becomes a valid Kotlin identifier or is refused, every text is
  escaped for its sink (KDoc, string literal, template), and no document can inject code.
- **Generated code keeps the rules** of this file: explicit API, no `else`, no `!!`, no `lateinit`, no comments other
  than KDoc, and one top-level type per file. Golden tests compile it and check it.
- **The runtime is the contract.** Generated code only calls public API of the runtime modules, so a runtime fix
  reaches every generated client and server without regeneration.

## Security rules

Every input of kapi may come from an attacker: documents and overlays, request and response bodies, headers,
parameters, cookies, tokens, server-sent events and WebSocket frames.

- **Limits before work.** Every reader and decoder has limits with hard ceilings: document size, depth, nodes,
  aliases, references, schema evaluation, pattern size, body size, line size, frame size and the number of parts,
  fields and headers. Nothing is truncated silently.
- **No input decides the complexity.** Grammars are hand-written parsers, never regular expressions of the platform.
  Patterns of JSON Schema run in kapi's own linear matcher. Maps keyed by input are bounded and use `OrderedMap` or
  `OrderedIndex` of Kore, because the hash maps of Wasm have no tree bins.
- **Destinations are checked.** Every URL a client calls, follows or delivers to passes the destination policy:
  schemes, hosts, private networks and every redirect hop. Credentials never leave the origin they belong to.
- **Escape untrusted text exactly once, at the sink.** Every `toString`, problem detail and exception message that
  shows text from a caller writes it through `SafeText` of Kore. Sensitive headers and fields are masked.
- **Foreign code is contained.** Handlers, resolvers, interceptors and authenticators of the caller run inside
  `contained` or `attempted` of Kore.
- **Hide what Java must not reach.** The runtime lives in `internal` packages that `module-info.java` never exports.
- **Every reader and decoder has hostile tests**: truncated, overlong, malformed and nested input, every limit at the
  limit and one past it, and the examples and test suites of the standards.

## Tests

- Kotest `FunSpec` only, Kotest assertions only, no mocking library.
- Tests live in `src/commonUnitTest/kotlin`, which runs on the JVM and on Wasm, in the package of the code they test.
  Tests that need the JDK or a real Ktor server live in `src/jvmUnitTest/kotlin` or, in a JVM module,
  `src/unitTest/kotlin`. A `fixture` folder holds at least two files. Shared test support lives in
  `kapi-spec-testing` for documents and in `kapi-runtime-testing` for generated code, never in main code.
- The generator has golden tests: the emitted files byte for byte, their compilation with the Kotlin compiler and
  scenarios that run generated clients against generated servers.
- Every change ships with its tests; a fixed weakness ships with a test that would have caught it.

## Build

- A library module applies exactly one plugin, `id("kapi.kotlin-library")`, `id("kapi.serialization-library")` for a
  multiplatform module with `@Serializable` types or, for a module only the JVM offers, `id("kapi.jvm-library")` or
  `id("kapi.jvm-serialization-library")`, and declares nothing but its description and its dependencies; the Gradle plugin applies
  `id("kapi.gradle-plugin")` and declares its `gradlePlugin { }` block and the Gradle API and TestKit for its tests
  as well, and the BOM applies `id("kapi.bom")`. The Gradle plugin is compiled against the Kotlin that Gradle embeds
  and therefore needs Gradle 9.9 or later; it depends on nothing but the Gradle API at runtime, and the generator runs
  in a worker process on a class path of its own.
- `build-logic` holds no Kotlin classes, no tasks and no Gradle API calls beyond reading the version catalog. Every
  plugin configures one concern and applies `kapi.base`:

  | Plugin | Configures |
  |---|---|
  | `kapi.base` | The Kreate plugin, the group and the version |
  | `kapi.dependency-locking` | Strict locking of every configuration |
  | `kapi.git-hooks` | The Git hooks |
  | `kapi.trivy` | Every Trivy scan |
  | `kapi.assurance` | The assurance gate and its `ignore` rules |
  | `kapi.standards` | The standards kapi follows, reviewed in `standards.lock` |
  | `kapi.platform` | The JDK, the Kotlin compiler, the targets and the Java module name |
  | `kapi.web-tools` | Node.js and Yarn for the Wasm target |
  | `kapi.testing` | Kotest and the test suites |
  | `kapi.detekt` | Detekt and its reports |
  | `kapi.coverage` | Kover and its verification |
  | `kapi.api-validation` | The API dump and the semver check |
  | `kapi.docs` | Dokka |
  | `kapi.publish` | The POM, the release guard and the Davils repository |
  | `kapi.archives` | The legal files and the manifest of every archive |
  | `kapi.tool-classpaths` | The version floors on the class paths of the build tools |
  | `kapi.changelog` | The changelog check |
  | `kapi.license-header` | The license header check |
  | `kapi.serialization` | The kotlinx.serialization compiler plugin, composed by `kapi.serialization-library` and `kapi.jvm-serialization-library` |
  | `kapi.gradle-plugin` | `java-gradle-plugin` on top of `kapi.jvm-library`, for `kapi-codegen-gradle`; its content inspection covers only `runtimeClasspath`, what the plugin ships, because the Kotlin Gradle plugin it compiles and tests against is never shipped (advisories, trust and confusion still cover every class path) |
  | `kapi.bom` | The Java platform of the BOM and its publication |

- Dependencies are declared in `gradle/libs.versions.toml`. Libraries of JetBrains (Ktor and kotlinx included) and of
  Davils (Kore, kaml, konfetti, kollect, kontinuity, krypt) may be added when they are needed; every other dependency
  needs the approval of a maintainer. Approved: KotlinPoet, in `kapi-codegen-core` only.
- Only `repo.davils.com` serves `com.davils`, through `exclusiveContent`; nothing is resolved from `mavenLocal()`.
- After a dependency changes, refresh the locks and the verification metadata in one run and review what changed:

  ```bash
  ./gradlew kreateResolveAndLockAll :build-logic:dependencies --write-locks --write-verification-metadata pgp,sha256 --export-keys --no-configuration-cache
  ```

- kapi builds only with released versions of Kreate and the Davils libraries, trusted by the Davils release key
  `36F578E8AD2D4391E0FA6C18154C625FAD370063`; no trust rule for unsigned snapshots is ever added.
- An `ignore` rule of the assurance gate has a reason, the exact module and an expiry no more than six months ahead.
  CRITICAL and HIGH findings can never be ignored.
- Every standard kapi names is declared in `build-logic/src/main/kotlin/kapi.standards.gradle.kts` and reviewed with
  `./gradlew kreateStandardsReview --standard=<id> --note="..."`. Prefer `rfc()`, `draft()` and `github()` over
  `document()`, whose fingerprints differ between machines.
- A new published module is also added to `settings.gradle.kts`, the `aggregate` of `kapi.assurance`, the BOM,
  `.github/CODEOWNERS` and every list of `.github/kotlin-workflow.yml` (reproducible, publish, release, attest), or the
  release silently skips it.

## Kore internals

kapi opts into these `@DavilsInternal` declarations of Kore; Kore changes them only in a major version:
`SafeText.text`, `SafeText.quoted`, `SafeText.REDACTED`, `UriReference` for document URIs and references,
`parseDecimalOrNull` for exact numbers, and `OrderedMap`, `OrderedSet` and `OrderedIndex` for the maps and sets keyed
by input. A new use is added here and to the table of Kore's `AGENTS.md`.

## Public API

The `api/*.api` dumps are the contract. `kreateApiCheck` fails on every unrecorded change and `kreateApiSemverCheck`
on a version that does not match it. A deliberate change is recorded with `./gradlew kreateApiDump` and gets a
`CHANGELOG.md` entry. A breaking change needs a major version and a commit marked as breaking. Deprecate before
removing: `@Deprecated` with a `ReplaceWith`, for at least one minor release. Every module carries the same version.

## Releases

The version lives in `gradle.properties` and nowhere else; `kreateReleaseGuard` refuses every other version. Every
build without a release tag is a `-SNAPSHOT`. A release is a tag without a `v`, such as `1.0.0`, on a commit of `main`.
Snapshots go to `https://repo.davils.com/snapshots`, releases to `https://repo.davils.com/releases`, never to Maven
Central. The pipeline publishes with `publishAllPublicationsToDavilsRepository --no-configuration-cache` and signs
with `ORG_GRADLE_PROJECT_signingInMemoryKey`, `…KeyId` and `…KeyPassword`.

## Git

Work on `develop`, release from `main`. Conventional Commits in English with the module as scope, such as
`feat(openapi): ...` or `build: ...`. Small commits, one concern each. Commit only when asked. No `Co-Authored-By`
trailer and no other attribution to tools or AI assistants.
