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

/**
 * A file the generator produced.
 *
 * @property path the path of the file relative to the root of its kind, with `/` as separator, such as
 * `com/example/petstore/model/Pet.kt`.
 * @property kind whether the file is Kotlin or a resource.
 * @property content the text of the file.
 * @property platform the platform the file needs: models and clients run everywhere, servers and mocks on the JVM.
 * @since 1.0.0
 */
public class GeneratedFile(
    public val path: String,
    public val kind: GeneratedFileKind,
    public val content: String,
    public val platform: GeneratedPlatform = GeneratedPlatform.Common,
) {
    override fun equals(other: Any?): Boolean = other is GeneratedFile && path == other.path && kind == other.kind &&
        content == other.content && platform == other.platform

    override fun hashCode(): Int = listOf(path, kind, content, platform).hashCode()

    override fun toString(): String = "GeneratedFile($path, $kind, $platform, ${content.length} characters)"
}
