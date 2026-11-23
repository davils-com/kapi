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

package com.davils.kapi.runtime.client.oauth

import com.davils.konfetti.core.Event
import com.davils.konfetti.core.TopicKey
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * What a token source reports on the topic [TOPIC] of its event bus; never a token, a code or a secret.
 *
 * @since 1.0.0
 */
public sealed interface OAuthEvent : Event {
    /**
     * The client id of the source.
     *
     * @since 1.0.0
     */
    public val clientId: String

    /**
     * The source got a token.
     *
     * @property clientId the client id of the source.
     * @property grant the name of the grant.
     * @since 1.0.0
     */
    public class Granted internal constructor(override val clientId: String, public val grant: String) : OAuthEvent {
        override fun toString(): String = "Granted(${SafeText.text(clientId)}, ${SafeText.text(grant)})"
    }

    /**
     * The source got no token.
     *
     * @property clientId the client id of the source.
     * @property grant the name of the grant.
     * @property reason why, such as `status 400` or `timeout`, with the `error` code of the server when it named one.
     * @since 1.0.0
     */
    public class Refused internal constructor(
        override val clientId: String,
        public val grant: String,
        public val reason: String,
    ) : OAuthEvent {
        override fun toString(): String =
            "Refused(${SafeText.text(clientId)}, ${SafeText.text(grant)}, ${SafeText.text(reason)})"
    }

    /**
     * The source asked the server to revoke its tokens.
     *
     * @property clientId the client id of the source.
     * @property revocation how it ended.
     * @since 1.0.0
     */
    public class Revoked internal constructor(override val clientId: String, public val revocation: Revocation) :
        OAuthEvent {
        override fun toString(): String = "Revoked(${SafeText.text(clientId)}, $revocation)"
    }

    /**
     * The topic of the events.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The topic `kapi.client.oauth`, declared on the bus when a source first reports to it.
         *
         * @since 1.0.0
         */
        public val TOPIC: TopicKey<OAuthEvent> = TopicKey("kapi.client.oauth")
    }
}
