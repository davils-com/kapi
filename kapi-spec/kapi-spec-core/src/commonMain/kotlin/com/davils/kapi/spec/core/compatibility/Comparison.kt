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

package com.davils.kapi.spec.core.compatibility

import com.davils.kapi.spec.core.collection.frozen
import com.davils.kore.core.annotation.DavilsInternal

/**
 * The changes between a baseline and a candidate version of a document, in the order they were found.
 *
 * A comparison is immutable and safe to share between threads.
 *
 * @param changes the changes, which are copied.
 * @since 1.0.0
 */
public class Comparison(changes: List<Change>) {
    /**
     * Every change, breaking or not.
     *
     * @since 1.0.0
     */
    public val changes: List<Change> = changes.frozen()

    /**
     * The changes that break clients written against the baseline.
     *
     * @since 1.0.0
     */
    public val breakingForClients: List<Change> get() = changes.filter { change -> change.client == Impact.Breaking }

    /**
     * The changes that break providers implementing the baseline.
     *
     * @since 1.0.0
     */
    public val breakingForProviders: List<Change>
        get() = changes.filter { change -> change.provider == Impact.Breaking }

    /**
     * Whether a client written against the baseline may fail against a provider of the candidate.
     *
     * @since 1.0.0
     */
    public val isBreakingForClients: Boolean get() = changes.any { change -> change.client == Impact.Breaking }

    /**
     * Whether a provider implementing the baseline may fail clients of the candidate.
     *
     * @since 1.0.0
     */
    public val isBreakingForProviders: Boolean get() = changes.any { change -> change.provider == Impact.Breaking }

    override fun equals(other: Any?): Boolean = other is Comparison && changes == other.changes

    override fun hashCode(): Int = changes.hashCode()

    override fun toString(): String =
        "Comparison(${changes.size} changes, ${breakingForClients.size} breaking for clients, " +
            "${breakingForProviders.size} breaking for providers)"
}
