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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText

private const val LIMIT: Int = 16

private const val SMALL: String = "a: 1"

private fun workspace(): Path {
    val base = createTempDirectory("kapi-resolver")
    val root = base.resolve("api").createDirectories()
    root.resolve("small.yaml").writeText(SMALL)
    root.resolve("large.yaml").writeText("a".repeat(LIMIT + 1))
    root.resolve("folder").createDirectories()
    base.resolve("outside.yaml").writeText("b: 2")
    return root
}

private suspend fun resolved(root: Path, uri: DocumentUri): Resolution = PathResolver(root, LIMIT).resolve(uri)

class PathResolverTest : FunSpec({
    context("a resolver of files below a root") {
        val root = workspace()

        test("reads a small file inside its root") {
            val found = resolved(
                root,
                PathResolver.uriOf(root.resolve("small.yaml")),
            ).shouldBeInstanceOf<Resolution.Found>()

            found.content.size shouldBe SMALL.length
        }

        test("refuses a file outside its root, a folder and a file above its limit") {
            val outside = resolved(root, PathResolver.uriOf(root.resolveSibling("outside.yaml")))
            val folder = resolved(root, PathResolver.uriOf(root.resolve("folder")))
            val large = resolved(root, PathResolver.uriOf(root.resolve("large.yaml")))

            outside.shouldBeInstanceOf<Resolution.Refused>().refusal shouldBe Refusal.Confinement
            folder.shouldBeInstanceOf<Resolution.Refused>().refusal shouldBe Refusal.Confinement
            large.shouldBeInstanceOf<Resolution.Refused>().refusal shouldBe Refusal.Size
        }

        test("finds nothing for a file that does not exist") {
            resolved(root, PathResolver.uriOf(root.resolve("missing.yaml"))) shouldBe Resolution.Missing
        }

        test("refuses what is no file and what is no URI") {
            val remote = resolved(root, DocumentUri("https://example.com/api.yaml"))
            val broken = resolved(root, DocumentUri("file://example.com/api.yaml"))

            remote.shouldBeInstanceOf<Resolution.Refused>().refusal shouldBe Refusal.Unresolvable
            broken.shouldBeInstanceOf<Resolution.Refused>().refusal shouldBe Refusal.Unresolvable
        }

        test("fails on a file it cannot read") {
            val locked = root.resolve("locked.yaml")
            locked.writeText("c: 3")
            Files.setPosixFilePermissions(locked, emptySet())

            resolved(root, PathResolver.uriOf(locked)).shouldBeInstanceOf<Resolution.Failed>()
        }
    }
})
