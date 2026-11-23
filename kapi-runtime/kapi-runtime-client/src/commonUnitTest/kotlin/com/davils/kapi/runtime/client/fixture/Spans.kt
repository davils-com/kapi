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

import com.davils.kollect.core.trace.Span
import com.davils.kollect.core.trace.SpanKind
import com.davils.kollect.core.trace.SpanOutcome
import com.davils.kollect.core.trace.Tracer

internal class Spans : Tracer {
    private val started: MutableList<Recorded> = mutableListOf()

    val recorded: List<Recorded> get() = started.toList()

    override fun start(operation: String, kind: SpanKind): Span {
        val span = Recorded(operation, kind)
        started += span
        return span
    }

    internal class Recorded(val operation: String, val kind: SpanKind) : Span {
        val attributes: MutableMap<String, String> = mutableMapOf()

        var outcome: SpanOutcome? = null

        override fun attribute(key: String, value: String) {
            attributes[key] = value
        }

        override fun end(outcome: SpanOutcome) {
            this.outcome = outcome
        }
    }
}
