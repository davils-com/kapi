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
import com.davils.kapi.runtime.server.ServerViolationCodes
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * What a [MessageSignatureVerifier] demands of a signature of RFC 9421 besides being valid.
 *
 * A signature must cover every component of [requiredComponents], such as `@method`, `@target-uri` and
 * `content-digest`; it must carry `created` unless [isCreatedRequired] is false, no older than [maxAge] and no further
 * ahead than [clockSkew] by [clock]; an `expires` it carries must not have passed; it must carry [tag] when one is
 * set, which keeps a signature made for another application from being replayed here (RFC 9421 §7.2.7); and when
 * [label] is set only the signature of that label is checked. The fields are read within [limits]. The policy is
 * immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class SignaturePolicy internal constructor(builder: SignaturePolicyBuilder) : Validatable {
    /**
     * The label of the signature to check, or `null` to accept any signature that passes.
     *
     * @since 1.0.0
     */
    public val label: String? = builder.label

    /**
     * The components every accepted signature covers, in lower case.
     *
     * @since 1.0.0
     */
    public val requiredComponents: Set<String> = builder.requiredComponents.toSet()

    /**
     * The oldest a signature may be by its `created`.
     *
     * @since 1.0.0
     */
    public val maxAge: Duration = builder.maxAge

    /**
     * How far `created` may lie ahead and `expires` may have passed, for clocks that differ.
     *
     * @since 1.0.0
     */
    public val clockSkew: Duration = builder.clockSkew

    /**
     * Whether a signature must carry `created`.
     *
     * @since 1.0.0
     */
    public val isCreatedRequired: Boolean = builder.isCreatedRequired

    /**
     * The `tag` every accepted signature carries, or `null` for any.
     *
     * @since 1.0.0
     */
    public val tag: String? = builder.tag

    /**
     * The clock of `created` and `expires`.
     *
     * @since 1.0.0
     */
    public val clock: Clock = builder.clock

    /**
     * The limits of the fields `Signature-Input` and `Signature`.
     *
     * @since 1.0.0
     */
    public val limits: FieldLimits = builder.limits

    override fun ValidationScope.validate() {
        if (!maxAge.isPositive()) fail(::maxAge, "must be positive", ServerViolationCodes.DURATION_INVALID)
        if (clockSkew.isNegative()) fail(::clockSkew, "must not be negative", ServerViolationCodes.DURATION_INVALID)
    }

    @OptIn(DavilsInternal::class)
    override fun toString(): String = "SignaturePolicy(label=${label?.let(SafeText::text) ?: "any"}, " +
        "requiredComponents=${requiredComponents.map(SafeText::text)}, maxAge=$maxAge, clockSkew=$clockSkew)"

    /**
     * The default policy.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The policy that demands `@method` and `@target-uri`, a `created` of the last five minutes and allows a
         * minute of clock skew.
         *
         * @since 1.0.0
         */
        public val Default: SignaturePolicy = SignaturePolicyBuilder().build()
    }
}
