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
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import kotlin.time.Clock

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

    /**
     * See [ServerSettings.verifier].
     *
     * @since 1.0.0
     */
    public var verifier: DeliveryVerifier? = null

    /**
     * See [ServerSettings.realm].
     *
     * @since 1.0.0
     */
    public var realm: String = DEFAULT_REALM

    /**
     * See [ServerSettings.sensitiveHeaders].
     *
     * @since 1.0.0
     */
    public var sensitiveHeaders: Set<String> = emptySet()

    /**
     * See [ServerSettings.clock].
     *
     * @since 1.0.0
     */
    public var clock: Clock = Clock.System

    internal var limits: ServerLimits = ServerLimits.Default
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
        val builder = ServerLimitsBuilder()
        builder.configure()
        limits = builder.build()
    }

    /**
     * Changes how problems are written; see [ServerSettings.problems].
     *
     * @param configure changes the settings.
     * @throws com.davils.kore.core.validation.report.ValidationException if a setting is invalid.
     * @since 1.0.0
     */
    public fun problems(configure: ProblemSettingsBuilder.() -> Unit) {
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
        authorizers[scheme] = authorizer
    }

    override fun assemble(): ServerSettings = ServerSettings(this)

    private companion object {
        const val DEFAULT_REALM: String = "api"
    }
}
