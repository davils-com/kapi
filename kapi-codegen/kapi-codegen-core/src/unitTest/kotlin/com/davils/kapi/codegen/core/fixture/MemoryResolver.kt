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

package com.davils.kapi.codegen.core.fixture

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.source.Resolution
import com.davils.kapi.spec.core.source.Resolver
import kotlinx.io.bytestring.encodeToByteString

internal class MemoryResolver(
    documents: Map<DocumentUri, String>,
    answers: Map<DocumentUri, Resolution> = emptyMap(),
) : Resolver {
    private val documents: Map<DocumentUri, String> = documents.toMap()

    private val answers: Map<DocumentUri, Resolution> = answers.toMap()

    private val asked: MutableList<DocumentUri> = mutableListOf()

    val requested: List<DocumentUri> get() = asked.toList()

    override suspend fun resolve(uri: DocumentUri): Resolution {
        asked += uri
        val text = documents[uri] ?: return answers[uri] ?: Resolution.Missing
        return Resolution.Found(text.encodeToByteString())
    }
}
