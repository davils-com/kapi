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

package com.davils.kapi.runtime.core.destination

import com.davils.kapi.runtime.core.destination.fixture.HTTPS
import com.davils.kapi.runtime.core.destination.fixture.PUBLIC_ADDRESS
import com.davils.kapi.runtime.core.destination.fixture.PUBLIC_NAME
import com.davils.kapi.runtime.core.destination.fixture.allowed
import com.davils.kapi.runtime.core.destination.fixture.checkHost
import com.davils.kapi.runtime.core.destination.fixture.refused
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val PORT: Int = 8_443

private const val LABEL_LENGTH: Int = 50

private const val LABELS: Int = 5

private val policy: DestinationPolicy = DestinationPolicy.Default

private val malformedAddresses: List<String> = listOf(
    "1..2.3",
    "1234.1.1.1",
    "1.2.3.4.5",
    "1.2.3",
    "1:2:3:4::5:6:7:8",
    "1:2:3",
)

class DestinationEdgesTest : FunSpec({
    context("URLs") {
        test("pass with an empty port") {
            policy.check("https://$PUBLIC_NAME:/") shouldBe allowed
        }

        test("are refused with a port of more than five digits") {
            policy.check("https://$PUBLIC_NAME:123456/") shouldBe refused(DestinationRefusal.Url)
        }

        test("are refused as redirects from a URL that is not absolute or to no URI reference") {
            policy.checkRedirect(from = "/relative", location = "/next") shouldBe refused(DestinationRefusal.Url)
            policy.checkRedirect(from = "https://$PUBLIC_NAME/", location = "a b") shouldBe
                refused(DestinationRefusal.Url)
        }
    }

    context("hosts") {
        test("refuse an unclosed literal and a name that is too long") {
            val longName = List(LABELS) { "a".repeat(LABEL_LENGTH) }.joinToString(".")

            policy.checkHost("[abc") shouldBe refused(DestinationRefusal.Host)
            policy.checkHost(longName) shouldBe refused(DestinationRefusal.Host)
        }

        malformedAddresses.forEach { text ->
            test("refuse the malformed address '$text'") {
                IpAddress.parseOrNull(text).shouldBeNull()
            }
        }
    }

    context("the policy") {
        test("refuses an empty list of resolved addresses even where private networks are allowed") {
            val open = destinationPolicy { arePrivateNetworksAllowed = true }

            open.checkAddresses(emptyList()) shouldBe refused(DestinationRefusal.Unresolved)
        }

        test("accepts schemes with symbols and refuses one that starts with a symbol") {
            val builder = DestinationPolicyBuilder()
            val refusing = DestinationPolicyBuilder()
            builder.schemes = setOf("svn+ssh", "a.b-c")
            refusing.schemes = setOf("~a")

            builder.validate().violations.shouldBeEmpty()
            refusing.validate().violations.map { violation -> violation.code } shouldBe
                listOf(DestinationViolationCodes.SCHEME_INVALID)
        }
    }

    context("a destination") {
        test("differs in its scheme, its host and its port") {
            val destination = Destination(scheme = HTTPS, host = PUBLIC_NAME, port = PORT)

            destination shouldNotBe Destination(scheme = "http", host = PUBLIC_NAME, port = PORT)
            destination shouldNotBe Destination(scheme = HTTPS, host = PUBLIC_ADDRESS, port = PORT)
            destination shouldNotBe Destination(scheme = HTTPS, host = PUBLIC_NAME)
            destination.equals(PUBLIC_NAME) shouldBe false
            Destination(scheme = HTTPS, host = PUBLIC_NAME).hashCode() shouldBe
                Destination(scheme = HTTPS, host = PUBLIC_NAME).hashCode()
        }
    }

    context("a decision") {
        test("differs by kind and by value") {
            val public = DestinationDecision.Allowed(listOf(IpAddress.parse(PUBLIC_ADDRESS)))

            public shouldNotBe DestinationDecision.Allowed()
            public.equals(refused(DestinationRefusal.Host)) shouldBe false
            refused(DestinationRefusal.Host) shouldNotBe refused(DestinationRefusal.Url)
            refused(DestinationRefusal.Host).equals(public) shouldBe false
        }
    }
})
