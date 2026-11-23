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

package com.davils.kapi.runtime.client.internal

import com.davils.konfetti.core.Event
import com.davils.konfetti.core.EventBus
import com.davils.konfetti.core.Topic
import com.davils.konfetti.core.TopicKey
import com.davils.kore.core.failure.contained

internal class EventSink<E : Event>(bus: EventBus?, key: TopicKey<E>) {
    private val topic: Topic<E>? = bus?.let { found -> contained { topicOf(found, key) }.getOrNull() }

    fun emit(event: () -> E) {
        val target = topic?.takeIf(Topic<E>::hasConsumers) ?: return
        contained { target.offer(event()) }
    }

    private fun topicOf(bus: EventBus, key: TopicKey<E>): Topic<E> {
        if (key !in bus) bus.declare(key)
        return bus[key]
    }
}
