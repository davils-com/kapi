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

package com.davils.kapi.spec.asyncapi.read

import com.davils.kapi.spec.asyncapi.ReaderConfiguration
import com.davils.kapi.spec.asyncapi.VersionFamily
import com.davils.kapi.spec.asyncapi.diagnostic.Findings
import com.davils.kapi.spec.asyncapi.read.reference.ReferenceQueue
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kore.core.text.TextSnippet
import com.davils.kore.core.validation.violation.ViolationCode

internal class ReadScope(
    val configuration: ReaderConfiguration,
    val family: VersionFamily,
    private val findings: Findings,
) {
    val references: ReferenceQueue = ReferenceQueue()

    fun report(code: ViolationCode, message: String, location: Location, snippet: TextSnippet?) {
        findings.report(code = code, message = message, location = location, snippet = snippet)
    }
}
