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

package com.davils.kapi.runtime.core.destination.fixture

import com.davils.kapi.runtime.core.destination.Destination
import com.davils.kapi.runtime.core.destination.DestinationDecision
import com.davils.kapi.runtime.core.destination.DestinationPolicy
import com.davils.kapi.runtime.core.destination.DestinationRefusal
import com.davils.kapi.runtime.core.destination.HostResolution
import com.davils.kapi.runtime.core.destination.IpAddress
import com.davils.kapi.runtime.core.destination.destinationPolicy

internal const val HTTPS: String = "https"

internal const val PUBLIC_NAME: String = "hooks.example.com"

internal const val PUBLIC_ADDRESS: String = "93.184.216.34"

internal const val PRIVATE_ADDRESS: String = "10.0.0.5"

internal val allowed: DestinationDecision = DestinationDecision.Allowed()

internal fun refused(refusal: DestinationRefusal): DestinationDecision = DestinationDecision.Refused(refusal)

internal fun allowedAt(vararg addresses: String): DestinationDecision =
    DestinationDecision.Allowed(addresses.map(IpAddress::parse))

internal fun resolving(vararg addresses: String): DestinationPolicy = destinationPolicy {
    resolver = { HostResolution.Resolved(addresses.map(IpAddress::parse)) }
}

internal suspend fun DestinationPolicy.checkHost(host: String, scheme: String = HTTPS): DestinationDecision =
    check(Destination(scheme, host))
