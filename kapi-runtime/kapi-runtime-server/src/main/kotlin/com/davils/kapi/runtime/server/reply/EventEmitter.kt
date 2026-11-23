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

package com.davils.kapi.runtime.server.reply

import com.davils.kapi.runtime.core.framing.ServerSentEvent

/**
 * Sends server-sent events of the WHATWG HTML standard §9.2 on a `text/event-stream` reply.
 *
 * Every event is written and flushed as one piece through the writer of the runtime core, so neither its type, its id
 * nor its data can end it early or inject a field. Sends from several coroutines are serialized; a send suspends while
 * the client reads slowly, so a fast producer is held back rather than buffered without bound.
 *
 * @since 1.0.0
 */
public interface EventEmitter {
    /**
     * Sends [event].
     *
     * @param event the event.
     * @throws kotlinx.coroutines.CancellationException if the client went away.
     * @since 1.0.0
     */
    public suspend fun send(event: ServerSentEvent)

    /**
     * Sends a comment, which a client ignores, such as one that keeps a connection open through proxies.
     *
     * @param text the text of the comment.
     * @throws IllegalArgumentException if [text] holds a line break.
     * @since 1.0.0
     */
    public suspend fun comment(text: String = "")
}
