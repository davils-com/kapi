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

### Changed

- **The linear pattern engine** moved from `kapi-spec-schema` to `kapi-runtime-core` as
  `com.davils.kapi.runtime.core.pattern`, so generated models validate `pattern` with it: `Pattern.compileOrThrow`
  compiles a known-good pattern once and `Constraints.matches` checks a text against it, reporting
  `kapi.runtime.constraint.pattern`.
- **The core runtime** shares what the other runtimes duplicated: `FieldSyntax` (tokens and field values) and the
  `bounded` limit check as `@DavilsInternal` API, `HttpStatus` with the status codes and their classes,
  `Headers.joined` for the combined value of every line of a field and `Codecs.decode` for a parsed `MediaType`.
