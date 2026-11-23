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

package com.davils.kapi.runtime.server.security

import com.davils.kapi.runtime.core.security.Secret
import com.davils.kapi.runtime.server.fixture.NOW
import com.davils.kapi.runtime.server.fixture.certificateOf
import com.davils.kapi.runtime.server.fixture.crypto
import com.davils.kapi.runtime.server.fixture.fixedClock
import com.davils.kapi.runtime.server.fixture.requestOf
import com.davils.kapi.runtime.server.request.PeerIdentity
import com.davils.kore.core.principal.principal
import com.davils.krypt.jose.JoseKey
import com.davils.krypt.jose.JsonValue
import com.davils.krypt.jose.jwk.JsonWebKeys
import com.davils.krypt.jose.jwk.JwkSet
import com.davils.krypt.jose.jws.JwsAlgorithm
import com.davils.krypt.jose.jwt.Jwt
import com.davils.krypt.jose.jwt.jwtClaims
import com.davils.krypt.jose.jwt.jwtValidator
import com.davils.krypt.primitive.key.SecretKey
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private const val WORKER: String = "worker"

private const val MINUTE: Long = 60L

private const val ISSUER: String = "https://issuer.example"

private const val AUDIENCE: String = "pets"

private const val SUBJECT: String = "alice"

private const val READ: String = "pets:read"

private const val WRITE: String = "pets:write"

private const val MANY: Int = 70_000

private val jwt: Jwt = Jwt(crypto)

private val secret: SecretKey = crypto.keys.generateSecret()

private val signing: JoseKey = JoseKey.Secret(secret, keyId = "k1")

private val validator = jwtValidator {
    algorithms(JwsAlgorithm.HS256)
    issuer = ISSUER
    audience(AUDIENCE)
}

private val jwtAuthorizer: JwtAuthorizer = JwtAuthorizer(validator, KeySource.of(listOf(signing)), jwt)

private suspend fun tokenOf(
    subject: String? = SUBJECT,
    scope: String? = "$READ $WRITE",
    scp: JsonValue? = null,
    expiresAt: Instant = NOW + 10.minutes,
): String {
    val claims = jwtClaims {
        issuer = ISSUER
        tokenSubject = subject
        audience = listOf(AUDIENCE)
        this.expiresAt = expiresAt
        scope?.let { text -> claim("scope", JsonValue.Text(text)) }
        scp?.let { value -> claim("scp", value) }
    }
    return jwt.sign(claims = claims, key = signing, algorithm = JwsAlgorithm.HS256)
}

private fun attempt(credential: PresentedCredential, scopes: Set<String> = setOf(READ)): AuthorizationAttempt =
    AuthorizationAttempt(scheme = "oauth", credential = credential, scopes = scopes, request = requestOf())

private fun token(text: String): PresentedCredential = PresentedCredential.Token(Secret(text))

private fun introspecting(answer: String?, audience: String? = null): IntrospectionAuthorizer = IntrospectionAuthorizer(
    transport = { answer },
    clock = fixedClock,
    issuer = ISSUER,
    audience = audience,
)

private val nowSeconds: Long = NOW.epochSeconds

class AuthorizersTest : FunSpec({
    context("the authorizer of JSON Web Tokens") {
        test("grants a valid token with the scopes the operation demands, as the principal of its subject") {
            val granted = jwtAuthorizer.authorize(attempt(token(tokenOf())))

            granted shouldBe Authorization.Granted(principal(SUBJECT, READ, WRITE))
        }

        test("reads the scopes of the claim scp as well, as a string or an array") {
            val listed = tokenOf(
                scope = null,
                scp = JsonValue.Array(listOf(JsonValue.Text(READ), JsonValue.Integer(1))),
            )
            val text = tokenOf(scope = null, scp = JsonValue.Text(READ))

            jwtAuthorizer.authorize(attempt(token(listed))) shouldBe Authorization.Granted(principal(SUBJECT, READ))
            jwtAuthorizer.authorize(attempt(token(text))) shouldBe Authorization.Granted(principal(SUBJECT, READ))
        }

        test("forbids a valid token that lacks a scope the operation demands") {
            jwtAuthorizer.authorize(attempt(token(tokenOf(scope = READ)), setOf(WRITE))) shouldBe
                Authorization.Forbidden(setOf(WRITE))
        }

        test("denies an expired, a foreign, a malformed or a subjectless token, and every other credential") {
            val foreign = JwtAuthorizer(
                validator,
                KeySource.of(listOf(JoseKey.Secret(crypto.keys.generateSecret()))),
                jwt,
            )

            jwtAuthorizer.authorize(attempt(token(tokenOf(expiresAt = NOW - 10.minutes)))) shouldBe
                Authorization.InvalidToken
            foreign.authorize(attempt(token(tokenOf()))) shouldBe Authorization.InvalidToken
            jwtAuthorizer.authorize(attempt(token("not.a.token"))) shouldBe Authorization.InvalidToken
            jwtAuthorizer.authorize(attempt(token(tokenOf(subject = null)))) shouldBe Authorization.InvalidToken
            jwtAuthorizer.authorize(attempt(PresentedCredential.Key(Secret("k")))) shouldBe Authorization.InvalidToken
        }

        test("grants the caller a principal of its own choice") {
            val custom = JwtAuthorizer(validator, KeySource.of(listOf(signing)), jwt) { valid ->
                principal("custom:${valid.claims.subject.orEmpty()}")
            }

            custom.authorize(attempt(token(tokenOf()))) shouldBe Authorization.Granted(principal("custom:$SUBJECT"))
            custom.toString() shouldBe "JwtAuthorizer"
        }

        test("finds its keys in a JSON Web Key Set, leaving out what it cannot use") {
            val keys = JsonWebKeys(crypto)
            val set = JwkSet(listOf(keys.of(secret, keyId = "k1"))).encode()
            val fromSet = JwtAuthorizer(validator, KeySource.jwks(keys) { set }, jwt)
            val broken = JwtAuthorizer(validator, KeySource.jwks(keys) { "{" }, jwt)

            fromSet.authorize(attempt(token(tokenOf()))) shouldBe Authorization.Granted(principal(SUBJECT, READ, WRITE))
            broken.authorize(attempt(token(tokenOf()))) shouldBe Authorization.InvalidToken
        }
    }

    context("the authorizer of API keys") {
        val keys = ApiKeyAuthorizer { key -> principal("service").takeIf { key == Secret("k1") } }

        test("grants a known key and denies an unknown key and every other credential") {
            keys.authorize(attempt(PresentedCredential.Key(Secret("k1")))) shouldBe
                Authorization.Granted(principal("service"))
            keys.authorize(attempt(PresentedCredential.Key(Secret("k2")))) shouldBe Authorization.Unknown
            keys.authorize(attempt(token("k1"))) shouldBe Authorization.Unknown
            keys.toString() shouldBe "ApiKeyAuthorizer"
        }
    }

    context("the authorizer of basic credentials") {
        val basic = BasicAuthorizer(
            BasicVerifier.of(user = "ops", password = Secret("pw"), principal = principal("ops")),
        )

        test("grants the user with the right password and denies every other") {
            basic.authorize(attempt(PresentedCredential.Basic("ops", Secret("pw")))) shouldBe
                Authorization.Granted(principal("ops"))
            basic.authorize(attempt(PresentedCredential.Basic("ops", Secret("px")))) shouldBe Authorization.Unknown
            basic.authorize(attempt(PresentedCredential.Basic("opz", Secret("pw")))) shouldBe Authorization.Unknown
            basic.authorize(attempt(token("pw"))) shouldBe Authorization.Unknown
            basic.toString() shouldBe "BasicAuthorizer"
        }
    }

    context("the authorizer of client certificates") {
        test("grants the chain it knows and denies every other credential") {
            val chain = listOf(certificateOf(WORKER))
            val certificates = CertificateAuthorizer { found -> principal(WORKER).takeIf { found == chain } }

            certificates.authorize(attempt(PresentedCredential.Certificates(chain))) shouldBe
                Authorization.Granted(principal(WORKER))
            certificates.authorize(attempt(PresentedCredential.Certificates(emptyList()))) shouldBe
                Authorization.Unknown
            certificates.authorize(attempt(PresentedCredential.Peer(PeerIdentity(WORKER)))) shouldBe
                Authorization.Unknown
            certificates.toString() shouldBe "CertificateAuthorizer"
        }
    }

    context("the authorizer of token introspection") {
        test("grants an active token of the issuer and the audience with the scopes it lists") {
            val answer = """{"active":true,"sub":"alice","scope":"$READ","iss":"$ISSUER","aud":["$AUDIENCE"],""" +
                """"exp":${nowSeconds + MINUTE},"nbf":${nowSeconds - MINUTE}}"""

            introspecting(answer, audience = AUDIENCE).authorize(attempt(token("t"))) shouldBe
                Authorization.Granted(principal(SUBJECT, READ))
            introspecting(answer.replace(oldValue = "[\"$AUDIENCE\"]", newValue = "\"$AUDIENCE\""), audience = AUDIENCE)
                .authorize(attempt(token("t"))) shouldBe Authorization.Granted(principal(SUBJECT, READ))
        }

        test("names the caller by its user or its client when the answer has no subject") {
            introspecting("""{"active":true,"username":"bob","scope":"$READ","iss":"$ISSUER"}""")
                .authorize(attempt(token("t"))) shouldBe Authorization.Granted(principal("bob", READ))
            introspecting("""{"active":true,"client_id":"app","scope":"$READ","iss":"$ISSUER"}""")
                .authorize(attempt(token("t"))) shouldBe Authorization.Granted(principal("app", READ))
        }

        test("forbids an active token that lacks a scope the operation demands") {
            introspecting("""{"active":true,"sub":"alice","iss":"$ISSUER"}""").authorize(attempt(token("t"))) shouldBe
                Authorization.Forbidden(setOf(READ))
        }

        test("denies an inactive, expired, early, foreign or unnamed token and every broken answer") {
            listOf(
                """{"active":false,"sub":"alice","scope":"$READ","iss":"$ISSUER"}""",
                """{"active":"true","sub":"alice","scope":"$READ","iss":"$ISSUER"}""",
                """{"active":true,"sub":"alice","scope":"$READ","iss":"$ISSUER","exp":$nowSeconds}""",
                """{"active":true,"sub":"alice","scope":"$READ","iss":"$ISSUER","nbf":${nowSeconds + MINUTE}}""",
                """{"active":true,"sub":"alice","scope":"$READ","iss":"https://other"}""",
                """{"active":true,"scope":"$READ","iss":"$ISSUER"}""",
                """{"active":true,"sub":"alice","scope":"$READ","iss":"$ISSUER","aud":"other"}""",
                "{",
                "[]",
                " ".repeat(MANY),
                null,
            ).forEach { answer ->
                introspecting(answer, audience = AUDIENCE.takeIf { answer?.contains("aud") == true })
                    .authorize(attempt(token("t"))) shouldBe Authorization.InvalidToken
            }
            introspecting("{}").authorize(attempt(PresentedCredential.Key(Secret("t")))) shouldBe
                Authorization.InvalidToken
            introspecting("{}").toString() shouldBe "IntrospectionAuthorizer"
        }

        test("is asked with the token the request presents") {
            val asked = mutableListOf<String>()
            val recording = IntrospectionAuthorizer(transport = { secret ->
                asked += secret.reveal()
                null
            })

            recording.authorize(attempt(token("opaque")))

            asked shouldBe listOf("opaque")
        }
    }

    context("the values of authorization") {
        test("show their scheme and scopes, and mask every secret") {
            val shown = attempt(PresentedCredential.Basic("ops", Secret("pw"))).toString()

            shown shouldBe "AuthorizationAttempt(oauth, Basic(ops, ****), [pets:read])"
            PresentedCredential.Key(Secret("k")).toString() shouldBe "Key(****)"
            token("t").toString() shouldBe "Token(****)"
            PresentedCredential.Certificates(emptyList()).toString() shouldBe "Certificates(0)"
            PresentedCredential.Peer(PeerIdentity("u", 1L)).toString() shouldBe "Peer(PeerIdentity(u, 1))"
            PresentedCredential.Peer(PeerIdentity("u")) shouldBe PresentedCredential.Peer(PeerIdentity("u"))
            PresentedCredential.Basic("u", Secret("p")).hashCode() shouldBe "u".hashCode()
        }
    }

    context("an answer of introspection that names no valid caller") {
        test("is denied, and an issuer of any kind is accepted when none is demanded") {
            val anyIssuer = IntrospectionAuthorizer(
                transport = { """{"active":true,"sub":" padded","scope":"$READ"}""" },
            )
            val numbered = IntrospectionAuthorizer(
                transport = { """{"active":true,"sub":7,"aud":[1,"x"],"scope":"$READ"}""" },
            )

            anyIssuer.authorize(attempt(token("t"))) shouldBe Authorization.InvalidToken
            numbered.authorize(attempt(token("t"))) shouldBe Authorization.InvalidToken
            IntrospectionAuthorizer(
                transport = { """{"active":true,"sub":"a","aud":[{}],"scope":"$READ"}""" },
                audience = "x",
            ).authorize(attempt(token("t"))) shouldBe Authorization.InvalidToken
        }
    }
})
