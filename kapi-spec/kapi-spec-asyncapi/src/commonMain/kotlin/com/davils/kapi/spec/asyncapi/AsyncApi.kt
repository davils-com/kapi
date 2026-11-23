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

package com.davils.kapi.spec.asyncapi

import com.davils.kaml.core.Yaml
import com.davils.kapi.spec.asyncapi.compatibility.DocumentComparison
import com.davils.kapi.spec.asyncapi.write.DocumentWriter
import com.davils.kapi.spec.core.compatibility.Comparison
import com.davils.kapi.spec.core.compatibility.ComparisonBuilder
import com.davils.kore.core.validation.report.ValidationException

/**
 * The entry point of AsyncAPI in kapi: reads documents of AsyncAPI 3.0 and writes them back.
 *
 * ```kotlin
 * val reading = AsyncApi.reader { limits { maximumDocumentLength = 4 * 1024 * 1024 } }.read(source)
 * when (reading) {
 *     is Reading.Read -> reading.document.channels
 *     is Reading.Invalid -> reading.diagnostics.render()
 *     is Reading.Malformed -> reading.diagnostics.render()
 * }
 * ```
 *
 * The object holds no state; every reader carries its own configuration and is safe to share between threads.
 *
 * @since 1.0.0
 */
public object AsyncApi {
    /**
     * Returns a reader configured by [configure].
     *
     * @param configure sets the resolver, the limits, the reference policy and the severities of the reader; the
     * defaults suit documents from an untrusted source.
     * @return the reader, which may be used for any number of readings.
     * @throws ValidationException if the configuration is invalid, such as a limit above its hard ceiling.
     * @since 1.0.0
     */
    public fun reader(configure: ReaderBuilder.() -> Unit = {}): Reader {
        val builder = ReaderBuilder()
        builder.configure()
        return Reader(builder.build())
    }

    /**
     * Compares [baseline] with [candidate], two versions of one API, and returns what changed and what it means for
     * clients of the application the baseline describes and for implementations of it.
     *
     * ```kotlin
     * val comparison = AsyncApi.compare(baseline, candidate) { limits { maximumDepth = 64 } }
     * check(!comparison.isBreakingForClients) { comparison.breakingForClients.joinToString() }
     * ```
     *
     * The comparison matches servers, channels and operations by their keys and the messages of an operation by the
     * keys they have in their channel. It compares an operation and its messages with their traits applied. The
     * payload and the headers of the messages of a `send` operation are compared as data the application sends, those
     * of a `receive` operation as data it receives, so a narrower payload of a `receive` operation breaks clients and
     * a narrower payload of a `send` operation breaks implementations. Schemas of AsyncAPI, JSON Schema and OpenAPI
     * are compared keyword by keyword, references inside them are followed through the targets of each document
     * within the reference hops of the limits, and a schema of another format, such as Avro, is compared as it was
     * written. Bindings are compared as a whole.
     *
     * Every change carries a code of [CompatibilityCodes] and the places of the changed parts. The comparison stays
     * within the limits of [configure]: what lies beyond the depth, the reference hops or the steps, and the changes
     * beyond the most it reports, become one change of [CompatibilityCodes.INCOMPLETE] each instead of being dropped
     * silently. Both documents are only read; the comparison runs in the thread of the caller and shares no state.
     *
     * @param baseline the version clients and implementations were written against.
     * @param candidate the version that replaces it.
     * @param configure sets the limits of the comparison; the defaults are those of a reading.
     * @return the changes in the order they were found.
     * @throws ValidationException if the configuration is invalid, such as a limit above its hard ceiling.
     * @since 1.0.0
     */
    public fun compare(
        baseline: Document,
        candidate: Document,
        configure: ComparisonBuilder.() -> Unit = {},
    ): Comparison {
        val builder = ComparisonBuilder()
        builder.configure()
        val comparison = DocumentComparison(baseline = baseline, candidate = candidate, settings = builder.build())
        return comparison.compare()
    }

    /**
     * Writes [document] as YAML.
     *
     * Every value is written so that it reads back to the same document; text from the document cannot start a node
     * of its own, because the emitter of kaml quotes what cannot be written plain.
     *
     * @param document the document to write.
     * @return the YAML text of the document.
     * @since 1.0.0
     */
    public fun write(document: Document): String {
        val writer = DocumentWriter()
        return Yaml.Default.emit(writer.document(document))
    }
}
