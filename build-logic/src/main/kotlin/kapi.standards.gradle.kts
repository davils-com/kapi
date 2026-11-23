/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.time.Duration

plugins {
    id("kapi.base")
}

kreate {
    assurance {
        standards {
            enabled = true
            reviewEvery = Duration.ofDays(365)

            inventory {
                includes.set(setOf("*.md", "kapi-*/kapi-*/src/**"))
                excludes.set(
                    setOf(
                        "kapi-spec/kapi-spec-schema/src/jvmUnitTest/resources/json-schema-test-suite/**",
                        "kapi-spec/kapi-spec-asyncapi/src/commonMain/kotlin/com/davils/kapi/spec/asyncapi/structure/" +
                            "Schema300Texts.kt",
                    ),
                )
            }

            github("OAI/OpenAPI-Specification") {
                note = "OpenAPI 3.0, 3.1 and 3.2: the documents kapi-spec-openapi reads, checks and writes and " +
                    "kapi-codegen-core turns into Kotlin."
            }
            github("asyncapi/spec") {
                note = "AsyncAPI 3: the documents kapi-spec-asyncapi reads, checks and writes, with the WebSocket " +
                    "binding."
            }
            github("asyncapi/spec-json-schemas") {
                note = "The official JSON Schema of AsyncAPI 3.0.0 that the structure check of kapi-spec-asyncapi " +
                    "embeds unchanged."
            }
            github("OAI/Overlay-Specification") {
                note = "OpenAPI Overlays: the overlays the OpenAPI reader applies before it reads a document."
            }
            github("json-schema-org/JSON-Schema-Test-Suite") {
                note = "The official test suite every required and optional case of kapi-spec-schema passes."
            }
            draft("draft-bhutton-json-schema") {
                note = "JSON Schema 2020-12 core: the dialect kapi-spec-schema validates by default."
            }
            draft("draft-bhutton-json-schema-validation") {
                note = "JSON Schema 2020-12 validation: the validation keywords and format assertions of " +
                    "kapi-spec-schema."
            }
            draft("draft-ietf-httpapi-idempotency-key-header") {
                note = "The Idempotency-Key field: retries of non-idempotent calls in kapi-runtime-client."
            }
            draft("draft-ietf-httpapi-ratelimit-headers") {
                note = "The RateLimit and RateLimit-Policy fields: read by the client, written by the server runtime."
            }
            whatwg("html") {
                note = "Server-sent events: the event stream reader and writer of kapi-runtime-core."
            }
            rfc(1123) {
                note = "Host names: the hostname format assertion of kapi-spec-schema."
            }
            rfc(2046) {
                note = "Multipart media types: the boundaries and parts of the multipart codecs of kapi-runtime-core."
            }
            rfc(3339) {
                note = "Date and time on the internet: the date-time, date, time and duration format assertions."
            }
            rfc(3492) {
                note = "Punycode: the xn-- labels of the idn-hostname format assertion."
            }
            rfc(3986) {
                note = "URI generic syntax: document URIs, references and their resolution through UriReference."
            }
            rfc(3987) {
                note = "Internationalized resource identifiers: the iri and iri-reference format assertions."
            }
            rfc(4291) {
                note = "IPv6 addressing: the ipv6 format assertion and the address classes of the destination policy."
            }
            rfc(4647) {
                note = "Matching of language tags: the Accept-Language negotiation of kapi-runtime-core."
            }
            rfc(4648) {
                note = "Base16, Base32 and Base64: byte strings of the wire types and the byte format."
            }
            rfc(5321) {
                note = "SMTP addresses: the email format assertion."
            }
            rfc(5890) {
                note = "IDNA definitions: the idn-hostname format assertion."
            }
            rfc(5891) {
                note = "IDNA protocol: the idn-hostname format assertion."
            }
            rfc(5892) {
                note = "IDNA code points: the idn-hostname format assertion."
            }
            rfc(5893) {
                note = "IDNA right-to-left scripts: the bidi rule of the idn-hostname format assertion."
            }
            rfc(6266) {
                note = "Content-Disposition: file names of uploads and downloads in kapi-runtime-core."
            }
            rfc(6455) {
                note = "The WebSocket protocol: the channel sessions of the client and server runtimes."
            }
            rfc(6531) {
                note = "Internationalized email: the idn-email format assertion."
            }
            rfc(6570) {
                note = "URI templates: the uri-template format assertion."
            }
            rfc(6749) {
                note = "OAuth 2.0: the token sources of kapi-runtime-client and the security schemes of the readers."
            }
            rfc(6750) {
                note = "Bearer tokens: sending them in the client and their challenges in the server runtime."
            }
            rfc(6901) {
                note = "JSON Pointer: locations of diagnostics, references and the json-pointer format assertion."
            }
            rfc(7009) {
                note = "Token revocation: the revocation of tokens by kapi-runtime-client."
            }
            rfc(7464) {
                note = "JSON text sequences: the framing of streamed records in kapi-runtime-core."
            }
            rfc(7523) {
                note = "JWT client authentication: private_key_jwt and client_secret_jwt of kapi-runtime-client."
            }
            rfc(7578) {
                note = "multipart/form-data: the encoder and the bounded streaming decoder of kapi-runtime-core."
            }
            rfc(7617) {
                note = "Basic authentication: the Basic credentials of the client and the server runtime."
            }
            rfc(7636) {
                note = "PKCE: the authorization code flow of kapi-runtime-client."
            }
            rfc(7662) {
                note = "Token introspection: the introspection authorizer of kapi-runtime-server."
            }
            rfc(7692) {
                note = "Compression extensions for WebSocket: the limits of the channel sessions."
            }
            rfc(8187) {
                note = "Encoding of header parameter values: filename* and title* of kapi-runtime-core."
            }
            rfc(8259) {
                note = "JSON: the JSON codec of kapi-runtime-core and the JSON values of the validator."
            }
            rfc(8288) {
                note = "Web linking: the Link field of pagination in kapi-runtime-core."
            }
            rfc(8414) {
                note = "Authorization server metadata: the discovery of OAuth endpoints in kapi-runtime-client."
            }
            rfc(8628) {
                note = "Device authorization grant: the device flow of kapi-runtime-client and of OpenAPI 3.2."
            }
            rfc(8693) {
                note = "Token exchange: the token exchange of kapi-runtime-client."
            }
            rfc(8949) {
                note = "CBOR: the strict, bounded CBOR codec of kapi-runtime-core."
            }
            rfc(9110) {
                note = "HTTP semantics: methods, status codes, negotiation, conditional and range requests."
            }
            rfc(9111) {
                note = "HTTP caching: Cache-Control and Vary of kapi-runtime-core."
            }
            rfc(9207) {
                note = "OAuth 2.0 issuer identification: the iss parameter kapi-runtime-client checks in an " +
                    "authorization response against the issuer of the client."
            }
            rfc(9421) {
                note = "HTTP message signatures: the verification of webhook deliveries in kapi-runtime-server."
            }
            rfc(9449) {
                note = "DPoP: proof-of-possession of the tokens of kapi-runtime-client."
            }
            rfc(9457) {
                note = "Problem details: the problems of every refused request and of typed client results."
            }
            rfc(9530) {
                note = "Digest fields: Content-Digest and Repr-Digest of kapi-runtime-core and the delivery verifier."
            }
            rfc(9535) {
                note = "JSONPath: the targets of OpenAPI Overlays, evaluated by kaml."
            }
            rfc(9562) {
                note = "UUIDs: the uuid format assertion and the multipart boundaries."
            }
            rfc(9651) {
                note = "Structured field values: the RateLimit, Idempotency-Key and digest fields of kapi-runtime-core."
            }
            rfc(5322) {
                note = "Internet message format: the addr-spec the email format assertion follows."
            }
            rfc(5861) {
                note = "Cache-Control extensions for stale content: stale-while-revalidate and stale-if-error."
            }
            rfc(5952) {
                note = "The text of IPv6 addresses: how the destination policy shows addresses."
            }
            rfc(6265) {
                note = "Cookies: the cookie parameters of the client and the server runtime."
            }
            rfc(6454) {
                note = "The web origin: credentials and redirects stay on their origin."
            }
            rfc(6838) {
                note = "Media type registration: the syntax of media types and their suffixes."
            }
            rfc(6890) {
                note = "Special-purpose IP addresses: the private and reserved ranges the destination policy refuses."
            }
            rfc(7396) {
                note = "JSON Merge Patch: the tri-state patches of the wire types."
            }
            rfc(7517) {
                note = "JSON Web Keys: the key sets the JWT authorizer of kapi-runtime-server verifies against."
            }
            rfc(7519) {
                note = "JSON Web Tokens: the tokens the JWT authorizer of kapi-runtime-server validates."
            }
            rfc(8246) {
                note = "The immutable Cache-Control extension of kapi-runtime-core."
            }
            rfc(8725) {
                note = "The JWT best current practice the JWT authorizer of kapi-runtime-server follows."
            }
            rfc(9525) {
                note = "Service identity in TLS: the peer certificates the certificate authorizer checks."
            }
            iso("ISO 8601") {
                note = "Durations and dates: the timeouts of the x-kapi-timeout extension and the duration format."
            }
            unicode {
                note = "Names, descriptions and values of untrusted documents are shown through SafeText of Kore."
            }
        }
    }
}
