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

import com.davils.kapi.runtime.core.destination.fixture.LOCALHOST
import com.davils.kapi.runtime.core.destination.fixture.PRIVATE_ADDRESS
import com.davils.kapi.runtime.core.destination.fixture.PUBLIC_ADDRESS
import com.davils.kapi.runtime.core.destination.fixture.PUBLIC_NAME
import com.davils.kapi.runtime.core.destination.fixture.PUBLIC_V6
import com.davils.kapi.runtime.core.destination.fixture.UNREADABLE
import com.davils.kapi.runtime.core.destination.fixture.allowed
import com.davils.kapi.runtime.core.destination.fixture.allowedAt
import com.davils.kapi.runtime.core.destination.fixture.checkHost
import com.davils.kapi.runtime.core.destination.fixture.invalidHosts
import com.davils.kapi.runtime.core.destination.fixture.privateHosts
import com.davils.kapi.runtime.core.destination.fixture.refused
import com.davils.kapi.runtime.core.destination.fixture.resolving
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val LONGEST_LABEL_COUNT: Int = 130

private val notPublic: DestinationDecision = refused(DestinationRefusal.NotPublic)

private val invalid: DestinationDecision = refused(DestinationRefusal.Host)

class DestinationPolicyTest : FunSpec({
    context("the default policy") {
        val policy = DestinationPolicy.Default

        test("allows https to a public name without resolving it") {
            policy.checkHost(PUBLIC_NAME) shouldBe allowed
            policy.checkHost("Hooks.Example.COM.", "HTTPS") shouldBe allowed
        }

        test("refuses another scheme") {
            policy.checkHost(PUBLIC_NAME, "http") shouldBe refused(DestinationRefusal.Scheme)
        }

        test("allows a public address and names it") {
            policy.checkHost(PUBLIC_ADDRESS) shouldBe allowedAt(PUBLIC_ADDRESS)
            policy.checkHost("[$PUBLIC_V6]") shouldBe allowedAt(PUBLIC_V6)
            policy.checkHost("2001:4860:4860::8888") shouldBe allowedAt("2001:4860:4860::8888")
        }

        test("judges an address that carries an IPv4 address by that address") {
            policy.checkHost("[::ffff:93.184.216.34]") shouldBe allowedAt("::ffff:93.184.216.34")
            policy.checkHost("[2002:5db8:d822::1]") shouldBe allowedAt("2002:5db8:d822::1")
            policy.checkHost("[64:ff9b::5db8:d822]") shouldBe allowedAt("64:ff9b::5db8:d822")
        }

        privateHosts.forEach { host ->
            test("refuses $host, which is not public") {
                policy.checkHost(host) shouldBe notPublic
            }
        }

        invalidHosts.forEach { host ->
            test("refuses '$host', which is no host it can read") {
                policy.checkHost(host) shouldBe invalid
            }
        }

        test("refuses a name that is longer than a host name may be") {
            policy.checkHost("a.".repeat(LONGEST_LABEL_COUNT) + "com") shouldBe invalid
        }
    }

    context("a URL") {
        val policy = DestinationPolicy.Default

        test("passes with the host and port it names") {
            policy.check("https://hooks.example.com:8443/a?b#c") shouldBe allowed
            policy.check("https://[2606:4700::1111]:443/") shouldBe allowedAt(PUBLIC_V6)
            policy.check("https://93.184.216.34") shouldBe allowedAt(PUBLIC_ADDRESS)
        }

        test("is refused when it is no absolute URL with a host or has a bad port") {
            val url = refused(DestinationRefusal.Url)

            listOf(
                "/relative",
                "mailto:a@example.com",
                "https://",
                "https:///x",
                "https://a b.com/",
                "https://a.com:99999/",
                "https://a.com:x/",
                "https://:443/",
            ).forEach { text -> policy.check(text) shouldBe url }
        }

        test("is refused when it carries user information") {
            policy.check("https://admin:secret@hooks.example.com/") shouldBe refused(DestinationRefusal.UserInfo)
            policy.check("https://hooks.example.com@127.0.0.1/") shouldBe refused(DestinationRefusal.UserInfo)
        }

        test("is refused for a private host or another scheme") {
            policy.check("https://localhost:8080/") shouldBe notPublic
            policy.check("http://hooks.example.com/") shouldBe refused(DestinationRefusal.Scheme)
            policy.check("https://[::ffff:7f00:1]/") shouldBe notPublic
        }

        test("checks every redirect hop resolved against the URL it came from") {
            policy.checkRedirect("https://api.example.com/a/b", "../c") shouldBe allowed
            policy.checkRedirect("https://api.example.com/a", "https://169.254.169.254/latest") shouldBe notPublic
            policy.checkRedirect("https://api.example.com/a", "//localhost/x") shouldBe notPublic
            policy.checkRedirect("https://api.example.com/a", "http://api.example.com/") shouldBe
                refused(DestinationRefusal.Scheme)
            policy.checkRedirect("not a url", "/x") shouldBe refused(DestinationRefusal.Url)
            policy.checkRedirect("https://api.example.com/", "a b") shouldBe refused(DestinationRefusal.Url)
        }
    }

    context("an origin") {
        test("is shared by scheme, host and effective port") {
            val origin = Destination("https", "api.example.com")

            origin.isSameOrigin(Destination("HTTPS", "API.example.com", 443)) shouldBe true
            origin.isSameOrigin(Destination("https", "api.example.com.", null)) shouldBe true
            origin.isSameOrigin(Destination("https", "api.example.com", 8443)) shouldBe false
            origin.isSameOrigin(Destination("http", "api.example.com")) shouldBe false
            origin.isSameOrigin(Destination("https", "evil.example.com")) shouldBe false
            Destination("ws", "a").effectivePort shouldBe 80
            Destination("ftp", "a").effectivePort shouldBe null
        }

        test("is equal by its parts and shown escaped") {
            Destination("https", "a", 1) shouldBe Destination("https", "a", 1)
            Destination("https", "a").hashCode() shouldBe Destination("https", "a").hashCode()
            Destination("https", "a\n").toString() shouldBe "Destination(https://a\\n)"
            Destination("https", "a", 1).toString() shouldBe "Destination(https://a:1)"
            Destination("https", "a").equals("a") shouldBe false
        }
    }

    context("allowed hosts") {
        val policy = destinationPolicy { hosts = setOf(PUBLIC_NAME, "*.partner.example", PRIVATE_ADDRESS) }

        test("are the only hosts that pass and are trusted as they are") {
            policy.checkHost(PUBLIC_NAME) shouldBe allowed
            policy.checkHost("eu.partner.example") shouldBe allowed
            policy.checkHost(PRIVATE_ADDRESS) shouldBe allowed
            policy.checkResolved(Destination("https", PUBLIC_NAME), emptyList()) shouldBe allowed
        }

        test("refuse every other host") {
            val notAllowed = refused(DestinationRefusal.HostNotAllowed)

            policy.checkHost("example.com") shouldBe notAllowed
            policy.checkHost("partner.example") shouldBe notAllowed
            policy.checkHost(PUBLIC_ADDRESS) shouldBe notAllowed
        }

        test("still refuse a host that cannot be read") {
            policy.checkHost(UNREADABLE) shouldBe invalid
        }
    }

    context("private networks") {
        test("pass when the policy allows them") {
            val policy = destinationPolicy { arePrivateNetworksAllowed = true }
            val loopback = listOf(IpAddress.parse("127.0.0.1"))

            policy.checkHost(LOCALHOST) shouldBe allowed
            policy.checkHost("127.0.0.1") shouldBe allowed
            policy.arePrivateNetworksAllowed shouldBe true
            policy.checkAddresses(loopback) shouldBe DestinationDecision.Allowed(loopback)
        }
    }

    context("a resolver") {
        test("lets a name pass whose addresses are all public and names them") {
            resolving(PUBLIC_ADDRESS, PUBLIC_V6).checkHost(PUBLIC_NAME) shouldBe allowedAt(PUBLIC_ADDRESS, PUBLIC_V6)
        }

        test("refuses a name that resolves to an address that is not public") {
            resolving(PUBLIC_ADDRESS, PRIVATE_ADDRESS).checkHost(PUBLIC_NAME) shouldBe
                refused(DestinationRefusal.ResolvesToPrivate)
        }

        test("refuses a name that resolves to nothing, cannot be resolved or whose resolver throws") {
            val failing = destinationPolicy { resolver = { HostResolution.Failed("timeout") } }
            val throwing = destinationPolicy { resolver = { throw IllegalStateException("down") } }
            val unresolved = refused(DestinationRefusal.Unresolved)

            resolving().checkHost(PUBLIC_NAME) shouldBe unresolved
            failing.checkHost(PUBLIC_NAME) shouldBe unresolved
            throwing.checkHost(PUBLIC_NAME) shouldBe unresolved
        }

        test("is not asked for an address or a name that is refused anyway") {
            val asked = mutableListOf<String>()
            val policy = destinationPolicy {
                resolver = { host ->
                    asked += host
                    HostResolution.Skipped
                }
            }

            policy.checkHost(PUBLIC_ADDRESS)
            policy.checkHost(LOCALHOST)
            policy.checkHost(PUBLIC_NAME)

            asked shouldBe listOf(PUBLIC_NAME)
        }
    }

    context("addresses an engine resolved") {
        val policy = DestinationPolicy.Default
        val destination = Destination("https", PUBLIC_NAME)

        test("pass when they are all public and are refused otherwise") {
            val public = listOf(IpAddress.parse(PUBLIC_ADDRESS))

            policy.checkResolved(destination, public) shouldBe DestinationDecision.Allowed(public)
            policy.checkResolved(destination, listOf(IpAddress.parse("10.1.2.3"))) shouldBe
                refused(DestinationRefusal.ResolvesToPrivate)
            policy.checkResolved(destination, emptyList()) shouldBe refused(DestinationRefusal.Unresolved)
            policy.checkResolved(Destination("https", LOCALHOST), public) shouldBe notPublic
            policy.checkAddresses(public) shouldBe DestinationDecision.Allowed(public)
            policy.checkAddresses(listOf(IpAddress.parse("::1"))) shouldBe refused(DestinationRefusal.ResolvesToPrivate)
        }
    }

    context("validation") {
        test("accepts the defaults, hosts in lower case and hosts that start with a wildcard") {
            val builder = DestinationPolicyBuilder()
            builder.hosts = setOf(PUBLIC_NAME, "*.partner.example", PRIVATE_ADDRESS, "::1")

            builder.validate().violations.shouldBeEmpty()
        }

        test("reports a policy without schemes") {
            val builder = DestinationPolicyBuilder()
            builder.schemes = emptySet()

            builder.validate().violations.map { violation -> violation.code } shouldBe
                listOf(DestinationViolationCodes.SCHEMES_EMPTY)
        }

        test("reports every scheme and every host that is not written as the policy compares it") {
            val builder = DestinationPolicyBuilder()
            builder.schemes = setOf("HTTPS", "https", "1x", "")
            builder.hosts = setOf("Example.com", "[::1]", UNREADABLE)

            builder.validate().violations.map { violation -> violation.code } shouldBe listOf(
                DestinationViolationCodes.SCHEME_INVALID,
                DestinationViolationCodes.SCHEME_INVALID,
                DestinationViolationCodes.SCHEME_INVALID,
                DestinationViolationCodes.HOST_INVALID,
                DestinationViolationCodes.HOST_INVALID,
                DestinationViolationCodes.HOST_INVALID,
            )
        }
    }

    context("rendering") {
        test("is the default policy when nothing is configured and names what it allows") {
            destinationPolicy().schemes shouldBe DestinationPolicy.Default.schemes
            destinationPolicy().hosts shouldBe emptySet()
            DestinationPolicy.Default.toString() shouldContain "schemes=[https]"
        }

        test("shows decisions and resolutions") {
            DestinationDecision.Refused(DestinationRefusal.Host).toString() shouldBe "Refused(Host)"
            allowedAt(PUBLIC_ADDRESS).toString() shouldBe "Allowed([93.184.216.34])"
            allowedAt(PUBLIC_ADDRESS).hashCode() shouldBe allowedAt(PUBLIC_ADDRESS).hashCode()
            HostResolution.Skipped.toString() shouldBe "Skipped"
            HostResolution.Failed("a\nb").toString() shouldBe "Failed(a\\nb)"
            HostResolution.Failed("x") shouldBe HostResolution.Failed("x")
            HostResolution.Failed("x").hashCode() shouldBe "x".hashCode()
            HostResolution.Resolved(emptyList()) shouldBe HostResolution.Resolved(emptyList())
            HostResolution.Resolved(emptyList()).toString() shouldBe "Resolved([])"
            HostResolution.Resolved(emptyList()).hashCode() shouldBe emptyList<IpAddress>().hashCode()
            DestinationDecision.Refused(DestinationRefusal.Host).hashCode() shouldBe DestinationRefusal.Host.hashCode()
            HostResolver.None.resolve(PUBLIC_NAME) shouldBe HostResolution.Skipped
        }
    }
})
