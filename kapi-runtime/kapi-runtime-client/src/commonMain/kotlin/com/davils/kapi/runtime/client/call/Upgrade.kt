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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.io.bytestring.ByteString

/**
 * The protocol a call switches to with `101 Switching Protocols` of RFC 9110 §15.2.2, and what it writes after the
 * switch.
 *
 * The upgrade reaches the engine as the attribute [RequestAttributes.upgrade]; a call that carries one is sent once.
 *
 * @property protocol the protocol of the `Upgrade` field, such as `tcp`.
 * @property input what the client writes after the switch.
 * @since 1.0.0
 */
public class Upgrade(public val protocol: String, public val input: Flow<ByteString> = emptyFlow()) {
    override fun toString(): String = "Upgrade(${SafeText.text(protocol)})"
}
