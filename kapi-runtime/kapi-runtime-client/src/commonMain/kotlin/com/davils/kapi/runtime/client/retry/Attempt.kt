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

package com.davils.kapi.runtime.client.retry

import com.davils.kapi.runtime.client.call.Call
import com.davils.kapi.runtime.client.call.Exchange

private const val HASH_FACTOR: Int = 31

/**
 * One attempt of a call under a resilience policy: the result a retry of kontinuity judges with
 * [CallRetry.results] and waits for with [CallRetry.interval].
 *
 * An attempt is immutable. [toString] shows the call and the exchange, never a credential.
 *
 * @property call the call.
 * @property exchange how the attempt ended.
 * @since 1.0.0
 */
public class Attempt(public val call: Call, public val exchange: Exchange) {
    override fun equals(other: Any?): Boolean = other is Attempt && call == other.call && exchange == other.exchange

    override fun hashCode(): Int = call.hashCode() * HASH_FACTOR + exchange.hashCode()

    override fun toString(): String = "Attempt($call, $exchange)"
}
