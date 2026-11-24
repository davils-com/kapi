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

package com.davils.kapi.runtime.client.fixture

import com.davils.konfetti.core.Event
import com.davils.konfetti.core.Topic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.yield

internal suspend fun <T : Event> collecting(scope: CoroutineScope, topic: Topic<T>, count: Int): Deferred<List<T>> {
    val collected = scope.async {
        val events = topic.events().take(count)
        events.map { envelope -> envelope.payload }.toList()
    }
    while (!topic.hasConsumers) yield()
    return collected
}
