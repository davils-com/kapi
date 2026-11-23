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

package com.davils.kapi.runtime.client

import com.davils.kapi.runtime.client.call.Timeout
import com.davils.kapi.runtime.client.dpop.DpopProofs
import com.davils.kapi.runtime.client.pipeline.CallPhase
import com.davils.kapi.runtime.client.pipeline.Interception
import com.davils.kapi.runtime.client.pipeline.Interceptor
import com.davils.kapi.runtime.client.redirect.RedirectPolicy
import com.davils.kapi.runtime.core.codec.Codecs
import com.davils.kapi.runtime.core.destination.DestinationPolicy
import com.davils.kapi.runtime.core.framing.FramingLimits
import com.davils.kapi.runtime.core.security.Credentials
import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.trace.Tracer
import com.davils.konfetti.core.EventBus
import com.davils.kontinuity.policy.Policy
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import kotlin.time.TimeSource

/**
 * Configures the [ClientSettings] of an [ApiClient]; every option has a safe default.
 *
 * A builder is not thread-safe; the settings it builds are.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ClientSettingsBuilder internal constructor(internal val isFollowingRedirects: Boolean) :
    ValidatingBuilder<ClientSettings>() {
    private val added: MutableList<Interception> = mutableListOf()

    private val bound: MutableList<Pair<String, DpopProofs>> = mutableListOf()

    override val subject: String get() = "client settings"

    internal val interceptions: List<Interception> get() = added

    internal val bindings: List<Pair<String, DpopProofs>> get() = bound

    /**
     * The name of the client in telemetry, lower case words joined by a dot, a dash or an underscore; `api` by
     * default.
     *
     * @since 1.0.0
     */
    public var name: String = DEFAULT_NAME

    /**
     * The URL every path of a call is appended to, such as `https://api.example.com/v1`; empty for a client that only
     * calls destinations.
     *
     * @since 1.0.0
     */
    public var baseUrl: String = ""

    /**
     * The codecs that write the content of a call and read the content of an answer.
     *
     * @since 1.0.0
     */
    public var codecs: Codecs = Codecs.Default

    /**
     * The credentials of the security schemes the client meets.
     *
     * @since 1.0.0
     */
    public var credentials: Credentials = Credentials.None

    /**
     * The policy every URL the client calls or follows must pass; `https` to public hosts by default.
     *
     * @since 1.0.0
     */
    public var destinations: DestinationPolicy = DestinationPolicy.Default

    /**
     * The largest answer the client reads into memory, in bytes; 16 MiB by default and at most
     * [ClientSettings.HARD_RESPONSE_BYTES].
     *
     * @since 1.0.0
     */
    public var maxResponseBytes: Long = DEFAULT_RESPONSE_BYTES

    /**
     * The limits of the lines, records and events of a stream.
     *
     * @since 1.0.0
     */
    public var framing: FramingLimits = FramingLimits.Default

    /**
     * Header fields to mask when an answer is shown, next to `Authorization`, `Cookie` and the others the runtime
     * core always masks.
     *
     * @since 1.0.0
     */
    public var sensitiveHeaders: Set<String> = emptySet()

    /**
     * The time a call may take unless it sets its own; nothing by default, which leaves it to the engine.
     *
     * @since 1.0.0
     */
    public var timeout: Timeout = Timeout()

    /**
     * The resilience policy of kontinuity a retryable call runs under, such as a retry built with
     * [com.davils.kapi.runtime.client.retry.CallRetry]; none by default.
     *
     * @since 1.0.0
     */
    public var resilience: Policy? = null

    /**
     * The redirects the client follows itself; `null` by default, which hands a redirect to the caller as an answer.
     *
     * @since 1.0.0
     */
    public var redirects: RedirectPolicy? = null

    /**
     * The registry of kollect the client counts and times its calls in, or `null`.
     *
     * @since 1.0.0
     */
    public var meters: MeterRegistry? = null

    /**
     * The tracer of kollect that opens a client span for every call.
     *
     * @since 1.0.0
     */
    public var tracer: Tracer = Tracer.None

    /**
     * The event bus of konfetti the client reports retries, redirects, refreshes and resumed streams on, or `null`.
     *
     * @since 1.0.0
     */
    public var events: EventBus? = null

    /**
     * The time source of the durations the client measures.
     *
     * @since 1.0.0
     */
    public var timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic

    /**
     * Adds [interceptor] to [phase] under [name], after the interceptors already added to it.
     *
     * @param phase the phase.
     * @param name the name, unique in the client.
     * @param interceptor the interceptor.
     * @since 1.0.0
     */
    public fun intercept(phase: CallPhase, name: String, interceptor: Interceptor) {
        added += Interception(phase, name, interceptor)
    }

    /**
     * Binds the bearer tokens of the scheme [scheme] to the DPoP key of [proofs] by RFC 9449: every request that
     * carries such a token sends it as `Authorization: DPoP` with a fresh proof.
     *
     * @param scheme the name of the scheme.
     * @param proofs the proofs of the key the tokens are bound to.
     * @since 1.0.0
     */
    public fun dpop(scheme: String, proofs: DpopProofs) {
        bound += scheme to proofs
    }

    override fun assemble(): ClientSettings = ClientSettings(this)

    /**
     * The defaults of the settings.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The name of a client the application does not name.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_NAME: String = "api"

        /**
         * The largest answer a client reads unless configured otherwise: 16 MiB.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_RESPONSE_BYTES: Long = 16L * 1_024 * 1_024
    }
}
