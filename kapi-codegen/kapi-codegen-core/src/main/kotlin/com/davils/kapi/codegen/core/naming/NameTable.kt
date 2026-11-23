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


package com.davils.kapi.codegen.core.naming

import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.Keywords
import com.davils.kapi.codegen.core.pipeline.Findings

internal class NameTable(private val findings: Findings) {
    private val claims: MutableMap<String, NameClaim> = linkedMapOf()

    fun claim(claim: NameClaim) {
        val key = claim.name.lowercase()
        val first = claims[key]
        if (first == null) {
            claims[key] = claim
            return
        }
        findings.error(
            CodegenCodes.NAME_COLLISION,
            "${claim.origin} becomes ${claim.name}, which ${first.origin}${placeOf(first.location)} already is as " +
                "${first.name}; name one of them with ${Keywords.NAME}",
            claim.location,
        )
    }

    private fun placeOf(location: Location?): String {
        if (location == null) return ""
        return " at ${location.pointer}"
    }
}
