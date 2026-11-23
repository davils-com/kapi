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

import com.davils.kapi.runtime.core.structured.BareItem
import com.davils.kapi.runtime.core.structured.Item
import com.davils.kapi.runtime.core.structured.Parameters
import kotlinx.io.bytestring.ByteString

/**
 * A quota policy of the `RateLimit-Policy` field of draft-ietf-httpapi-ratelimit-headers, such as
 * `"default";q=100;w=10`.
 *
 * A policy is immutable; [toString] is its member of the field.
 *
 * @property name the name of the policy, printable ASCII.
 * @property quota the quota `q`, zero or more.
 * @property unit the unit `qu` of the quota.
 * @property window the window `w` in seconds, or `null` if the policy names none.
 * @property partitionKey the partition key `pk`, or `null` if the policy names none.
 * @throws IllegalArgumentException if [name] holds a character outside printable ASCII, or [quota] or [window] is
 * negative or has more than 15 digits.
 * @since 1.0.0
 */
public class RateLimitPolicy(
    public val name: String,
    public val quota: Long,
    public val unit: QuotaUnit = QuotaUnit.Requests,
    public val window: Long? = null,
    public val partitionKey: ByteString? = null,
) {
    init {
        require(quota >= 0 && (window == null || window >= 0)) { "A quota and a window are not negative" }
    }

    /**
     * The item of the field that writes this policy.
     *
     * @since 1.0.0
     */
    public val item: Item = Item(
        BareItem.Text(name),
        Parameters(
            listOfNotNull(
                "q" to BareItem.Integer(quota),
                ("qu" to BareItem.Text(unit.value)).takeIf { unit != QuotaUnit.Requests },
                window?.let { seconds -> "w" to BareItem.Integer(seconds) },
                partitionKey?.let { key -> "pk" to BareItem.Bytes(key) },
            ),
        ),
    )

    override fun equals(other: Any?): Boolean = other is RateLimitPolicy && item == other.item

    override fun hashCode(): Int = item.hashCode()

    override fun toString(): String = item.toString()
}
