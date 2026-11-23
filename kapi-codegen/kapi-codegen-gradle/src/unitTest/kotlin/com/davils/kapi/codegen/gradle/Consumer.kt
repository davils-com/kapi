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

package com.davils.kapi.codegen.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.io.path.writeText

private const val FIXTURE: String = "/com/davils/kapi/codegen/gradle/fixture/"

private const val SEPARATOR: String = "/"

private const val BACKSLASH: String = "\\"

private val skipped: Set<String> = setOf("build", ".gradle", ".kotlin")

private fun classpathOf(property: String): List<File> {
    val value = System.getProperty(property).orEmpty()
    val entries = value.split(File.pathSeparator)
    return entries.filter(String::isNotEmpty).map(::File)
}

private fun portable(path: String): String = path.replace(oldValue = BACKSLASH, newValue = SEPARATOR)

internal fun fixture(name: String): String {
    val stream = Consumer::class.java.getResourceAsStream(FIXTURE + name)
    val bytes = stream?.use { found -> found.readAllBytes() } ?: error("The fixture $name is missing")
    return String(bytes, Charsets.UTF_8)
}

internal class Consumer(val directory: Path, private val cache: Path, private val isInProcess: Boolean) {
    init {
        write(path = "settings.gradle.kts", content = fixture("settings.gradle.kts"))
        write(path = "gradle.properties", content = properties())
    }

    fun write(path: String, content: String) {
        val target = directory.resolve(path)
        target.parent.createDirectories()
        target.writeText(content)
    }

    fun read(path: String): String = directory.resolve(path).readText()

    fun exists(path: String): Boolean = Files.exists(directory.resolve(path))

    fun files(path: String): List<String> {
        val folder = directory.resolve(path)
        if (!Files.isDirectory(folder)) return emptyList()
        val names = Files.walk(folder).use { paths ->
            val found = paths.filter(Files::isRegularFile)
            found.map { file -> folder.relativize(file).joinToString(SEPARATOR) }.toList()
        }
        return names.sorted()
    }

    fun build(vararg arguments: String): BuildResult = runner(arguments.toList()).build()

    fun fail(vararg arguments: String): BuildResult = runner(arguments.toList()).buildAndFail()

    fun copy(): Consumer {
        val target = createTempDirectory("kapi-consumer")
        Files.walk(directory).use { paths ->
            val kept = paths.filter { path -> isKept(directory.relativize(path)) }
            kept.forEach { path ->
                val relative = directory.relativize(path)
                copyTo(source = path, target = target.resolve(relative.toString()))
            }
        }
        return Consumer(directory = target, cache = cache, isInProcess = isInProcess)
    }

    private fun isKept(relative: Path): Boolean {
        val first = relative.firstOrNull() ?: return true
        return first.toString() !in skipped
    }

    private fun copyTo(source: Path, target: Path) {
        if (Files.isDirectory(source)) {
            target.createDirectories()
            return
        }
        Files.copy(source, target)
    }

    private fun runner(arguments: List<String>): GradleRunner {
        val runner = GradleRunner.create()
        runner.withProjectDir(directory.toFile())
        runner.withPluginClasspath(classpathOf("kapi.test.plugin"))
        runner.withDebug(isInProcess)
        return runner.withArguments(arguments + "--stacktrace")
    }

    private fun properties(): String {
        val lines = listOf(
            "kotlin.stdlib.default.dependency=false",
            "kotlin.daemon.jvmargs=-Xmx1g",
            "kapiCache=${portable(cache.toString())}",
            "kapiGenerator=${property("kapi.test.generator")}",
            "kapiJvm=${property("kapi.test.jvm")}",
            "kapiWasm=${property("kapi.test.wasm")}",
        )
        return lines.joinToString(separator = "\n", postfix = "\n")
    }

    private fun property(name: String): String {
        val paths = classpathOf(name).map { file -> portable(file.path) }
        return paths.joinToString(File.pathSeparator)
    }

    companion object {
        fun create(isInProcess: Boolean = true): Consumer = Consumer(
            directory = createTempDirectory("kapi-consumer"),
            cache = createTempDirectory("kapi-cache"),
            isInProcess = isInProcess,
        )
    }
}

internal fun BuildResult.outcomeOf(path: String): TaskOutcome? = task(path)?.outcome
