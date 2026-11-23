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


package com.davils.kapi.codegen.core.pipeline

import com.davils.kapi.spec.core.source.SourceDocument
import com.davils.kapi.codegen.core.Fingerprint
import com.davils.kapi.codegen.core.GeneratedFile
import com.davils.kapi.codegen.core.GeneratedFileKind
import com.davils.kapi.codegen.core.Provenance
import com.davils.kapi.codegen.core.options.GeneratorOptions
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.toHexString
import java.security.MessageDigest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

private const val FOLDER: String = "META-INF/kapi/openapi/"

private const val SUFFIX: String = ".provenance.json"

private const val SEPARATOR: Char = '/'

private const val SHA_256: String = "SHA-256"

@OptIn(ExperimentalSerializationApi::class)
private val readable: Json = Json {
    encodeDefaults = true
    prettyPrint = true
    prettyPrintIndent = "  "
}

internal object Provenances {
    fun of(
        options: GeneratorOptions,
        sources: List<SourceDocument>,
        files: List<GeneratedFile>,
    ): Provenance = Provenance(
        protocol = Provenance.PROTOCOL,
        generator = options.generatorVersion,
        api = options.apiName,
        packageName = options.packageName,
        options = digestOf(canonical(options)),
        sources = sources.map { source -> fingerprint(source.uri.value.substringAfterLast(SEPARATOR), source.digest) },
        files = files.map { file -> Fingerprint(file.path, digestOf(file.content)) },
    )

    fun file(provenance: Provenance): GeneratedFile = GeneratedFile(
        path = FOLDER + provenance.api.lowercase() + SUFFIX,
        kind = GeneratedFileKind.Resource,
        content = readable.encodeToString(Provenance.serializer(), provenance) + "\n",
    )

    private fun canonical(options: GeneratorOptions): String {
        val artifacts = options.artifacts.map { artifact -> artifact.name }.sorted()
        val lines = listOf(
            "packageName=${options.packageName}",
            "apiName=${options.apiName}",
            "typePrefix=${options.typePrefix}",
            "artifacts=${artifacts.joinToString(",")}",
            "visibility=${options.visibility.name}",
        )
        return lines.joinToString("\n")
    }

    private fun digestOf(text: String): String {
        val digest = MessageDigest.getInstance(SHA_256).digest(text.encodeToByteArray())
        return rendered(ByteString(digest))
    }

    private fun fingerprint(name: String, digest: ByteString): Fingerprint =
        Fingerprint(name, rendered(digest))

    private fun rendered(digest: ByteString): String = "sha256:" + digest.toHexString()
}
