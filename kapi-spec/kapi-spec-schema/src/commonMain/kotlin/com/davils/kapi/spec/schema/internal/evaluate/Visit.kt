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

package com.davils.kapi.spec.schema.internal.evaluate

import com.davils.kapi.spec.schema.internal.Trail
import com.davils.kapi.spec.schema.internal.value.Instance

internal class Visit(
    val instance: Instance,
    val instanceTrail: Trail?,
    val keywordTrail: Trail?,
    val scope: Scope?,
    val chain: Chain?,
) {
    fun entering(resource: Int): Visit {
        if (scope?.resource == resource) return this
        return Visit(
            instance = instance,
            instanceTrail = instanceTrail,
            keywordTrail = keywordTrail,
            scope = Scope(scope, resource),
            chain = chain,
        )
    }
}
