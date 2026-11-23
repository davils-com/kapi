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

package com.davils.kapi.runtime.core

import com.davils.kapi.runtime.core.codec.CodecsBuilder
import com.davils.kapi.runtime.core.codec.fixture.UpperCodec
import com.davils.kapi.runtime.core.codec.form.FormLimitsBuilder
import com.davils.kapi.runtime.core.codec.multipart.MultipartLimitsBuilder
import com.davils.kapi.runtime.core.destination.DestinationPolicyBuilder
import com.davils.kapi.runtime.core.framing.FramingLimitsBuilder
import com.davils.kapi.runtime.core.negotiation.MediaRange
import com.davils.kapi.runtime.core.pattern.PatternLimitsBuilder
import com.davils.kapi.runtime.core.security.CredentialsBuilder
import com.davils.kapi.runtime.core.security.Secret
import com.davils.kapi.runtime.core.security.TokenSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec

class BuilderClosingTest : FunSpec({
    context("a builder that was evaluated") {
        test("refuses every change of a limit instead of losing it") {
            val fields = FieldLimitsBuilder()
            val forms = FormLimitsBuilder()
            val parts = MultipartLimitsBuilder()
            val framing = FramingLimitsBuilder()
            val patterns = PatternLimitsBuilder()
            fields.build()
            forms.build()
            parts.build()
            framing.validate()
            patterns.build()

            shouldThrow<IllegalStateException> { fields.maxMembers = 1 }
            shouldThrow<IllegalStateException> { forms.maxFields = 1 }
            shouldThrow<IllegalStateException> { parts.maxParts = 1 }
            shouldThrow<IllegalStateException> { framing.maxRecordBytes = 1 }
            shouldThrow<IllegalStateException> { patterns.maximumStates = 1 }
        }

        test("refuses every added codec, credential and destination") {
            val codecs = CodecsBuilder()
            val credentials = CredentialsBuilder()
            val destinations = DestinationPolicyBuilder()
            codecs.build()
            credentials.build()
            destinations.build()

            shouldThrow<IllegalStateException> { codecs.maxDepth = 1 }
            shouldThrow<IllegalStateException> { codecs.codec(UpperCodec(listOf(MediaRange.Any))) }
            shouldThrow<IllegalStateException> { credentials.scheme("bearer", TokenSource { Secret("t") }) }
            shouldThrow<IllegalStateException> { credentials.peer("peer") }
            shouldThrow<IllegalStateException> { destinations.hosts = setOf("example.com") }
        }
    }
})
