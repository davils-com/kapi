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

package com.davils.kapi.codegen.core.asyncapi

import com.davils.kapi.codegen.core.Generation
import com.davils.kapi.spec.asyncapi.Document
import com.davils.kapi.spec.core.source.SourceDocument

/**
 * Generates Kotlin for the WebSocket channels of an AsyncAPI 3 document: the models of the messages, a typed client
 * per channel for every platform of the client runtime and a typed server per channel for the JVM.
 *
 * The operations of an AsyncAPI document describe the application, which for a WebSocket API is the server: what the
 * server receives (`action: receive`) the generated client sends, and what the server sends (`action: send`) the
 * generated client receives. Only channels served over `ws` or `wss` are generated; every other channel is skipped
 * with a warning. The models are planned and emitted by the model generator of the OpenAPI generator, so the schemas
 * of the messages, written in JSON Schema draft-07 or the schema of OpenAPI 3.0, become the same Kotlin as the schemas
 * of an OpenAPI document; Avro, RAML and Protobuf are refused.
 *
 * Generation is deterministic: the same document, options and version yield the same files. Every name of the
 * document becomes a Kotlin identifier and every text is escaped for its sink, so no document injects code. A document
 * the generator cannot express faithfully is refused with located diagnostics with the codes of [GenerationCodes]. A
 * generator holds no state and is safe to share between threads.
 *
 * @property options the options.
 * @since 1.0.0
 */
public class Generator internal constructor(public val options: GeneratorOptions) {
    /**
     * Generates the Kotlin of [document].
     *
     * The generation suspends while the schemas of the messages are read as the components of an OpenAPI document;
     * it reads neither the network nor the file system.
     *
     * @param document the document, as the reader of AsyncAPI read it without errors.
     * @param sources the documents it was read from, whose digests the provenance records.
     * @return the generated files with the diagnostics, or the diagnostics that refuse the document.
     * @since 1.0.0
     */
    public suspend fun generate(document: Document, sources: List<SourceDocument> = emptyList()): Generation =
        GenerationRun(options).run(document, sources)

    override fun toString(): String = "Generator($options)"
}
