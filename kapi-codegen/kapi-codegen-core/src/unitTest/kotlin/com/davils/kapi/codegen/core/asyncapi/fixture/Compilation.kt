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

package com.davils.kapi.codegen.core.asyncapi.fixture

import java.net.URLClassLoader
import java.nio.file.Path

internal class Compilation(val messages: String, val isSuccessful: Boolean, private val classes: Path) {
    fun <T> loaded(name: String, block: (Class<*>) -> T): T {
        val urls = arrayOf(classes.toUri().toURL())
        return URLClassLoader(urls, Compilation::class.java.classLoader).use { loader -> block(loader.loadClass(name)) }
    }
}
