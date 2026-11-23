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

package com.davils.kapi.runtime.client.channel.fixture

import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketExtension
import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlin.coroutines.CoroutineContext

internal class FakeSession : WebSocketSession {
    val inbound: Channel<Frame> = Channel(Channel.UNLIMITED)

    val sent: Channel<Frame> = Channel(Channel.UNLIMITED)

    override val coroutineContext: CoroutineContext = Job()

    override var masking: Boolean = false

    override var maxFrameSize: Long = Long.MAX_VALUE

    override val incoming: ReceiveChannel<Frame> get() = inbound

    override val outgoing: SendChannel<Frame> get() = sent

    override val extensions: List<WebSocketExtension<*>> = emptyList()

    override suspend fun flush() = Unit

    @Deprecated("Use cancel() instead.", level = DeprecationLevel.ERROR)
    override fun terminate() = Unit

    fun frames(): List<Frame> {
        val all = mutableListOf<Frame>()
        var frame = sent.tryReceive().getOrNull()
        while (frame != null) {
            all += frame
            frame = sent.tryReceive().getOrNull()
        }
        return all
    }
}
