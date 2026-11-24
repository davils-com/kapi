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

package com.davils.kapi.runtime.server.webhook

import com.davils.kapi.runtime.core.FieldLimits
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Configures a [SignaturePolicy].
 *
 * A builder is not thread-safe; the policy it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class SignaturePolicyBuilder internal constructor() : ValidatingBuilder<SignaturePolicy>() {
    override val subject: String get() = "signature policy"

    /**
     * See [SignaturePolicy.label].
     *
     * @since 1.0.0
     */
    public var label: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [SignaturePolicy.requiredComponents].
     *
     * @since 1.0.0
     */
    public var requiredComponents: Set<String> = setOf("@method", "@target-uri")
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [SignaturePolicy.maxAge].
     *
     * @since 1.0.0
     */
    public var maxAge: Duration = DEFAULT_AGE_MINUTES.minutes
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [SignaturePolicy.clockSkew].
     *
     * @since 1.0.0
     */
    public var clockSkew: Duration = 1.minutes
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [SignaturePolicy.isCreatedRequired].
     *
     * @since 1.0.0
     */
    public var isCreatedRequired: Boolean = true
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [SignaturePolicy.tag].
     *
     * @since 1.0.0
     */
    public var tag: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [SignaturePolicy.clock].
     *
     * @since 1.0.0
     */
    public var clock: Clock = Clock.System
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [SignaturePolicy.limits].
     *
     * @since 1.0.0
     */
    public var limits: FieldLimits = FieldLimits.Default
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [SignaturePolicy.isContentBound].
     *
     * @since 1.0.0
     */
    public var isContentBound: Boolean = true
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [SignaturePolicy.replays].
     *
     * @since 1.0.0
     */
    public var replays: ReplayGuard? = null
        set(value) {
            checkOpen()
            field = value
        }

    override fun assemble(): SignaturePolicy = SignaturePolicy(this)

    private companion object {
        const val DEFAULT_AGE_MINUTES: Int = 5
    }
}

/**
 * Builds the policy of a [MessageSignatureVerifier].
 *
 * @param configure changes the policy from its defaults.
 * @return the policy.
 * @throws com.davils.kore.core.validation.report.ValidationException if a duration is out of range.
 * @since 1.0.0
 */
public fun signaturePolicy(configure: SignaturePolicyBuilder.() -> Unit = {}): SignaturePolicy {
    val builder = SignaturePolicyBuilder()
    builder.configure()
    return builder.build()
}
