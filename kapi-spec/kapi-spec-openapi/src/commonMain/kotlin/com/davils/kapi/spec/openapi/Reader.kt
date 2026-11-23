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

import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.openapi.read.ReadSession
import com.davils.kapi.spec.openapi.structure.OfficialSchemas

/**
 * Reads OpenAPI documents with one [configuration]; built by [OpenApi.reader].
 *
 * A reader is safe to share between threads; each reading loads its documents through the resolver of the
 * configuration, from the coroutine of the caller. The only state a reader keeps between readings are the official
 * JSON Schemas of OpenAPI that [StructureCheck.ReaderAndOfficialSchema] validates against: each is compiled once, on
 * the first reading of its version family, within the limits of the configuration, and shared by every later reading.
 *
 * @property configuration the configuration of every reading.
 * @since 1.0.0
 */
public class Reader internal constructor(public val configuration: ReaderConfiguration) {
    private val schemas: OfficialSchemas = OfficialSchemas(configuration.limits)

    /**
     * Reads [source].
     *
     * @param source the document.
     * @return the outcome; a reading never throws for a problem of the document.
     * @since 1.0.0
     */
    public suspend fun read(source: Source): Reading = read(source, emptyList())

    /**
     * Reads [source] after applying [overlays] in order.
     *
     * @param source the document.
     * @param overlays the OpenAPI Overlays to apply.
     * @return the outcome; a reading never throws for a problem of the document.
     * @since 1.0.0
     */
    public suspend fun read(source: Source, overlays: List<Source>): Reading =
        ReadSession(configuration = configuration, schemas = schemas, source = source, overlays = overlays).run()

    override fun toString(): String = "Reader($configuration)"
}
