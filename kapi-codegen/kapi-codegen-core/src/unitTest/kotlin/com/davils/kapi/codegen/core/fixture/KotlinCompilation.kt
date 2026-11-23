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

package com.davils.kapi.codegen.core.fixture

import com.davils.kapi.codegen.core.GeneratedFile
import io.kotest.matchers.collections.shouldBeEmpty
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeText

private const val PREFIX: String = "kapi-codegen-compilation"

private const val CLASSES: String = "classes"

private const val SERIALIZATION_PLUGIN: String = "kotlin-serialization-compiler-plugin-embeddable"

internal object KotlinCompilation {
    fun problems(files: List<GeneratedFile>): List<String> {
        if (Golden.kotlin(files).isEmpty()) return emptyList()
        return compiled(files)
    }

    @OptIn(ExperimentalPathApi::class)
    suspend fun <T> loaded(files: List<GeneratedFile>, block: suspend (ClassLoader) -> T): T {
        val root = createTempDirectory(PREFIX)
        try {
            compile(root, files).shouldBeEmpty()
            val classes = root.resolve(CLASSES).toUri()
            val parent = KotlinCompilation::class.java.classLoader
            return URLClassLoader(arrayOf(classes.toURL()), parent).use { loader -> block(loader) }
        } finally {
            root.deleteRecursively()
        }
    }

    @OptIn(ExperimentalPathApi::class)
    private fun compiled(files: List<GeneratedFile>): List<String> {
        val root = createTempDirectory(PREFIX)
        val problems = compile(root, files)
        root.deleteRecursively()
        return problems
    }

    private fun compile(root: Path, files: List<GeneratedFile>): List<String> {
        val sources = root.resolve("sources")
        Golden.kotlin(files).forEach { file ->
            val target = sources.resolve(file.path)
            target.parent.createDirectories()
            target.writeText(file.content)
        }
        val collector = RecordingCollector()
        val arguments = arguments(sources = sources.toString(), classes = root.resolve(CLASSES).toString())
        K2JVMCompiler().exec(collector, Services.EMPTY, arguments)
        return collector.messages
    }

    private fun arguments(sources: String, classes: String): K2JVMCompilerArguments {
        val classPath = System.getProperty("java.class.path")
        val entries = classPath.split(File.pathSeparator)
        val plugin = entries.first { entry -> SERIALIZATION_PLUGIN in entry }
        val arguments = K2JVMCompilerArguments()
        arguments.freeArgs = listOf(sources)
        arguments.destination = classes
        arguments.classpath = classPath
        arguments.noStdlib = true
        arguments.noReflect = true
        arguments.allWarningsAsErrors = true
        arguments.explicitApi = "strict"
        arguments.jvmTarget = "21"
        arguments.pluginClasspaths = arrayOf(plugin)
        return arguments
    }
}
