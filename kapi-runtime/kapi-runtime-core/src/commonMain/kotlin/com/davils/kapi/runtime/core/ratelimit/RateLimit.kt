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
 * The state of a quota in the `RateLimit` field of draft-ietf-httpapi-ratelimit-headers, such as
 * `"default";r=50;t=30`.
 *
 * A value is immutable; [toString] is its member of the field.
 *
 * @property name the name of the policy the state belongs to, printable ASCII.
 * @property remaining the remaining quota `r`, zero or more.
 * @property reset the seconds `t` until the quota resets, or `null` if the server names none.
 * @property partitionKey the partition key `pk`, or `null` if the server names none.
 * @throws IllegalArgumentException if [name] holds a character outside printable ASCII, or [remaining] or [reset] is
 * negative or has more than 15 digits.
 * @since 1.0.0
 */
public class RateLimit(
    public val name: String,
    public val remaining: Long,
    public val reset: Long? = null,
    public val partitionKey: ByteString? = null,
) {
    init {
        require(remaining >= 0 && (reset == null || reset >= 0)) { "A remaining quota and a reset are not negative" }
    }

    /**
     * The item of the field that writes this state.
     *
     * @since 1.0.0
     */
    public val item: Item = Item(
            BareItem.Text(name),
            Parameters(
                listOfNotNull(
                    "r" to BareItem.Integer(remaining),
                    reset?.let { seconds -> "t" to BareItem.Integer(seconds) },
                    partitionKey?.let { key -> "pk" to BareItem.Bytes(key) },
            ),
        ),
    )

    override fun equals(other: Any?): Boolean = other is RateLimit && item == other.item

    override fun hashCode(): Int = item.hashCode()

    override fun toString(): String = item.toString()
}
