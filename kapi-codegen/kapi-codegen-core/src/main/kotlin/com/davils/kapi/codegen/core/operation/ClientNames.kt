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


package com.davils.kapi.codegen.core.operation

import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.Keywords
import com.davils.kapi.codegen.core.pipeline.Findings

private val taken: Set<String> = setOf("body", "call", "client", "exchange", "it")

internal class ClientNames(private val findings: Findings) {
    fun audit(name: String, origin: String, location: Location?) {
        if (name !in taken) return
        findings.error(
            CodegenCodes.NAME_COLLISION,
            "$origin becomes $name, which the generated client uses itself; name it with ${Keywords.NAME}",
            location,
        )
    }
}
