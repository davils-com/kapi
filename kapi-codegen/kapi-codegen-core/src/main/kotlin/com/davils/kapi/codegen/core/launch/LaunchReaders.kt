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

import com.davils.kapi.spec.asyncapi.AsyncApi
import com.davils.kapi.spec.openapi.OpenApi
import java.nio.file.Path
import com.davils.kapi.spec.asyncapi.Reader as ChannelReader
import com.davils.kapi.spec.openapi.Reader as DocumentReader

private const val MAXIMUM_DOCUMENT_LENGTH: Int = 16 * 1024 * 1024

internal class LaunchReaders(private val root: Path) {
    fun openApi(): DocumentReader = OpenApi.reader {
        resolver = PathResolver(root, MAXIMUM_DOCUMENT_LENGTH)
        references { this.root = PathResolver.uriOf(this@LaunchReaders.root.resolve(".")) }
    }

    fun asyncApi(): ChannelReader = AsyncApi.reader {
        resolver = PathResolver(root, MAXIMUM_DOCUMENT_LENGTH)
        references { this.root = PathResolver.uriOf(this@LaunchReaders.root.resolve(".")) }
    }
}
