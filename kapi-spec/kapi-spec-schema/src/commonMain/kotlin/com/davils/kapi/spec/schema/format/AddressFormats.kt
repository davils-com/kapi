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

package com.davils.kapi.spec.schema.format

import com.davils.konnect.internetprotocol.address.Ipv4Address
import com.davils.konnect.internetprotocol.address.Ipv6Address

internal object AddressFormats {
    fun ipv4(value: String): String? {
        val isValid = Ipv4Address.parseOrNull(value) != null
        return "an IPv4 address is four decimal octets without leading zeros, such as 192.0.2.1".takeUnless { isValid }
    }

    fun ipv6(value: String): String? {
        val isValid = isIpv6(value)
        return "an IPv6 address has eight hexadecimal groups or fewer with one ::, and no zone".takeUnless { isValid }
    }

    fun isIpv6(value: String): Boolean = Ipv6Address.parseOrNull(value) != null
}
