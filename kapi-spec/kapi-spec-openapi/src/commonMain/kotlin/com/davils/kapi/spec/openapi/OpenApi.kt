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

package com.davils.kapi.spec.openapi

import com.davils.kaml.core.Yaml
import com.davils.kapi.spec.openapi.write.DocumentWriter

/**
 * The entry point of OpenAPI in kapi: reads documents of OpenAPI 3.0, 3.1 and 3.2, and Swagger 2.0 by upgrading
 * them, and writes them back.
 *
 * ```kotlin
 * val reading = OpenApi.reader { limits { maximumDocumentLength = 4 * 1024 * 1024 } }.read(source)
 * when (reading) {
 *     is Reading.Read -> reading.document.paths
 *     is Reading.Invalid -> reading.diagnostics.render()
 *     is Reading.Malformed -> reading.diagnostics.render()
 * }
 * ```
 *
 * The object holds no state; every reader carries its own configuration and is safe to share between threads.
 *
 * @since 1.0.0
 */
public object OpenApi {
    /**
     * Returns a reader configured by [configure].
     *
     * @param configure sets the limits, the reference policy, the severities and the structure check of the reader;
     * the defaults suit documents from an untrusted source.
     * @return the reader, which may be used for any number of readings.
     * @throws IllegalArgumentException if the configuration is invalid, such as a limit above its hard ceiling.
     * @since 1.0.0
     */
    public fun reader(configure: ReaderBuilder.() -> Unit = {}): Reader {
        val builder = ReaderBuilder()
        builder.configure()
        return Reader(builder.build())
    }

    /**
     * Writes [document] as YAML in the form of its version family.
     *
     * Every value is written so that it reads back to the same document; text from the document cannot start a node
     * of its own, because the emitter of kaml quotes what cannot be written plain.
     *
     * @param document the document to write.
     * @return the YAML text of the document.
     * @since 1.0.0
     */
    public fun write(document: Document): String {
        val writer = DocumentWriter(document.version.family)
        return Yaml.Default.emit(writer.document(document))
    }
}
