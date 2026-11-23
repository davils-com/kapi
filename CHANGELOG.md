# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

### Added

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

### Changed

- **The linear pattern engine** moved from `kapi-spec-schema` to `kapi-runtime-core` as
  `com.davils.kapi.runtime.core.pattern`, so generated models validate `pattern` with it: `Pattern.compileOrThrow`
  compiles a known-good pattern once and `Constraints.matches` checks a text against it, reporting
  `kapi.runtime.constraint.pattern`.
- **The core runtime** shares what the other runtimes duplicated: `FieldSyntax` (tokens and field values) and the
  `bounded` limit check as `@DavilsInternal` API, `HttpStatus` with the status codes and their classes,
  `Headers.joined` for the combined value of every line of a field and `Codecs.decode` for a parsed `MediaType`.
- **The server runtime** reads the shared grammar, limit check and status codes of the core runtime instead of copies,
  and adds `ReplyBuilder.events(serializer, items)` for typed server-sent events, `Input.part`, `Input.partItems` and
  `Input.field` that decode parts and form fields through the codecs of the server, an `admission` hook on
  `ChannelEndpoint` that refuses an invalid handshake before the upgrade, `ChannelSession.request` with a
  `Correlation` and `ChannelLimits.maxHeldMessages`, `Request.of { }` to build a request without Ktor and
  `ReplyBody.Value.encoded`.
- **The client runtime** adds `CallBuilder.queryString` for a `querystring` parameter of another media type than a
  form, encoded strictly, `ChannelSettings.toBuilder`, `Correlation.json` over the JSON of messages, DPoP proofs on
  WebSocket handshakes, `CallFailure.Unsent` for an interceptor that hands back a trip that was never sent, and
  `RangeOutcome.Ranges.parts` with the parts of a `multipart/byteranges` answer.
- **The test support** calls the operations of generated server routes without Ktor on the JVM: `Routes.call` with a
  request of `Request.of` or one it builds, and `Reply.content`, `Reply.text`, `Reply.mediaType` and `Reply.decoded`
  to read the reply.
- **Every builder** of the DSLs closes after its first `build()` or `validate()`: a setter or a function that changes
  it afterwards throws an `IllegalStateException` instead of losing the change silently.
- **Generated models** are regular classes instead of data classes: they write `equals`, `hashCode` and `toString`
  by hand, mask every property marked `x-kapi-sensitive` in `toString` and offer `copy` with named parameters, but no
  `componentN`.
- **Generated patterns** run in the linear engine of `kapi-runtime-core`: a model compiles each pattern once into a
  private `Pattern` of its file and checks it with `Constraints.matches`, never a `Regex` of the platform, and a
  pattern the engine refuses is refused at generation with a located `codegen.schema.unsupported`; the samples of
  mocks match patterns with the engine as well.
- **Generated KDoc** describes models, their properties, enums and unions with the `title` and `description` of their
  schemas and the functions of operations in clients, senders and services with their `summary` and `description`,
  escaped for KDoc: no `*/` or `/*`, no tags, invisible and bidirectional characters as escapes.
- **OpenAPI 3.2 in the generator**: a `querystring` parameter of JSON, `application/json` or a `+json` type, is sent
  with `CallBuilder.queryString` and read with `Input.queryString` of its media type, beside the form it already
  supported; the operations of `additionalOperations` are generated for clients, senders and servers with their
  method as the document names it, and a key that is no method token in upper case is refused with a located
  `codegen.operation.unsupported`.
- **Generated servers** stream typed server-sent events with `ReplyBuilder.events(serializer, items)` and read the
  parts of a multipart body and the fields of a form with `Input.part`, `Input.partItems` and `Input.field`, so every
  value is decoded by the codecs of the server, validated and reported under `body.<name>`; repeated parts of numbers
  or flags are served instead of refused.
- **Generated WebSocket channels** check the parameters of a handshake in the `admission` of their
  `ChannelEndpoint` and refuse an invalid one with a problem with status 400 before the upgrade; a request the server
  sends with a reply and correlation ids becomes `request<Operation>` on the server session over
  `ChannelSession.request` and the `correlation` of the endpoint, and clients pair their requests with
  `Correlation.json`, reading each id from the JSON of a message by its discriminator, so ids inside union payloads
  are found on every platform. The correlation object of a channel moved to the `channel` package.
