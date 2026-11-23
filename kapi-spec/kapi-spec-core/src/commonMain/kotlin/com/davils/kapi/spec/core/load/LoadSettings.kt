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

package com.davils.kapi.spec.core.load

import com.davils.kapi.spec.core.policy.ReadLimits
import com.davils.kapi.spec.core.policy.ReferencePolicy
import com.davils.kapi.spec.core.source.Resolver
import com.davils.kore.core.annotation.DavilsInternal

/**
 * What a [DocumentLoader] needs from the configuration of a reader.
 *
 * @property resolver loads the documents.
 * @property limits the limits of the reading.
 * @property references which documents references may lead to.
 * @property hasMergeKeys whether the merge key `<<` of YAML 1.1 is honoured.
 * @since 1.0.0
 */
@DavilsInternal
public class LoadSettings(
    public val resolver: Resolver,
    public val limits: ReadLimits,
    public val references: ReferencePolicy,
    public val hasMergeKeys: Boolean,
) {
    private val identity: List<Any> get() = listOf(resolver, limits, references, hasMergeKeys)

    override fun equals(other: Any?): Boolean = other is LoadSettings && identity == other.identity

    override fun hashCode(): Int = identity.hashCode()

    override fun toString(): String = "LoadSettings(limits=$limits, references=$references, hasMergeKeys=$hasMergeKeys)"
}
