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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.runtime.core.destination

import com.davils.kapi.runtime.core.destination.internal.DestinationCheck
import com.davils.kapi.runtime.core.destination.internal.Hosts
import com.davils.kapi.runtime.core.destination.internal.Urls
import com.davils.kapi.runtime.core.internal.FieldGrammar
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.outcome.Outcome
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

private const val SCHEME_SYMBOLS: String = "+.-"

/**
 * The policy every URL a client calls, follows or delivers to must pass, which guards against server-side request
 * forgery.
 *
 * A destination passes when its scheme is allowed and its host either is listed in [hosts] or, when no hosts are
 * listed, is public: no address of a private, loopback, link-local, multicast, shared, documentation or reserved
 * network, judging an IPv4-mapped, a NAT64 and a 6to4 address by the IPv4 address it carries, and no name such as
 * `localhost`, a name without a dot or one under `local`, `internal`, `home.arpa` and the like. A public name is then
 * resolved by [resolver] and passes only when every address it resolves to is public, which refuses a name that an
 * attacker points into a private network. With [arePrivateNetworksAllowed] every readable host passes.
 *
 * Check the URL of every request with [check], every redirect hop with [checkRedirect], and the addresses an engine
 * resolved itself with [checkResolved], so a name cannot resolve to another address between the check and the
 * connection. A URL with user information is refused, since it hides where it leads. Credentials must not follow a
 * redirect to another origin: compare the hops with [Destination.isSameOrigin].
 *
 * A policy is immutable and safe to share between threads; the resolver it calls must be as well, and whatever it
 * throws is contained and refuses the destination.
 *
 * @since 1.0.0
 */
public class DestinationPolicy internal constructor(builder: DestinationPolicyBuilder) : Validatable {
    /**
     * The schemes a destination may have.
     *
     * @since 1.0.0
     */
    public val schemes: Set<String> = builder.schemes.toSet()

    /**
     * The hosts a destination may have; empty for every public host.
     *
     * @since 1.0.0
     */
    public val hosts: Set<String> = builder.hosts.toSet()

    /**
     * Whether a destination in a private network passes when no hosts are listed.
     *
     * @since 1.0.0
     */
    public val arePrivateNetworksAllowed: Boolean = builder.arePrivateNetworksAllowed

    /**
     * The resolver of public host names.
     *
     * @since 1.0.0
     */
    public val resolver: HostResolver = builder.resolver

    /**
     * Decides whether [destination] may be called, resolving a public host name with [resolver].
     *
     * @param destination the destination.
     * @return the decision.
     * @since 1.0.0
     */
    public suspend fun check(destination: Destination): DestinationDecision = DestinationCheck(this).decide(destination)

    /**
     * Decides whether the absolute URL [url] may be called.
     *
     * @param url the URL, which may come from an attacker, such as a webhook target or a link of a response.
     * @return the decision; [DestinationRefusal.Url] for text that is no absolute URL of RFC 3986 with a host and a
     * valid port, [DestinationRefusal.UserInfo] for a URL with user information.
     * @since 1.0.0
     */
    public suspend fun check(url: String): DestinationDecision = when (val destination = Urls.destinationOf(url)) {
        is Outcome.Failure -> DestinationDecision.Refused(destination.failure)
        is Outcome.Success -> check(destination.value)
    }

    /**
     * Decides whether a redirect from [from] to [location] may be followed, resolving [location] against [from] as
     * RFC 3986 §5 does.
     *
     * @param from the absolute URL of the response that redirects.
     * @param location the value of its `Location` field, an absolute or a relative reference.
     * @return the decision about the target of the redirect.
     * @since 1.0.0
     */
    public suspend fun checkRedirect(from: String, location: String): DestinationDecision =
        when (val destination = Urls.redirectOf(from, location)) {
            is Outcome.Failure -> DestinationDecision.Refused(destination.failure)
            is Outcome.Success -> check(destination.value)
        }

    /**
     * Decides whether [destination] may be called at [addresses], the addresses an engine resolved for its host,
     * without calling [resolver].
     *
     * @param destination the destination.
     * @param addresses the addresses the connection would use.
     * @return the decision; a listed host and a policy that allows private networks pass whatever the addresses.
     * @since 1.0.0
     */
    public fun checkResolved(destination: Destination, addresses: List<IpAddress>): DestinationDecision =
        DestinationCheck(this).decideResolved(destination, addresses)

    /**
     * Decides whether [addresses] may be connected to, whatever host they belong to.
     *
     * @param addresses the addresses.
     * @return [DestinationDecision.Allowed] if every address is public or the policy allows private networks.
     * @since 1.0.0
     */
    public fun checkAddresses(addresses: List<IpAddress>): DestinationDecision {
        if (arePrivateNetworksAllowed && addresses.isNotEmpty()) return DestinationDecision.Allowed(addresses)
        return DestinationCheck(this).addresses(addresses)
    }

    override fun ValidationScope.validate() {
        if (schemes.isEmpty()) {
            fail(::schemes, "must name at least one scheme", DestinationViolationCodes.SCHEMES_EMPTY)
        }
        schemes.filterNot(::isScheme).forEach { scheme ->
            fail(
                ::schemes,
                "names ${SafeText.quoted(scheme)}, which is no scheme in lower case",
                DestinationViolationCodes.SCHEME_INVALID,
            )
        }
        hosts.filterNot(Hosts::isPattern).forEach { host ->
            fail(
                ::hosts,
                "names ${SafeText.quoted(host)}, which is neither a host in lower case nor one that starts with *.",
                DestinationViolationCodes.HOST_INVALID,
            )
        }
    }

    override fun toString(): String = "DestinationPolicy(schemes=${schemes.map(SafeText::text)}, " +
        "hosts=${hosts.map(SafeText::text)}, arePrivateNetworksAllowed=$arePrivateNetworksAllowed)"

    private fun isScheme(scheme: String): Boolean {
        val first = scheme.firstOrNull()
        val isLower = scheme.all { char -> char in 'a'..'z' || FieldGrammar.isDigit(char) || char in SCHEME_SYMBOLS }
        return first != null && first in 'a'..'z' && isLower
    }

    /**
     * The default policy.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Allows `https` to every public host and resolves nothing.
         *
         * @since 1.0.0
         */
        public val Default: DestinationPolicy = DestinationPolicyBuilder().build()
    }
}
