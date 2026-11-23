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

package com.davils.kapi.runtime.core.ratelimit

import com.davils.kapi.runtime.core.FieldLimits
import com.davils.kapi.runtime.core.structured.BareItem
import com.davils.kapi.runtime.core.structured.Item
import com.davils.kapi.runtime.core.structured.Member
import com.davils.kapi.runtime.core.structured.StructuredFields
import kotlinx.io.bytestring.ByteString

/**
 * Reads and writes the `RateLimit-Policy` and `RateLimit` fields of draft-ietf-httpapi-ratelimit-headers, Lists of
 * RFC 9651 whose items are the names of policies with their parameters.
 *
 * A value that is no List of RFC 9651 or exceeds the [FieldLimits] is refused as a whole. A member the draft does not
 * define, such as one that is no String, lacks its required parameter (`q` of a policy, `r` of a state) or carries a
 * parameter of the wrong type, a negative number or an unknown quota unit, is ignored, as the draft asks of a
 * recipient. The functions keep no state and are safe to call from any thread.
 *
 * @since 1.0.0
 */
public object RateLimits {
    /**
     * Reads the value of `RateLimit-Policy`.
     *
     * @param text the field value, the lines of a repeated field joined with commas.
     * @param limits the limits of the List.
     * @return the valid policies in order, or `null` if [text] is no List or exceeds a limit.
     * @since 1.0.0
     */
    public fun parsePolicies(text: String, limits: FieldLimits = FieldLimits.Default): List<RateLimitPolicy>? =
        StructuredFields.parseListOrNull(text, limits)?.mapNotNull(::policyOf)

    /**
     * Reads the value of `RateLimit`.
     *
     * @param text the field value, the lines of a repeated field joined with commas.
     * @param limits the limits of the List.
     * @return the valid states in order, or `null` if [text] is no List or exceeds a limit.
     * @since 1.0.0
     */
    public fun parseLimits(text: String, limits: FieldLimits = FieldLimits.Default): List<RateLimit>? =
        StructuredFields.parseListOrNull(text, limits)?.mapNotNull(::limitOf)

    /**
     * Writes [policies] as the value of `RateLimit-Policy`.
     *
     * @param policies the policies in order.
     * @return the field value.
     * @since 1.0.0
     */
    public fun formatPolicies(policies: List<RateLimitPolicy>): String =
        StructuredFields.serializeList(policies.map(RateLimitPolicy::item))

    /**
     * Writes [limits] as the value of `RateLimit`.
     *
     * @param limits the states in order.
     * @return the field value.
     * @since 1.0.0
     */
    public fun formatLimits(limits: List<RateLimit>): String =
        StructuredFields.serializeList(limits.map(RateLimit::item))

    private fun policyOf(member: Member): RateLimitPolicy? {
        val item = member as? Item
        val name = (item?.value as? BareItem.Text)?.value
        val quota = item?.let { found -> number(found, "q") }
        val unitText = item?.run { parameters["qu"] }
        val unit = unitOf(unitText)
        val window = item?.run { parameters["w"] }
        val windowSeconds = item?.let { found -> number(found, "w") }
        val isValid = (window == null || windowSeconds != null) && isKeyValid(item) && quota != null
        if (name == null || unit == null || !isValid) return null
        return RateLimitPolicy(
            name = name,
            quota = quota,
            unit = unit,
            window = windowSeconds,
            partitionKey = keyOf(item),
        )
    }

    private fun limitOf(member: Member): RateLimit? {
        val item = member as? Item
        val name = (item?.value as? BareItem.Text)?.value
        val remaining = item?.let { found -> number(found, "r") }
        val reset = item?.run { parameters["t"] }
        val resetSeconds = item?.let { found -> number(found, "t") }
        val isValid = (reset == null || resetSeconds != null) && isKeyValid(item)
        if (name == null || remaining == null || !isValid) return null
        return RateLimit(name = name, remaining = remaining, reset = resetSeconds, partitionKey = keyOf(item))
    }

    private fun number(item: Item, key: String): Long? {
        val value = (item.parameters[key] as? BareItem.Integer)?.value
        return value?.takeIf { number -> number >= 0 }
    }

    private fun unitOf(item: BareItem?): QuotaUnit? {
        if (item == null) return QuotaUnit.Requests
        val text = (item as? BareItem.Text)?.value
        return QuotaUnit.entries.firstOrNull { unit -> unit.value == text }
    }

    private fun isKeyValid(item: Item?): Boolean {
        val key = item?.run { parameters["pk"] }
        return key == null || key is BareItem.Bytes
    }

    private fun keyOf(item: Item?): ByteString? = (item?.run { parameters["pk"] } as? BareItem.Bytes)?.value
}
