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

package com.davils.kapi.runtime.core.security

import com.davils.kapi.runtime.core.security.fixture.FailingTokenSource
import com.davils.kapi.runtime.core.security.fixture.FakeTokenSource
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private const val BEARER: String = "bearer"

private const val API_KEY: String = "apiKey"

private const val OAUTH: String = "oauth"

private const val FIRST: String = "first"

private const val SECOND: String = "second"

private fun credentialsOf(token: String?): Credentials = credentials {
    scheme(BEARER) { token?.let(::Secret) }
}

@OptIn(DavilsInternal::class)
class CredentialsTest : FunSpec({
    context("a secret") {
        test("is never shown and equals a secret of the same text") {
            Secret(FIRST).toString() shouldBe SafeText.REDACTED
            Secret(FIRST) shouldBe Secret(FIRST)
            Secret(FIRST) shouldNotBe Secret(SECOND)
            Secret(FIRST) shouldNotBe Secret("firsT")
            Secret(FIRST).hashCode() shouldBe Secret(SECOND).hashCode()
            Secret(FIRST).equals(FIRST) shouldBe false
            Secret(FIRST).reveal() shouldBe FIRST
        }
    }

    context("choose") {
        val credentials = credentials {
            scheme(BEARER) { Secret(FIRST) }
            scheme(API_KEY) { Secret(SECOND) }
        }

        test("takes the first alternative whose schemes are all there") {
            val alternatives = listOf(setOf(OAUTH), setOf(BEARER, OAUTH), setOf(BEARER, API_KEY), setOf(BEARER))

            credentials.choose(alternatives) shouldBe setOf(BEARER, API_KEY)
            credentials.schemes shouldBe setOf(BEARER, API_KEY)
        }

        test("takes the anonymous alternative when it comes first") {
            credentials.choose(listOf(emptySet(), setOf(BEARER))) shouldBe emptySet()
        }

        test("chooses nothing when no alternative can be met") {
            credentials.choose(listOf(setOf(OAUTH))) shouldBe null
            Credentials.None.choose(listOf(setOf(BEARER))) shouldBe null
        }
    }

    context("credential") {
        test("is the trimmed secret of the scheme") {
            credentialsOf("  $FIRST \n").credential(BEARER) shouldBe Credential.Present(Secret(FIRST))
        }

        test("is missing without a source, without a token or when the source fails") {
            credentialsOf(null).credential(BEARER) shouldBe Credential.Missing
            credentialsOf(FIRST).credential(API_KEY) shouldBe Credential.Missing
            credentials { scheme(BEARER, FailingTokenSource()) }.credential(BEARER) shouldBe Credential.Missing
        }

        test("is malformed when it is blank or carries a control character") {
            credentialsOf("   ").credential(BEARER) shouldBe Credential.Malformed
            credentialsOf("a\r\nX-Injected: 1").credential(BEARER) shouldBe Credential.Malformed
        }

        test("shows no secret") {
            val present = Credential.Present(Secret(FIRST))

            present.toString() shouldNotContain FIRST
            present.hashCode() shouldBe Secret(FIRST).hashCode()
            present.equals(FIRST) shouldBe false
            listOf(Credential.Missing, Credential.Malformed, Credential.Peer).map(Credential::toString) shouldBe
                listOf("Missing", "Malformed", "Peer")
        }
    }

    context("a peer scheme") {
        test("is met by the connection without a credential of its own") {
            val credentials = credentials {
                peer(BEARER)
                scheme(API_KEY) { Secret(SECOND) }
            }

            credentials.choose(listOf(setOf(BEARER))) shouldBe setOf(BEARER)
            credentials.credential(BEARER) shouldBe Credential.Peer
            credentials.credential(API_KEY) shouldBe Credential.Present(Secret(SECOND))
            credentials.schemes shouldBe setOf(API_KEY, BEARER)
        }

        test("is named once like every other scheme") {
            val builder = CredentialsBuilder()
            builder.scheme(BEARER) { null }
            builder.peer(BEARER)

            builder.validate().violations.map { violation -> violation.code } shouldBe
                listOf(CredentialsViolationCodes.SCHEME_DUPLICATE)
        }
    }

    context("refresh") {
        test("asks the source of the scheme and hands it the rejected secret") {
            val source = FakeTokenSource(listOf(FIRST, SECOND))
            val credentials = credentials { scheme(BEARER, source) }

            credentials.refresh(BEARER, Secret(FIRST)) shouldBe true
            credentials.credential(BEARER) shouldBe Credential.Present(Secret(SECOND))
            credentials.refresh(BEARER, Secret(SECOND)) shouldBe false
            source.rejected shouldBe listOf(Secret(FIRST), Secret(SECOND))
        }

        test("is refused by a source that cannot refresh, fails or is missing") {
            val credentials = credentialsOf(FIRST)

            credentials.refresh(BEARER, null) shouldBe false
            credentials.refresh(API_KEY, null) shouldBe false
            credentials { scheme(BEARER, FailingTokenSource()) }.refresh(BEARER, null) shouldBe false
        }
    }

    context("validation") {
        test("accepts schemes with distinct names") {
            val builder = CredentialsBuilder()
            builder.scheme(BEARER) { null }
            builder.scheme(API_KEY) { null }

            builder.validate().violations.shouldBeEmpty()
        }

        test("reports a blank scheme and one that is named twice") {
            val builder = CredentialsBuilder()
            builder.scheme(" ") { null }
            builder.scheme(BEARER) { null }
            builder.scheme(BEARER) { null }

            builder.validate().violations.map { violation -> violation.code } shouldBe listOf(
                CredentialsViolationCodes.SCHEME_BLANK,
                CredentialsViolationCodes.SCHEME_DUPLICATE,
            )
        }
    }

    context("rendering") {
        test("names the schemes and never a secret") {
            val rendered = credentialsOf(FIRST).toString()

            credentials().schemes shouldBe emptySet()
            rendered shouldContain BEARER
            rendered shouldNotContain FIRST
        }

        test("has no token without tokens in the fake source") {
            val source = FakeTokenSource(emptyList())

            source.token() shouldBe null
            source.refresh(null) shouldBe false
            source.rejected shouldBe listOf(null)
        }
    }
})
