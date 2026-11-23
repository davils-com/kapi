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

package com.davils.kapi.spec.core.compatibility

import com.davils.kore.core.annotation.DavilsInternal

/**
 * The way data flows through the part of a document a comparison looks at: from the client to the provider, such as a
 * request body, or from the provider to the client, such as a response.
 *
 * What a narrower schema means depends on it: a narrower request breaks clients that still send what was allowed, a
 * narrower response breaks providers that still send it.
 *
 * @property label how a message names the direction.
 * @since 1.0.0
 */
@DavilsInternal
public enum class Direction(public val label: String) {
    /**
     * From the client to the provider.
     *
     * @since 1.0.0
     */
    Request("in a request") {
        override val narrowed: Pair<Impact, Impact> get() = Impact.Breaking to Impact.NonBreaking

        override val widened: Pair<Impact, Impact> get() = Impact.NonBreaking to Impact.Breaking
    },

    /**
     * From the provider to the client.
     *
     * @since 1.0.0
     */
    Response("in a response") {
        override val narrowed: Pair<Impact, Impact> get() = Impact.NonBreaking to Impact.Breaking

        override val widened: Pair<Impact, Impact> get() = Impact.Breaking to Impact.NonBreaking
    },
    ;

    /**
     * What it means for clients and for providers, in this order, when fewer values are allowed than before.
     *
     * @since 1.0.0
     */
    public abstract val narrowed: Pair<Impact, Impact>

    /**
     * What it means for clients and for providers, in this order, when more values are allowed than before.
     *
     * @since 1.0.0
     */
    public abstract val widened: Pair<Impact, Impact>
}
