# kapi

kapi is the API toolkit of Davils for Kotlin: it reads OpenAPI 3.0, 3.1 and 3.2, Swagger 2.0 and AsyncAPI 3 strictly,
checks them with located diagnostics, validates JSON Schema 2020-12, compares two versions of a document for breaking
changes and generates typed clients, servers, models, mocks, webhooks and WebSocket channels on top of
[Ktor](https://ktor.io). The application hands kapi its own Ktor `HttpClient` or installs kapi in its own Ktor server.
kapi parses YAML and JSON with [kaml](https://github.com/davils-com/kaml), uses [krypt](https://github.com/davils-com/krypt)
for digests, JOSE and mutual TLS and [kontinuity](https://github.com/davils-com/kontinuity) for resilience, reports
through events on [konfetti](https://github.com/davils-com/konfetti) topics and meters and spans in
[kollect](https://github.com/davils-com/kollect), and is built on [Kore](https://github.com/davils-com/kore) for use in
enterprise and critical infrastructure. It is published under the [Apache License 2.0](LICENSE).

| Module | Artifact | What it holds |
|---|---|---|
| `kapi-spec-core` | `com.davils:kapi-spec-core` | Sources, loading within limits, `$ref` resolution with confinement, resolvers, located diagnostics, the severity policy, overlays, writing and the comparison of two versions |
| `kapi-spec-schema` | `com.davils:kapi-spec-schema` | JSON Schema 2020-12 with the dialects of OpenAPI 3.0 and 3.1: a bounded validator and format assertions |
| `kapi-spec-openapi` | `com.davils:kapi-spec-openapi` | OpenAPI 3.0, 3.1 and 3.2: the model, the strict reader, the upgrade of Swagger 2.0, the rules, the check against the official schemas and the comparison |
| `kapi-spec-asyncapi` | `com.davils:kapi-spec-asyncapi` | AsyncAPI 3 with the WebSocket binding: the model, the strict reader, the rules, the check against the official schema and the comparison |
| `kapi-spec-testing` | `com.davils:kapi-spec-testing` | A corpus of documents, hostile documents and an in-memory resolver for tests of readers |
| `kapi-runtime-core` | `com.davils:kapi-runtime-core` | What generated code runs on: codecs, parameter styles, headers, negotiation, credentials, validation with a linear pattern matcher, problem details and the destination policy |
| `kapi-runtime-client` | `com.davils:kapi-runtime-client` | Calls over the `HttpClient` of the application: credentials and OAuth 2, streams, server-sent events, pages, resilience, telemetry and WebSocket sessions |
| `kapi-runtime-server` | `com.davils:kapi-runtime-server` | The Ktor server plugin, for the JVM only: requests read and checked as a whole, negotiation, streaming, authorization, problems and WebSocket channels |
| `kapi-runtime-testing` | `com.davils:kapi-runtime-testing` | Fake credentials, mock APIs, contract checks and calls of generated server routes without Ktor |
| `kapi-codegen-core` | `com.davils:kapi-codegen-core` | The generator, for the JVM only: audits a document and emits Kotlin with KotlinPoet, with the provenance of every file |
| `kapi-codegen-gradle` | `com.davils:kapi-codegen-gradle` | The Gradle plugin `com.davils.kapi`: generation in an isolated worker, lint, compatibility checks and SARIF reports |
| `kapi-bom` | `com.davils:kapi-bom` | The bill of materials of every module |

The readers and the client runtime are Kotlin Multiplatform for `jvm` and `wasmJs`, written for Kotlin 2.4 or newer;
generated models and clients run on both targets. The server runtime, the generator and the Gradle plugin need the JVM
and JDK 25; the plugin needs Gradle 9.9 or newer. Every module is a named Java module, such as
`com.davils.kapi.runtime.client`.

## Installation

kapi is published to the Davils repository, not to Maven Central. Declare it for the `com.davils` group only, for
plugins and for dependencies:

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        exclusiveContent {
            forRepository { maven("https://repo.davils.com/releases") }
            filter { includeGroupAndSubgroups("com.davils") }
        }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        exclusiveContent {
            forRepository { maven("https://repo.davils.com/releases") }
            filter { includeGroupAndSubgroups("com.davils") }
        }
        mavenCentral()
    }
}
```

## Generating code

The plugin reads the documents in `src/main/openapi` and `src/main/asyncapi`, generates in a worker process with a
class path of its own and adds the generated sources and the matching runtimes to the build: models and clients to
`commonMain` of a multiplatform project, servers to `jvmMain`, or everything to `main` of a JVM project.

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "2.4.21"
    id("com.davils.kapi") version "1.0.0"
}

kapi {
    openApi("petstore") {
        packageName = "com.example.petstore"
        artifacts {
            client = true
            server = true
        }
        acknowledge("codegen.parameter.reserved-header", "The client sets Accept itself")
        compatibility {
            baseline = file("api/petstore-1.0.0.yaml")
        }
    }
    asyncApi("chat") {
        packageName = "com.example.chat"
    }
}
```

| Task | What it does |
|---|---|
| `kapiGenerate<Name>` | Generates the code of one document; compilation depends on it |
| `kapiLint<Name>`, `kapiLint` | Audit a document exactly as a generation would, without writing code |
| `kapiCompatibility<Name>`, `kapiCompatibility` | Compare a document with its baseline; skipped without one |
| `kapiCheck` | Runs every lint and comparison; `check` runs it |

Every task writes a JSON report and a SARIF 2.1.0 log under `build/reports/kapi/<name>` for code scanning. An error
fails the build; a warning fails it unless the document's `acknowledge(code, reason)` accepts it with a reason, and an
acknowledgement that matches nothing fails it as well, so none outlives what it was written for. A comparison fails on
every change that breaks the clients, or the providers or both with `breaking = CompatibilitySide.Providers`, unless
the `compatibility` block acknowledges it.

Generated code is deterministic, keeps the rules of this repository and only calls public API of the runtimes, so a
fix of a runtime reaches every generated client and server without a new generation. Models are immutable classes with
`copy`, an `equals` and a `toString` that masks properties marked `x-kapi-sensitive`, and check every rule of their
schema, patterns included. Names, documentation and text of a document become valid Kotlin or are refused; no document
can inject code.

## Calling an API

A generated client takes an `ApiClient` over the `HttpClient` of the application and answers every operation with a
sealed result: one type per declared response, the unexpected ones, the malformed ones and the failures of the call.

```kotlin
val api = apiClient(httpClient) {
    baseUrl = "https://api.example.com/v1"
    credentials = credentials { scheme("bearer", tokens) }
    destinations = destinationPolicy { hosts = setOf("api.example.com") }
    resilience = retryPolicy
}

when (val result = PetstorePetsClient(api).listPets(limit = 20)) {
    is PetstoreListPetsResult.Ok -> show(result.body)
    is PetstoreListPetsResult.Other -> report(result.status, result.body.title)
    is PetstoreListPetsResult.Unexpected -> report(result.status)
    is PetstoreListPetsResult.Malformed -> report(result.status, result.reason)
    is PetstoreListPetsResult.Failed -> report(result.failure)
}
```

The client runtime covers what HTTP offers: every parameter style, content negotiation, JSON, forms, multipart, byte
ranges, conditional requests, streams of records and server-sent events with reconnection, pages, idempotency keys,
`Retry-After` and rate limits, redirects followed only under the destination policy, and OAuth 2 with client
credentials, authorization code with PKCE, the device flow, token exchange, private key JWTs, DPoP, mutual TLS and
discovery. Every URL a client calls, follows or delivers to passes the destination policy, and credentials never leave
their origin.

## Serving an API

A generated server is a service interface the application implements and routes it mounts in its Ktor server. The
runtime reads and checks a request as a whole, every parameter, part and field against the rules of its schema, and
answers what breaks the document with a problem detail of RFC 9457 before the service is asked.

```kotlin
class Pets(private val store: PetStore) : PetstorePetsService {
    override suspend fun listPets(limit: Int?): PetstoreListPetsResponse =
        PetstoreListPetsResponse.Ok(store.list(limit ?: 20))
}

fun Application.module() {
    install(Kapi) {
        authorizer("bearer", jwtAuthorizer)
    }
    routing {
        mount(PetstorePetsRoutes(Pets(store)))
    }
}
```

## WebSocket channels

An AsyncAPI document with the WebSocket binding becomes typed channels: a client session and a server session per
channel, sealed messages told apart by their discriminator, the handshake parameters and request and reply by
correlation id.

```kotlin
ChatRoomChannel(api).connect(roomId = "kotlin", nick = "ada") {
    sendSay(ChatSay(text = "Hello"))
    incoming.collect { message -> show(message) }
}
```

## Reading and comparing documents

The readers can be used on their own. A document is read through a resolver the caller implements; a remote reference
is refused unless the caller allows it, and every finding names its code, the source, the JSON Pointer, the line and
the column.

```kotlin
when (val reading = OpenApi.reader().read(Source.Text(text, DocumentUri("file:///api/petstore.yaml")))) {
    is Reading.Read -> use(reading.document)
    is Reading.Invalid -> reading.diagnostics.forEach(::report)
    is Reading.Malformed -> reading.diagnostics.forEach(::report)
}

val comparison = OpenApi.compare(baseline = released, candidate = current)
if (comparison.isBreakingForClients) comparison.breakingForClients.forEach(::report)
```

## Security

kapi assumes that every document, overlay, request, response, header, token, server-sent event and WebSocket frame may
come from an attacker. Every reader and decoder is bounded by limits with hard ceilings, grammars are hand-written,
patterns of JSON Schema run in a linear matcher of kapi's own, maps keyed by input are balanced trees, untrusted text is
escaped exactly once where it is shown, and code of the application runs contained. [SECURITY.md](SECURITY.md)
describes the threat model, the known limitations and how to report a vulnerability.

## Building

```bash
./gradlew check
```

It needs JDK 25 and network access. [AGENTS.md](AGENTS.md) holds the rules every change follows and
[CONTRIBUTING.md](CONTRIBUTING.md) how a change gets in.

## License

Copyright 2026 Davils. Licensed under the [Apache License, Version 2.0](LICENSE).
