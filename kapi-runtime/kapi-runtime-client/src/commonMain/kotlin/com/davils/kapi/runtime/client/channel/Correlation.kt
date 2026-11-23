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

package com.davils.kapi.runtime.client.channel

import kotlinx.serialization.json.JsonElement

/**
 * Pairs a request with its reply on a channel by a correlation id, such as the `correlationId` an AsyncAPI document
 * declares, which the generated code extracts.
 *
 * A correlation reads the id either from the typed message, through [sent] and [received], or from the message as
 * the codec of the channel wrote or read it, as a [JsonElement], through [Companion.json]; the second suits an id at a
 * JSON pointer of a payload whose type is a union. The JSON of a message is read by the codec of the channel within
 * its limits of size and depth, so it works for JSON and CBOR messages alike; a message the codec cannot read as JSON
 * has no id.
 *
 * The functions are foreign code: they run inside `contained` of Kore, and one that fails counts as one that found no
 * id. They must be safe to call from several coroutines at once.
 *
 * @param S the type of the messages the client sends.
 * @param R the type of the messages the client receives.
 * @property sent the correlation id of a message the client sends, or `null`.
 * @property received the correlation id of a message the client receives, or `null`.
 * @param json the ids read from the JSON of a message, or `null` to read them from the typed message.
 * @since 1.0.0
 */
public class Correlation<in S, in R> internal constructor(
    public val sent: (S) -> String?,
    public val received: (R) -> String?,
    internal val json: JsonIds?,
) {
    /**
     * Creates a correlation that reads the id from the typed message.
     *
     * @param sent the correlation id of a message the client sends, or `null`.
     * @param received the correlation id of a message the client receives, or `null`.
     * @since 1.0.0
     */
    public constructor(sent: (S) -> String?, received: (R) -> String?) : this(sent, received, json = null)

    internal class JsonIds(val sent: (JsonElement) -> String?, val received: (JsonElement) -> String?)

    override fun toString(): String = "Correlation"

    /**
     * Builds correlations over the JSON of messages.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Returns a correlation that reads the id of a message from its JSON, as the codec of the channel writes and
         * reads it.
         *
         * @param sent the correlation id of the JSON of a message the client sends, or `null`.
         * @param received the correlation id of the JSON of a message the client receives, or `null`; [sent] by
         * default, for a request and a reply that carry the id at the same place.
         * @return the correlation, for messages of any type.
         * @since 1.0.0
         */
        public fun json(
            sent: (JsonElement) -> String?,
            received: (JsonElement) -> String? = sent,
        ): Correlation<Any?, Any?> = Correlation(sent = { null }, received = { null }, json = JsonIds(sent, received))
    }
}
