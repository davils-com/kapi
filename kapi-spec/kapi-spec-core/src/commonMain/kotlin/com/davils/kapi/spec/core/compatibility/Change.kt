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

package com.davils.kapi.spec.core.compatibility

import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kore.core.validation.violation.ViolationCode

/**
 * One change between two versions of a document, with what it means for clients and for providers.
 *
 * The [message] names what changed and shows every text of the documents through `SafeText`, so a change is safe to
 * show. A change is immutable and safe to share between threads.
 *
 * @property code the stable code of the rule that found the change, such as `openapi.compatibility.path-removed`.
 * @property client what the change means for clients written against the baseline.
 * @property provider what the change means for providers implementing the baseline.
 * @property message what changed.
 * @property baseline where the changed part stands in the baseline, or `null` if it was added.
 * @property candidate where the changed part stands in the candidate, or `null` if it was removed.
 * @since 1.0.0
 */
public class Change(
    public val code: ViolationCode,
    public val client: Impact,
    public val provider: Impact,
    public val message: String,
    public val baseline: Location? = null,
    public val candidate: Location? = null,
) {
    private val identity: List<Any?> get() = listOf(code, client, provider, message, baseline, candidate)

    /**
     * Whether the change breaks clients or providers.
     *
     * @since 1.0.0
     */
    public val isBreaking: Boolean get() = client == Impact.Breaking || provider == Impact.Breaking

    override fun equals(other: Any?): Boolean = other is Change && identity == other.identity

    override fun hashCode(): Int = identity.hashCode()

    override fun toString(): String {
        val place = candidate ?: baseline
        val at = place?.let { found -> " at $found" }.orEmpty()
        return "[$code] client=$client provider=$provider $message$at"
    }
}
