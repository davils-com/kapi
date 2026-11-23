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

package com.davils.kapi.runtime.server.telemetry

import com.davils.konfetti.core.TopicKey

/**
 * The konfetti topics a server publishes on.
 *
 * @since 1.0.0
 */
public object ServerTopics {
    /**
     * The topic `kapi.server` of every [ServerEvent]; a server declares it on its event bus when the bus lacks it.
     *
     * @since 1.0.0
     */
    public val EVENTS: TopicKey<ServerEvent> = TopicKey("kapi.server")
}
