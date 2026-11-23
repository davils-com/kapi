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

import com.davils.kapi.spec.asyncapi.read.ReadSession
import com.davils.kapi.spec.asyncapi.structure.OfficialSchema
import com.davils.kapi.spec.core.source.Source

/**
 * Reads AsyncAPI 3 documents, written as YAML or JSON, strictly and within the limits of its [configuration].
 *
 * A reading loads the root document and every document a reference names through the resolver, checks every field
 * against the specification, resolves the references, checks the rules of AsyncAPI and reports every finding with
 * its place. A reader is safe to share between threads; each reading runs in the coroutine of its caller. The only
 * state a reader keeps between readings is the official JSON Schema of AsyncAPI 3.0 that
 * [StructureCheck.ReaderAndOfficialSchema] validates against: it is compiled once, on the first reading that needs it,
 * within the limits of the configuration, and shared by every later reading.
 *
 * @property configuration the configuration of the reader.
 * @since 1.0.0
 */
public class Reader internal constructor(public val configuration: ReaderConfiguration) {
    private val schema: OfficialSchema = OfficialSchema(configuration.limits)

    /**
     * Reads the document of [source].
     *
     * @param source the root document, as text or loaded through the resolver.
     * @return the reading; a document that cannot be read is a [Reading.Malformed], never an exception.
     * @since 1.0.0
     */
    public suspend fun read(source: Source): Reading {
        val session = ReadSession(configuration = configuration, schema = schema, source = source)
        return session.run()
    }

    override fun toString(): String = "Reader($configuration)"
}
