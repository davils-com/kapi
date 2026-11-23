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

package com.davils.kapi.runtime.client.internal

import com.davils.kapi.runtime.client.stream.RecordFraming
import com.davils.kapi.runtime.core.framing.FramingFailure
import com.davils.kapi.runtime.core.framing.FramingLimits
import com.davils.kapi.runtime.core.framing.JsonLinesReader
import com.davils.kapi.runtime.core.framing.JsonSequenceReader
import com.davils.kore.core.outcome.Outcome
import kotlinx.io.bytestring.ByteString

internal class Records(private val framing: RecordFraming, limits: FramingLimits) {
    private val lines: JsonLinesReader = JsonLinesReader(limits)

    private val sequence: JsonSequenceReader = JsonSequenceReader(limits)

    fun feed(chunk: ByteString): Outcome<List<String>, FramingFailure> = when (framing) {
        RecordFraming.JsonLines -> lines.feed(chunk)
        RecordFraming.JsonSequence -> sequence.feed(chunk)
    }

    fun finish(): Outcome<List<String>, FramingFailure> = when (framing) {
        RecordFraming.JsonLines -> lines.finish()
        RecordFraming.JsonSequence -> sequence.finish()
    }
}
