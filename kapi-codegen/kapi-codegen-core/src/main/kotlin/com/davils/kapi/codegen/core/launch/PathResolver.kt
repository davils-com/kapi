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

package com.davils.kapi.codegen.core.launch

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.source.Refusal
import com.davils.kapi.spec.core.source.Resolution
import com.davils.kapi.spec.core.source.Resolver
import kotlinx.io.bytestring.ByteString
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path

private const val FILE: String = "file"

internal class PathResolver(root: Path, private val maximumLength: Int) : Resolver {
    private val root: Path = root.toRealPath()

    override suspend fun resolve(uri: DocumentUri): Resolution {
        val local = uri.takeIf { found -> found.scheme == FILE }
        val path = local?.let(::pathOf) ?: return Resolution.Refused(Refusal.Unresolvable)
        return read(path)
    }

    private fun read(path: Path): Resolution = try {
        val real = path.toRealPath()
        val isInside = real.startsWith(root) && Files.isRegularFile(real)
        val isSmall = isInside && Files.size(real) <= maximumLength
        answer(real, isInside, isSmall)
    } catch (_: NoSuchFileException) {
        Resolution.Missing
    } catch (failure: IOException) {
        Resolution.Failed(failure.javaClass.simpleName)
    }

    private fun answer(path: Path, isInside: Boolean, isSmall: Boolean): Resolution {
        val refusal = Refusal.Confinement.takeUnless { isInside } ?: Refusal.Size.takeUnless { isSmall }
        if (refusal != null) return Resolution.Refused(refusal)
        return Resolution.Found(ByteString(Files.readAllBytes(path)))
    }

    private fun pathOf(uri: DocumentUri): Path? = try {
        Path.of(URI(uri.value))
    } catch (_: URISyntaxException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    companion object {
        fun uriOf(path: Path): DocumentUri {
            val normal = path.toAbsolutePath().normalize()
            val uri = normal.toUri()
            return DocumentUri(uri.toString())
        }
    }
}
