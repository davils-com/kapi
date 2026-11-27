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

package com.davils.kapi.runtime.server

import com.davils.kapi.runtime.core.codec.Codecs
import com.davils.kapi.runtime.server.problem.ProblemSettings
import com.davils.kapi.runtime.server.problem.ProblemSettingsBuilder
import com.davils.kapi.runtime.server.security.Authorizer
import com.davils.kapi.runtime.server.telemetry.TelemetrySettings
import com.davils.kapi.runtime.server.telemetry.TelemetrySettingsBuilder
import com.davils.kapi.runtime.server.webhook.DeliveryVerifier
import com.davils.kontinuity.ratelimiter.RateLimiterConfig
import com.davils.kontinuity.ratelimiter.rateLimiterConfig
import com.davils.kontinuity.ratelimiter.tokenBucket
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * Configures the [ServerSettings] of `install(Kapi) { … }`.
 *
 * A builder is not thread-safe; the settings it builds are.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ServerSettingsBuilder internal constructor() : ValidatingBuilder<ServerSettings>() {
    override val subject: String get() = "server settings"

    /**
     * See [ServerSettings.codecs]; the built-in codecs by default.
     *
     * @since 1.0.0
     */
    public var codecs: Codecs = Codecs.Default
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [ServerSettings.verifier].
     *
     * @since 1.0.0
     */
    public var verifier: DeliveryVerifier? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [ServerSettings.realm].
     *
     * @since 1.0.0
     */
    public var realm: String = DEFAULT_REALM
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [ServerSettings.sensitiveHeaders].
     *
     * @since 1.0.0
     */
    public var sensitiveHeaders: Set<String> = emptySet()
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [ServerSettings.clock].
     *
     * @since 1.0.0
     */
    public var clock: Clock = Clock.System
        set(value) {
            checkOpen()
            field = value
        }

    internal var limits: ServerLimits = ServerLimits.Default
        private set

    internal var rateLimit: RateLimiterConfig? = null
        private set

    internal var problems: ProblemSettings = ProblemSettings.Default
        private set

    internal var telemetry: TelemetrySettings = TelemetrySettings.None
        private set

    internal val authorizers: MutableMap<String, Authorizer> = linkedMapOf()

    /**
     * Changes the limits of requests from their defaults; see [ServerSettings.limits].
     *
     * @param configure changes the limits.
     * @throws com.davils.kore.core.validation.report.ValidationException if a limit is out of range.
     * @since 1.0.0
     */
    public fun limits(configure: ServerLimitsBuilder.() -> Unit) {
        checkOpen()
        val builder = ServerLimitsBuilder()
        builder.configure()
        limits = builder.build()
    }

    /**
     * Limits how many requests one caller sends to one operation, such as `rateLimit(requests = 100, per = 1.minutes)`;
     * see [ServerSettings.rateLimit].
     *
     * A caller is its principal once authorized, or else the address of its peer. A request over the limit is answered
     * at once with `429 Too Many Requests`, the code [com.davils.kapi.runtime.server.problem.ServerCodes.RATE_LIMITED]
     * and a `Retry-After` of the period; at most 10,000 callers per operation are counted at once, and the requests of
     * a new caller are refused while every counted caller is still active, so a flood of callers cannot lift the limit.
     *
     * @param requests the requests one caller may send per period, 1 to 1,000,000.
     * @param per the period, positive and at most one day.
     * @throws com.davils.kore.core.validation.report.ValidationException if [requests] or [per] is out of range.
     * @since 1.1.0
     */
    public fun rateLimit(requests: Int, per: Duration) {
        checkOpen()
        rateLimit = rateLimiterConfig {
            limitForPeriod = requests
            limitRefreshPeriod = per
            timeout = Duration.ZERO
            algorithm = tokenBucket(burst = requests)
        }
    }

    /**
     * Changes how problems are written; see [ServerSettings.problems].
     *
     * @param configure changes the settings.
     * @throws com.davils.kore.core.validation.report.ValidationException if a setting is invalid.
     * @since 1.0.0
     */
    public fun problems(configure: ProblemSettingsBuilder.() -> Unit) {
        checkOpen()
        val builder = ProblemSettingsBuilder()
        builder.configure()
        problems = builder.build()
    }

    /**
     * Sets where the server reports; see [ServerSettings.telemetry].
     *
     * @param configure sets the tracer, the meters and the events.
     * @since 1.0.0
     */
    public fun telemetry(configure: TelemetrySettingsBuilder.() -> Unit) {
        checkOpen()
        val builder = TelemetrySettingsBuilder()
        builder.configure()
        telemetry = builder.build()
    }

    /**
     * Authorizes the security scheme [scheme] with [authorizer]; a later call for the same scheme replaces it.
     *
     * @param scheme the name of the scheme in the document, such as `oauth`.
     * @param authorizer the authorizer.
     * @since 1.0.0
     */
    public fun authorizer(scheme: String, authorizer: Authorizer) {
        checkOpen()
        authorizers[scheme] = authorizer
    }

    override fun assemble(): ServerSettings = ServerSettings(this)

    private companion object {
        const val DEFAULT_REALM: String = "api"
    }
}
