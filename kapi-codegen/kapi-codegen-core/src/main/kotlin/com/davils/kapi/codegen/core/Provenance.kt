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

package com.davils.kapi.codegen.core

import kotlinx.serialization.Serializable

/**
 * Where the files of a generation come from: the generator, the options, the documents it read and the digest of
 * every file it wrote, so that a build can prove that the generated code is what the document says.
 *
 * @property protocol the version of this record, [PROTOCOL].
 * @property generator the version of the generator.
 * @property api the name of the API.
 * @property packageName the package of the generated code.
 * @property options the digest of the options.
 * @property sources the digest of every document the generation read.
 * @property files the digest of every file the generation wrote.
 * @since 1.0.0
 */
@Serializable
public class Provenance(
    public val protocol: Int,
    public val generator: String,
    public val api: String,
    public val packageName: String,
    public val options: String,
    public val sources: List<Fingerprint>,
    public val files: List<Fingerprint>,
) {
    private val parts: List<Any> get() = listOf(protocol, generator, api, packageName, options, sources, files)

    override fun equals(other: Any?): Boolean = other is Provenance && parts == other.parts

    override fun hashCode(): Int = parts.hashCode()

    override fun toString(): String = "Provenance($api, $packageName, ${files.size} files)"

    /**
     * The constants of the record.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The version of the record this generator writes.
         *
         * @since 1.0.0
         */
        public const val PROTOCOL: Int = 1
    }
}
