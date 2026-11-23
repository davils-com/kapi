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

import com.davils.kapi.codegen.core.GeneratedFile
import com.davils.kapi.codegen.core.GeneratedFileKind
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Files

private const val PLUGIN: String = "kotlin-serialization-compiler-plugin-embeddable"

internal object Compiler {
    fun compile(files: List<GeneratedFile>, sources: Map<String, String> = emptyMap()): Compilation {
        val root = Files.createTempDirectory("kapi-asyncapi")
        val kotlin = files.filter { file -> file.kind == GeneratedFileKind.Kotlin }
        val written = kotlin.associate { file -> file.path to file.content } + sources
        written.forEach { (path, content) ->
            val target = root.resolve("src").resolve(path)
            Files.createDirectories(target.parent)
            Files.writeString(target, content)
        }
        val classes = root.resolve("classes")
        val classPath = System.getProperty("java.class.path").split(File.pathSeparator)
        val plugin = classPath.first { entry -> PLUGIN in entry }
        val output = ByteArrayOutputStream()
        val code = PrintStream(output).use { stream ->
            K2JVMCompiler().exec(
                stream,
                "-d",
                classes.toString(),
                "-classpath",
                classPath.joinToString(File.pathSeparator),
                "-Xplugin=$plugin",
                "-no-stdlib",
                "-no-reflect",
                "-Xexplicit-api=strict",
                "-Werror",
                root.resolve("src").toString(),
            )
        }
        return Compilation(output.toString(), code == ExitCode.OK, classes)
    }
}
