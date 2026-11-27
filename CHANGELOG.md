# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 1.1.0 - 2026-11-27

### Added

- **`kapi-runtime-server-ipc`, the peers of local connections.** The plugin `IpcPeers` hands the process on the other
  side of a Unix domain socket or a Windows named pipe of `konnect-ipc-server` to the kapi server as
  `PeerAttributes.identity`, so an authorizer trusts `PresentedCredential.Peer`; `IpcPeer.toPeerIdentity()` names its
  user and process.
- **Every operation is protected by default.** A bulkhead of kontinuity lets one operation serve at most
  `ServerLimits.maxConcurrentRequests` (512 by default) requests at once and refuses the next one at once with
  `503 Service Unavailable` and `kapi.server.overloaded`. `ServerSettingsBuilder.rateLimit(requests, per)` limits each
  caller, its principal or else its address, with a keyed rate limiter of kontinuity and answers `429 Too Many
  Requests` with `kapi.server.rate-limited` and a `Retry-After`.
- **Cached keys and an in-memory replay guard.** `KeySource.cached(source, lifetime)` hands out the keys of a source,
  such as a JWKS, for their lifetime from a cache of kontinuity, loads them once for every caller and keeps no
  failure; `ReplayGuard.inMemory(maxEntries)` remembers accepted signatures until they expire.
- **A client retries by default.** Without its own `resilience`, a client runs its retryable calls under a retry of
  `CallRetry` with three attempts, a jittered exponential backoff and the waits the server asks for, reporting through
  the meters, the tracer and the event bus of the client; `isDefaultResilience = false` tries every call once.
- **Addresses of Konnect.** `IpAddress.toKonnect()` and `IpAddress.of(address)` convert to and from the addresses of
  Konnect.

### Changed

- **The destination policy reads and classifies addresses with Konnect 1.2.0.** The parser of IP addresses and the
  list of blocks that are not public are gone; an address counts as public when Konnect finds it globally reachable
  in the IANA special-purpose registries, and an IPv4 address embedded in a mapped, a NAT64 or a 6to4 address is
  still judged by itself.
- **The formats `ipv4` and `ipv6` of JSON Schema are checked by Konnect.**
- **The events of the client, of OAuth and of the server reach the bus through the `Emitter` of konfetti 1.1.0.**
- **Built with Kreate 5.5.2 and Gradle 9.9.2 on Kore 2.5.0, konfetti 1.1.0, kollect 1.1.0, kaml 1.0.1, krypt 1.2.0,
  kontinuity 1.2.0 and Konnect 1.2.0**, with Ktor 3.6.1 and kotlinx-io 0.9.3.

## 1.0.0 - 2026-11-24

The first version of kapi: the API toolkit of Davils as an open source library under the Apache License 2.0. It reads
OpenAPI 3.0, 3.1 and 3.2, Swagger 2.0 and AsyncAPI 3 strictly, validates JSON Schema 2020-12, compares two versions of
a document and generates typed clients, servers, models, mocks, webhooks and WebSocket channels on top of Ktor 3.6, for
Kotlin on the JVM and on `wasmJs`, built with Kreate 5.4.1 on Kore 2.3.2, kaml 1.0.0, krypt 1.0.0, kontinuity 1.0.0,
konfetti 1.0.0 and kollect 1.0.0 and published to `https://repo.davils.com`. The code comes from the OpenAPI stack of an
internal Davils project and is hardened for critical infrastructure on the way, including an independent security
audit of every module before this release.

### Added

- **The reading of documents** `kapi-spec-core`: text and located sources, suspending resolvers of the application
  with remote references refused unless a resolver allows them, loading through kaml within `ReadLimits` and their
  hard ceilings, `$ref` resolution with confinement, hop and document limits and cycle detection, diagnostics that
  name their code, severity, source, JSON Pointer, line, column and snippet, the severity policy, Overlays 1.0 and the
  writing of documents.
- **JSON Schema** `kapi-spec-schema`: `JsonSchema` compiles and validates JSON Schema 2020-12 with the dialects of
  OpenAPI 3.0 and 3.1 over YAML nodes and JSON elements, within budgets for steps, depth, instance size and errors,
  with format assertions, `$ref` and `$dynamicRef`, `unevaluatedItems` and `unevaluatedProperties`, the outputs
  `Flag` and `Basic`, and patterns in the linear matcher of the core runtime.
- **OpenAPI** `kapi-spec-openapi`: `OpenApi.reader()` reads OpenAPI 3.0, 3.1 and 3.2 strictly and upgrades Swagger
  2.0, with the model, typo suggestions for unknown fields, the rules, the check against the official schemas,
  overlays and the writer.
- **AsyncAPI** `kapi-spec-asyncapi`: `AsyncApi.reader()` reads AsyncAPI 3 strictly with the WebSocket binding, with
  the model, the rules, `Document.effective` for operations and messages with their traits applied, and the writer.
- **Test support for readers** `kapi-spec-testing`: a corpus of OpenAPI documents, hostile documents and an in-memory
  resolver.
- **The core runtime** `kapi-runtime-core`: codecs for JSON, CBOR, text, forms, multipart and octet streams within
  limits, parameter styles, header fields such as `Link`, `Retry-After`, `Content-Disposition`, `Cache-Control` and
  `Idempotency-Key`, content negotiation by RFC 9110, entity tags and preconditions, byte ranges, server-sent events,
  JSON Lines and JSON text sequences, digest fields of RFC 9530, problem details of RFC 9457, credentials and
  `Secret`, validation with `Constraints` and exact `Decimal`s, and the destination policy against server-side
  request forgery.
- **The server runtime** `kapi-runtime-server`: the Ktor plugin `install(Kapi)` and `mount(routes)` read and check a
  request as a whole within limits for bodies, header fields, queries, multipart bodies and decompression, negotiate
  the reply, stream bodies and server-sent events, answer conditional and range requests, authorize with API keys,
  Basic, JWTs through krypt, token introspection and client certificates, verify webhooks with HTTP message signatures
  of RFC 9421 and digests of RFC 9530, answer with problem details, serve WebSocket channels and report through
  kollect and konfetti.
- **Test support for generated code** `kapi-runtime-testing`: fake token sources and clocks, mock APIs that record
  every exchange, a fake authorization server, contract checks against a document, and `Routes.call` for generated
  server routes without Ktor.
- **The bill of materials** `kapi-bom` with every module of kapi.
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
- **Documents and JSON Schema hardened after an audit**: overlays can no longer crash a reading or grow a document past
  `maximumNodes` and `maximumDepth`; resolvers of the application run contained and a failure becomes `*.load.failed`;
  a text source is sized before it is encoded; typo suggestions skip fields of a far length; comparisons charge a step
  for every property and stop recording seen changes once full; AsyncAPI traits that would copy more than the
  document may hold are refused with `asyncapi.trait.too-large`; patterns are charged for every state and character
  they run and compile at once for a repeated body without states; annotation tracking is charged for every item it
  tracks; and a validation stops at the error that passes `maximumErrors`.
- **Runtimes hardened after an audit**: path parameters are read from the route only, never from a query field of the
  same name; credentials in header fields of their own are dropped like `Authorization` on a redirect to another
  origin; a cookie credential must be a cookie value; an OAuth 2 authorization response that repeats `code`, `state` or
  `iss` is refused and `iss` is required when the server announces it by RFC 9207 (`ServerMetadata.isIssuerReturned`);
  answers of an authorization server never show their content; codecs of the application run contained; a WebSocket
  channel is refused when the `WebSockets` plugin of Ktor reads frames larger than the channel allows, with
  `CallFailure.ChannelFramesUnbounded` on the client and an exception of `mount` on the server, so set its
  `maxFrameSize`; webhook signatures must cover a `Content-Digest` that matches the body
  (`SignaturePolicy.isContentBound`) and a `ReplayGuard` in `SignaturePolicy.replays` refuses replays; producers of
  streamed replies run contained and as the authorized caller, and a failing one breaks the response; and unique
  strings, numbers and enums are checked by sorting instead of hashing.
- **The generator hardened after an audit**: AsyncAPI payload models write the origin of their schema only into
  `@DavilsGenerated`, so a pointer with `*/` cannot inject code; hidden characters in raw strings keep their value as
  `${'\uXXXX'}` templates; and the file resolver refuses a `file` URI that names a host, which Windows reads as a
  network share, and confines a path to its root before it touches the file system.
