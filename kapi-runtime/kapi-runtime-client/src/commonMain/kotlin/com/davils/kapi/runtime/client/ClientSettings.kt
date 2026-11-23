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

package com.davils.kapi.runtime.client

import com.davils.kapi.runtime.client.call.Timeout
import com.davils.kapi.runtime.client.dpop.DpopProofs
import com.davils.kapi.runtime.client.internal.Texts
import com.davils.kapi.runtime.client.pipeline.Interception
import com.davils.kapi.runtime.client.redirect.RedirectPolicy
import com.davils.kapi.runtime.core.codec.Codecs
import com.davils.kapi.runtime.core.destination.DestinationPolicy
import com.davils.kapi.runtime.core.framing.FramingLimits
import com.davils.kapi.runtime.core.security.Credentials
import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.trace.Tracer
import com.davils.konfetti.core.EventBus
import com.davils.kontinuity.policy.Policy
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import kotlin.time.TimeSource

/**
 * The settings of an [ApiClient], built by [apiClient].
 *
 * Settings are immutable and safe to share between threads. They are refused when the name or the base URL is
 * malformed, a limit is not above zero or above its hard ceiling, a part of the timeout is not above zero and finite,
 * an interceptor or a DPoP binding is named twice, or the `HttpClient` follows redirects itself, which would hand
 * credentials to wherever a server points. [toString] never shows a credential.
 *
 * @since 1.0.0
 */
public class ClientSettings internal constructor(builder: ClientSettingsBuilder) : Validatable {
    /**
     * The name of the client in telemetry.
     *
     * @since 1.0.0
     */
    public val name: String = builder.name

    /**
     * The base URL without a slash at its end.
     *
     * @since 1.0.0
     */
    public val baseUrl: String = builder.baseUrl.trimEnd('/')

    /**
     * The codecs of the client.
     *
     * @since 1.0.0
     */
    public val codecs: Codecs = builder.codecs

    /**
     * The credentials of the client.
     *
     * @since 1.0.0
     */
    public val credentials: Credentials = builder.credentials

    /**
     * The destination policy of the client.
     *
     * @since 1.0.0
     */
    public val destinations: DestinationPolicy = builder.destinations

    /**
     * The largest answer the client reads into memory, in bytes.
     *
     * @since 1.0.0
     */
    public val maxResponseBytes: Long = builder.maxResponseBytes

    /**
     * The limits of streams.
     *
     * @since 1.0.0
     */
    public val framing: FramingLimits = builder.framing

    /**
     * The header fields masked next to those the runtime core always masks.
     *
     * @since 1.0.0
     */
    public val sensitiveHeaders: Set<String> = builder.sensitiveHeaders.toSet()

    /**
     * The time a call may take unless it sets its own.
     *
     * @since 1.0.0
     */
    public val timeout: Timeout = builder.timeout

    /**
     * The resilience policy of retryable calls, or `null`.
     *
     * @since 1.0.0
     */
    public val resilience: Policy? = builder.resilience

    /**
     * The redirects the client follows, or `null`.
     *
     * @since 1.0.0
     */
    public val redirects: RedirectPolicy? = builder.redirects

    /**
     * The meter registry, or `null`.
     *
     * @since 1.0.0
     */
    public val meters: MeterRegistry? = builder.meters

    /**
     * The tracer.
     *
     * @since 1.0.0
     */
    public val tracer: Tracer = builder.tracer

    /**
     * The event bus, or `null`.
     *
     * @since 1.0.0
     */
    public val events: EventBus? = builder.events

    /**
     * The time source of durations.
     *
     * @since 1.0.0
     */
    public val timeSource: TimeSource.WithComparableMarks = builder.timeSource

    /**
     * The interceptors in the order they were added.
     *
     * @since 1.0.0
     */
    public val interceptions: List<Interception> = builder.interceptions.toList()

    /**
     * The DPoP proofs of each bound scheme.
     *
     * @since 1.0.0
     */
    public val dpop: Map<String, DpopProofs> = builder.bindings.toMap()

    private val boundSchemes: List<String> = builder.bindings.map { (scheme, _) -> scheme }

    private val isFollowingRedirects: Boolean = builder.isFollowingRedirects

    override fun ValidationScope.validate() {
        if (!Texts.isLowerWords(name)) {
            fail(::name, "must be lower case words joined by a dot, a dash or an underscore", CODES.NAME_INVALID)
        }
        if (baseUrl.isNotEmpty() && !Texts.isPlainUrl(baseUrl)) {
            fail(::baseUrl, BASE_URL, CODES.BASE_URL_INVALID)
        }
        if (maxResponseBytes !in 1..HARD_RESPONSE_BYTES) {
            fail(::maxResponseBytes, "must be above zero and at most $HARD_RESPONSE_BYTES", CODES.LIMIT_INVALID)
        }
        if (timeout.parts.any { limit -> !limit.isPositive() || limit.isInfinite() }) {
            fail(::timeout, "must be above zero and finite in every part it sets", CODES.TIMEOUT_INVALID)
        }
        validateNames()
        if (isFollowingRedirects) {
            fail(::baseUrl, FOLLOWED, CODES.REDIRECTS_FOLLOWED)
        }
    }

    override fun toString(): String =
        "ClientSettings(name=${SafeText.text(name)}, baseUrl=${SafeText.text(baseUrl)}, credentials=$credentials, " +
            "maxResponseBytes=$maxResponseBytes)"

    private fun ValidationScope.validateNames() {
        val names = interceptions.map(Interception::name)
        if (names.any(String::isBlank) || names.toSet().size != names.size) {
            fail(::interceptions, "must each have a name of its own that is not blank", CODES.INTERCEPTOR_INVALID)
        }
        if (boundSchemes.any(String::isBlank) || dpop.size != boundSchemes.size) {
            fail(::dpop, "must bind each scheme once, and no blank one", CODES.DPOP_INVALID)
        }
    }

    /**
     * The hard ceilings of the settings.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The largest answer any client reads into memory: 1 GiB.
         *
         * @since 1.0.0
         */
        public const val HARD_RESPONSE_BYTES: Long = 1_024L * 1_024 * 1_024

        private const val BASE_URL: String = "must be empty or an http or https URL without a query or a fragment"

        private const val FOLLOWED: String = "is called by an HttpClient that follows redirects itself, which hands " +
            "credentials to wherever a server points; create it with followRedirects = false and set redirects"

        private val CODES: ClientViolationCodes = ClientViolationCodes
    }
}
