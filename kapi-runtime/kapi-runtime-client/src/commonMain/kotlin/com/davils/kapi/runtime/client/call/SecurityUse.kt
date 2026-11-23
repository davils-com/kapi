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

package com.davils.kapi.runtime.client.call

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

private const val HASH_FACTOR: Int = 31

/**
 * One security scheme of a security requirement of an operation and where its credential goes.
 *
 * A use is immutable and safe to share between threads.
 *
 * @property scheme the name of the scheme in the document, such as `bearerAuth`.
 * @property placement where the credential goes.
 * @since 1.0.0
 */
public class SecurityUse(public val scheme: String, public val placement: CredentialPlacement) {
    override fun equals(other: Any?): Boolean =
        other is SecurityUse && scheme == other.scheme && placement == other.placement

    override fun hashCode(): Int = scheme.hashCode() * HASH_FACTOR + placement.hashCode()

    override fun toString(): String = "SecurityUse(${SafeText.text(scheme)}, $placement)"
}
