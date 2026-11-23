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

package com.davils.kapi.runtime.server

import com.davils.kapi.runtime.core.codec.Codecs
import com.davils.kapi.runtime.core.header.FieldSyntax
import com.davils.kapi.runtime.server.problem.ProblemSettings
import com.davils.kapi.runtime.server.security.Authorizer
import com.davils.kapi.runtime.server.telemetry.TelemetrySettings
import com.davils.kapi.runtime.server.webhook.DeliveryVerifier
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import kotlin.time.Clock

private const val MOST_REALM_LENGTH: Int = 256

/**
 * The settings of a server that [Kapi] installs: its codecs, its limits, its problems, its authorizers, its verifier
 * of deliveries, its telemetry, its realm and its clock.
 *
 * The settings are immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class ServerSettings internal constructor(builder: ServerSettingsBuilder) : Validatable {
    /**
     * The codecs that read bodies and write replies.
     *
     * @since 1.0.0
     */
    public val codecs: Codecs = builder.codecs

    /**
     * The limits of requests.
     *
     * @since 1.0.0
     */
    public val limits: ServerLimits = builder.limits

    /**
     * How problems are written.
     *
     * @since 1.0.0
     */
    public val problems: ProblemSettings = builder.problems

    /**
     * The authorizers by the names of their security schemes.
     *
     * @since 1.0.0
     */
    public val authorizers: Map<String, Authorizer> = builder.authorizers.toMap()

    /**
     * The verifier of webhooks and callbacks, or `null` to refuse every delivery whose operation declares no
     * security.
     *
     * @since 1.0.0
     */
    public val verifier: DeliveryVerifier? = builder.verifier

    /**
     * Where the server reports.
     *
     * @since 1.0.0
     */
    public val telemetry: TelemetrySettings = builder.telemetry

    /**
     * The realm of the challenges of `WWW-Authenticate`; `api` by default.
     *
     * @since 1.0.0
     */
    public val realm: String = builder.realm

    /**
     * The names of further header fields to mask besides the credentials every server masks.
     *
     * @since 1.0.0
     */
    public val sensitiveHeaders: Set<String> = builder.sensitiveHeaders.toSet()

    /**
     * The clock of HTTP-dates with two-digit years.
     *
     * @since 1.0.0
     */
    public val clock: Clock = builder.clock

    override fun ValidationScope.validate() {
        val isRealm = realm.length in 1..MOST_REALM_LENGTH && realm.all { char -> char in ' '..'~' && char != '"' }
        if (!isRealm) {
            fail(::realm, "must be printable ASCII without quotes", ServerViolationCodes.REALM_INVALID)
        }
        if (!authorizers.keys.all(FieldSyntax::isToken)) {
            fail(::authorizers, "must be keyed by the names of schemes", ServerViolationCodes.SCHEME_INVALID)
        }
    }

    @OptIn(DavilsInternal::class)
    override fun toString(): String = "ServerSettings(realm=${SafeText.text(realm)}, $limits, " +
        "schemes=${authorizers.keys.map(SafeText::text)}, verifier=${verifier != null}, $telemetry)"
}
