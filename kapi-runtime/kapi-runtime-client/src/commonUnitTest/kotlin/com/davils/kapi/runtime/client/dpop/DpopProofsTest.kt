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

package com.davils.kapi.runtime.client.dpop

import com.davils.kapi.runtime.client.ClientSettingsBuilder
import com.davils.kapi.runtime.client.ClientViolationCodes
import com.davils.kapi.runtime.client.call.CredentialPlacement
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.client.call.SecurityUse
import com.davils.kapi.runtime.client.fixture.Exchanges
import com.davils.kapi.runtime.client.fixture.ok
import com.davils.kapi.runtime.client.fixture.pets
import com.davils.kapi.runtime.client.oauth.ClientAuthentication
import com.davils.kapi.runtime.client.oauth.fixture.Jwts
import com.davils.kapi.runtime.client.oauth.fixture.TokenServer
import com.davils.kapi.runtime.client.oauth.fixture.json
import com.davils.kapi.runtime.core.security.Secret
import com.davils.kapi.runtime.core.security.credentials
import com.davils.kore.core.outcome.Outcome
import com.davils.krypt.core.algorithm.DigestAlgorithm
import com.davils.krypt.core.algorithm.KeyType
import com.davils.krypt.jose.JoseKey
import com.davils.krypt.jose.jws.Jws
import com.davils.krypt.jose.jws.JwsAlgorithm
import com.davils.krypt.primitive.Crypto
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.json.JsonObject
import kotlin.io.encoding.Base64

private const val URL: String = "https://api.example.com/v1/pets?limit=1#top"

private const val TARGET: String = "https://api.example.com/v1/pets"

private const val DPOP: String = "DPoP"

private const val NONCE_FIELD: String = "DPoP-Nonce"

private const val NONCE: String = "nonce"

private const val FIRST_NONCE: String = "n-1"

private const val SCHEME: String = "oauth"

private const val TOKEN: String = "t1"

private const val ORIGINS: Int = 65

private const val FIRST_ORIGIN: String = "https://h0.example.com"

private const val LAST_ORIGIN: String = "https://h64.example.com"

private val crypto: Crypto = Crypto()

private val base64: Base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

private suspend fun proofs(): Pair<DpopProofs, JoseKey.Public> {
    val pair = crypto.keys.generate(KeyType.P256)
    val proofs = dpopProofs(
        crypto = crypto,
        key = JoseKey.Private(pair.privateKey),
        publicKey = pair.publicKey,
        algorithm = JwsAlgorithm.ES256,
    )
    return proofs to JoseKey.Public(pair.publicKey)
}

private suspend fun claimsOf(proofs: DpopProofs, url: String, method: String = "GET"): JsonObject =
    Jwts.claims(proofs.proof(method = method, url = url))

class DpopProofsTest : FunSpec({
    context("a proof") {
        test("names the method, the URL without query and fragment, a fresh ID and the hash of the token") {
            val (proofs, key) = proofs()
            val proof = proofs.proof(method = "GET", url = URL, accessToken = Secret(TOKEN))
            val other = claimsOf(proofs, URL)
            val claims = Jwts.claims(proof)
            val header = Jwts.header(proof)
            val hash = crypto.digests.digest(TOKEN.encodeToByteString(), DigestAlgorithm.Sha256)
            val verified = Jws(crypto).verify(proof, listOf(key), setOf(JwsAlgorithm.ES256))

            verified.shouldBeInstanceOf<Outcome.Success<*>>()
            Jwts.text(header, "typ") shouldBe "dpop+jwt"
            Jwts.text(Jwts.member(header, "jwk"), "kty") shouldBe "EC"
            Jwts.text(claims, "htm") shouldBe "GET"
            Jwts.text(claims, "htu") shouldBe TARGET
            Jwts.text(claims, "ath") shouldBe base64.encode(hash.toByteArray())
            Jwts.text(claims, "jti") shouldNotBe Jwts.text(other, "jti")
            Jwts.text(other, "ath") shouldBe null
            proofs.toString() shouldBe "DpopProofs(ES256)"
        }

        test("carries the nonce the server of its origin handed out, and ignores one that is malformed") {
            val (proofs, _) = proofs()
            proofs.acceptNonce(url = URL, nonce = FIRST_NONCE)
            proofs.acceptNonce(url = "https://other.example.com/x", nonce = "o-1")
            proofs.acceptNonce(url = URL, nonce = "bad\"nonce")
            proofs.acceptNonce(url = URL, nonce = null)

            Jwts.text(claimsOf(proofs, TARGET, method = "POST"), NONCE) shouldBe FIRST_NONCE
            Jwts.text(claimsOf(proofs, "https://third.example.com"), NONCE) shouldBe null
        }

        test("keeps the nonces of a bounded number of origins") {
            val (proofs, _) = proofs()
            repeat(ORIGINS) { index -> proofs.acceptNonce(url = "https://h$index.example.com", nonce = "n$index") }

            Jwts.text(claimsOf(proofs, LAST_ORIGIN), NONCE) shouldBe "n64"
            Jwts.text(claimsOf(proofs, FIRST_ORIGIN), NONCE) shouldBe null
            Jwts.text(claimsOf(proofs, "not a url"), NONCE) shouldBe null
        }

        test("gives way to a new origin with the nonce used least recently") {
            val (proofs, _) = proofs()
            repeat(ORIGINS - 1) { index -> proofs.acceptNonce(url = "https://h$index.example.com", nonce = "n$index") }
            claimsOf(proofs, FIRST_ORIGIN)

            proofs.acceptNonce(url = LAST_ORIGIN, nonce = "n64")

            Jwts.text(claimsOf(proofs, FIRST_ORIGIN), NONCE) shouldBe "n0"
            Jwts.text(claimsOf(proofs, "https://h1.example.com"), NONCE) shouldBe null
        }

        test("is never made with a MAC") {
            val pair = crypto.keys.generate(KeyType.P256)
            shouldThrow<IllegalArgumentException> {
                dpopProofs(
                    crypto = crypto,
                    key = JoseKey.Private(pair.privateKey),
                    publicKey = pair.publicKey,
                    algorithm = JwsAlgorithm.HS256,
                )
            }
        }
    }

    context("a client that binds a scheme to a DPoP key") {
        test("sends the token as DPoP with a proof, and again with the nonce the server asks for") {
            val (proofs, _) = proofs()
            val secured = pets.change { security(listOf(SecurityUse(SCHEME, CredentialPlacement.Bearer))) }
            val challenge = headersOf(
                HttpHeaders.WWWAuthenticate to listOf("""DPoP error="use_dpop_nonce""""),
                NONCE_FIELD to listOf(FIRST_NONCE),
            )
            val exchanges = Exchanges(configure = {
                credentials = credentials { scheme(SCHEME) { Secret(TOKEN) } }
                dpop(SCHEME, proofs)
            }) { request ->
                val nonce = Jwts.text(Jwts.claims(request.headers[DPOP].orEmpty()), NONCE)
                val unauthorized = respond(content = "", status = HttpStatusCode.Unauthorized, headers = challenge)
                ok("[]").takeIf { nonce != null } ?: unauthorized
            }

            exchanges.execute(secured).shouldBeInstanceOf<Exchange.Answered>().status shouldBe HttpStatusCode.OK.value
            val request = exchanges.requests.last()
            val claims = Jwts.claims(request.headers[DPOP].orEmpty())

            request.headers[HttpHeaders.Authorization] shouldBe "DPoP t1"
            Jwts.text(claims, "htu") shouldBe TARGET
            exchanges.requests.size shouldBe 2
        }

        test("is refused when it binds a scheme twice") {
            val (proofs, _) = proofs()
            val builder = ClientSettingsBuilder(isFollowingRedirects = false)
            builder.dpop(SCHEME, proofs)
            builder.dpop(SCHEME, proofs)
            val codes = builder.validate().violations.map { violation -> violation.code }

            codes shouldBe listOf(ClientViolationCodes.DPOP_INVALID)
        }
    }

    context("a token source that binds its tokens to a DPoP key") {
        test("proves the key at the token endpoint, retries with the nonce and accepts only DPoP tokens") {
            val (proofs, _) = proofs()
            val challenge = headersOf(
                HttpHeaders.ContentType to listOf("application/json"),
                NONCE_FIELD to listOf(FIRST_NONCE),
            )
            val tokens = TokenServer(configure = {
                authentication = ClientAuthentication.SecretBasic(Secret("s"))
                dpop = proofs
            }) { request ->
                val nonce = Jwts.text(Jwts.claims(request.headers[DPOP].orEmpty()), NONCE)
                val refused = respond(
                    content = """{"error":"use_dpop_nonce"}""",
                    status = HttpStatusCode.BadRequest,
                    headers = challenge,
                )
                nonce?.let { json("""{"access_token":"t1","token_type":"DPoP"}""") } ?: refused
            }
            val bearer = TokenServer(configure = {
                authentication = ClientAuthentication.SecretBasic(Secret("s"))
                dpop = proofs
            }) { json("""{"access_token":"t1","token_type":"Bearer"}""") }

            tokens.withTokens { source -> source.token() } shouldBe Secret(TOKEN)
            tokens.requests.size shouldBe 2
            bearer.withTokens { source -> source.token() } shouldBe null
        }
    }
})
