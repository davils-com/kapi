# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

### Security

- **Patterns of JSON Schema** charge the evaluation budget for every state of the matcher and every character of the
  text, so many large patterns over a long string are refused by `maximumEvaluationSteps` instead of running for
  hours within the default budget.
- **Generated AsyncAPI payload models** write the origin of their schema only into the `from` of `@DavilsGenerated`,
  escaped as a string literal: a pointer of a document with `*/` in it can no longer close a KDoc and inject Kotlin
  into the generated code.
- **Overlays** can no longer crash a reading or grow a document past its limits: an action whose JSONPath or patch
  goes past the limits of kaml is left out with `openapi.overlay.update` instead of throwing, an update that every
  match would copy past `maximumNodes` is refused before it is built, and an action that grows the document past
  `maximumNodes` or `maximumDepth` is left out; merging an update no longer hashes the keys of the document.
- **Pattern compilation** finishes at once for a repeated body without states, such as `(?:a{0}){999999999}`, which
  never finished before, and counts the branch of every optional copy, so `(?:){0,999999999}` can no longer slip past
  the limit of states.
- **Credentials in headers of their own**, such as an API key in `X-Api-Key`, are dropped like `Authorization` when
  the client follows a redirect to another origin, so a redirect can no longer carry them to a host they do not
  belong to.
- **Path parameters** of the server runtime are read from the route only: a query field named like a path parameter,
  such as `DELETE /accounts/me?accountId=victim`, can no longer replace the value of the path that routing, gateways
  and authorizers saw.

### Added

- **The compatibility check of the Gradle plugin**: `openApi("pets") { compatibility { baseline = file(...);
  breaking = CompatibilitySide.Clients; acknowledge(code, reason) } }` registers `kapiCompatibility<Name>`, which
  `kapiCompatibility`, `kapiCheck` and `check` run. It reads the document and its baseline with the confined
  resolver of the document, compares them with `OpenApi.compare` or `AsyncApi.compare` in the isolated worker of the
  generator through `Launcher.compare`, and fails on every change that breaks the chosen side unless it is
  acknowledged with a reason; every other change is a note, with the level `note` in the SARIF log. Without a
  baseline the task is skipped.
- **The build** of kapi with Kreate 5.4.1: one module per concern, one convention plugin per concern, strict
  dependency locking and verification, Detekt with every rule over main and test sources, Kover at 97 %, the API and
  semver check, Trivy, the standards audit and the assurance gate.
- **The client runtime** `kapi-runtime-client`: `ApiClient` and the `apiCall` DSL over the Ktor `HttpClient` of the
  application, with an interceptor chain, credentials, the destination policy for every URL and redirect hop,
  controlled redirects, retries with `Retry-After`, `RateLimit` and idempotency keys through kontinuity, conditional
  and range requests, streams, server-sent events with resumption, NDJSON and JSON text sequences, pages by cursor,
  offset, number and `Link`, multipart and form bodies, OAuth 2 token sources (client credentials, refresh,
  authorization code with PKCE, device authorization, token exchange, JWT client authentication, DPoP, discovery and
  revocation), WebSocket channel sessions, and spans, meters and events in kollect and konfetti.
- **The generator** `kapi-codegen-core`: `Codegen.openApi { }` turns an OpenAPI 3.0, 3.1 or 3.2 document into
  Kotlin on the kapi runtimes, with the provenance of every file: models with kotlinx.serialization, Kore validation
  and the wire types of the core runtime; clients on `ApiClient` with a sealed result per operation, typed
  `NotModified` for conditional requests, streams, server-sent events and pages; servers as `Routes` with a service
  interface and sealed responses, mounted with `mount(...)`; mocks on the server runtime; senders and receivers of
  webhooks and callbacks. The `QUERY` method and `querystring` parameters of OpenAPI 3.2 are generated, and every
  text of a document is escaped where it is written, invisible characters included.
- **The launch contract** of the generator for the Gradle plugin: a run reads an OpenAPI or an AsyncAPI document,
  chosen by `LaunchKind`, through a resolver confined to its root, and `Launcher.lint` audits a document exactly as a
  generation does but writes only the JSON report and the SARIF log, with the outcome `Audited`.
- **The Gradle plugin** `com.davils.kapi` (`kapi-codegen-gradle`): `kapi { openApi("petstore") { } }` and
  `asyncApi("chat") { }` register `kapiGenerate<Name>`, which generates in a process-isolated worker into
  `commonMain` and `jvmMain` of a multiplatform project or `main` of a JVM project and adds the runtimes, and
  `kapiLint<Name>`, which audits without writing code; `kapiLint` and `kapiCheck`, which `check` runs, aggregate
  them. Both tasks are cacheable and relocatable, write a JSON report and a SARIF log, and fail on every error, every
  unacknowledged warning and every acknowledgement without a reason or a matching warning. `generatorClasspath` and
  `runtimes { enabled = false }` let a build bring its own generator and runtimes.
- **The structure check of AsyncAPI**: `StructureCheck.ReaderAndOfficialSchema`, the default of `AsyncApi.reader {
  structure = ... }`, validates the root document against the official JSON Schema of AsyncAPI 3.0.0
  (`asyncapi/spec-json-schemas` 6.11.1, embedded unchanged and recorded in `NOTICE`) and reports
  `asyncapi.structure.schema` only where the reader has not reported yet, leaving references, boolean schemas and
  schemas of other formats, such as Avro, to the reader. The rules add `asyncapi.operation.key-duplicate` for one
  operation listed under several keys of
  `operations` and `asyncapi.security.reference-placement` for a security requirement that refers outside
  `components/securitySchemes`.
- **The comparison of two versions of a document** in `kapi-spec-core`: `Comparison` of `Change`s, each with a stable
  code, a message, its places in the baseline and the candidate and its `Impact` (`Breaking`, `NonBreaking` or `Info`)
  for clients and for providers, configured by `ComparisonBuilder` within `ReadLimits`, and the `@DavilsInternal` JSON
  Schema comparison `SchemaComparison` over `SchemaView`s, with `FixedSchemaView` for boolean schemas, that OpenAPI
  and AsyncAPI share: types and nullability,
  bounds, patterns, formats, enums, constants, properties, required properties, additional properties, items and
  alternatives, judged by the direction the data flows in, bounded in depth, reference hops, steps and changes, and
  safe against cyclic schemas.
- **The comparison of two OpenAPI documents**: `OpenApi.compare(baseline, candidate) { limits { } }` reports, with the
  codes of `CompatibilityCodes` (`openapi.compatibility.<rule>`), removed and added servers, paths and operations,
  renamed path parameters, changed operation ids, deprecations, removed, added, newly required and optional parameters
  and their serialization, request bodies and their media types, removed and added response statuses, media types and
  headers, security that is added, removed or needs other scopes, changed security schemes, and every change of the
  schemas of parameters, bodies and headers, judged as requests or responses. References are followed through the
  targets of each document within the limits of a reading.
- **The comparison of two AsyncAPI documents**: `AsyncApi.compare(baseline, candidate) { limits { } }` reports, with
  the codes of `CompatibilityCodes` (`asyncapi.compatibility.<rule>`), removed and added servers, channels, operations
  and messages, changed server and channel addresses, actions, channels of operations, content types and bindings,
  security that is added or removed, and every change of the payloads and headers of messages, with traits applied,
  judged by whether the application sends or receives them; schemas of other formats, such as Avro, are compared as
  written.

- **Schema rules for every input of a server**: generated servers check `minimum`, `maximum`, `multipleOf`,
  `minLength`, `maxLength`, `pattern`, `minItems`, `maxItems`, `uniqueItems`, the values of an enum and the rules of
  every item for parameters, the whole query string, the parts of a multipart body and the fields of a form before the
  service is asked, through the optional `rule: ValidationScope.(T) -> Unit` of `Input.value`, `items`, `fields`,
  `queryString`, `part`, `partItems` and `field`, and answer a violation with a problem with status 400 under the path
  of the input, such as `query.limit`.
- **Methods as written** (RFC 9110 §9.1) in every runtime: `apiCall` sends a method exactly as written and refuses
  one that is no token, `Operation` and `Request.of` serve any token, so a custom method `purge` of
  `additionalOperations` is no `PURGE`, and the mock API and the contract check match methods exactly.
- **The linear pattern engine** of `kapi-runtime-core`, `com.davils.kapi.runtime.core.pattern`: `Pattern.compileOrThrow`
  compiles a pattern once and `Constraints.matches` checks a text against it, so JSON Schema, generated models and the
  samples of mocks never run a `Regex` of the platform on a pattern of a document.
- **Shared HTTP helpers** of the core runtime: `FieldSyntax` for tokens and field values, `HttpStatus` with the status
  codes and their classes, `Headers.joined` and `Codecs.decode` for a parsed `MediaType`.
- **Typed streams, parts and channels** in the server runtime: `ReplyBuilder.events(serializer, items)` for typed
  server-sent events, `Input.part`, `Input.partItems` and `Input.field` that decode through the codecs of the server,
  an `admission` hook on `ChannelEndpoint` that refuses an invalid handshake before the upgrade,
  `ChannelSession.request` with a `Correlation` and `ChannelLimits.maxHeldMessages`, and `Request.of { }` to build a
  request without Ktor.
- **Query strings, correlation and byte ranges** in the client runtime: `CallBuilder.queryString` for a `querystring`
  parameter of another media type than a form, `ChannelSettings.toBuilder`, `Correlation.json` over the JSON of
  messages, DPoP proofs on WebSocket handshakes, `CallFailure.Unsent` for an interceptor that never sends, and
  `RangeOutcome.Ranges.parts` with the parts of a `multipart/byteranges` answer.
- **Server tests without Ktor**: `Routes.call` of `kapi-runtime-testing` calls the operations of generated server
  routes with a request of `Request.of`, and `Reply.content`, `Reply.text`, `Reply.mediaType` and `Reply.decoded` read
  the reply.
- **Closing builders**: every builder of the DSLs closes after its first `build()` or `validate()`, so a change
  afterwards throws an `IllegalStateException` instead of being lost silently.
- **Generated models** are regular classes with hand-written `equals`, `hashCode` and `toString`, which masks every
  property marked `x-kapi-sensitive`, and a `copy` with named parameters; a pattern the engine refuses is refused at
  generation with a located `codegen.schema.unsupported`.
- **Generated KDoc** from the `title`, `summary` and `description` of a document for models, properties, enums,
  unions and the functions of clients, senders and services, escaped for KDoc.
- **OpenAPI 3.2 in the generator**: a `querystring` parameter of a form or of JSON, and the operations of
  `additionalOperations` with their method exactly as the document names it.
- **Generated servers** stream typed server-sent events, read parts and fields through the codecs of the server and
  validate them under `body.<name>`.
- **Generated WebSocket channels** check the parameters of a handshake before the upgrade, generate
  `request<Operation>` for a request the server sends with a reply and correlation ids, and pair the requests of a
  client with `Correlation.json`, which reads each id by the discriminator of its message.
- **Safe diagnostics**: the generator shows every text of a document quoted and escaped through `SafeText` of Kore,
  exactly once, and locates every finding, also of a partial document.
- **Balanced maps for document input**: the specification modules and JSON Schema keep every map keyed by document
  input in Kore's balanced trees, which Wasm needs against keys whose hash codes collide, and hold every public map,
  list and set as an immutable copy; `KeyedMap`, `frozen()`, `frozenSet()` and keyed counterparts of `toMap`,
  `associate`, `associateBy`, `groupBy`, `distinctBy` and `filter` are `@DavilsInternal` API for the other modules,
  and `HostileDocuments.collidingKeys` and `collidingKeysDocument` of `kapi-spec-testing` test them.
- **Shared resolution of references**: `UriResolution`, `PointerTree` and the views of the comparison live in
  `kapi-spec-core` as `@DavilsInternal` API that the readers of OpenAPI and AsyncAPI and the generator share.
