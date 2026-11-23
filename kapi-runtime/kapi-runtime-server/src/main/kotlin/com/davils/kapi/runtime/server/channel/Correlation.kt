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

package com.davils.kapi.runtime.server.channel

/**
 * Pairs a request the server sends on a channel with the reply of the client by a correlation id, such as the
 * `correlationId` an AsyncAPI document declares, which the generated code extracts.
 *
 * Both functions are foreign code: they run inside `contained` of Kore, and one that fails counts as one that found
 * no id. They must be safe to call from several coroutines at once.
 *
 * @param S the type of the messages the server sends.
 * @param R the type of the messages the server receives.
 * @property sent the correlation id of a message the server sends, or `null`.
 * @property received the correlation id of a message the server receives, or `null`.
 * @since 1.0.0
 */
public class Correlation<in S, in R>(public val sent: (S) -> String?, public val received: (R) -> String?) {
    override fun toString(): String = "Correlation"
}
