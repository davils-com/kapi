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

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.davils.kapi")
}

fun classpathOf(name: String): List<String> = providers.gradleProperty(name).get().split(File.pathSeparator)

dependencies {
    implementation(files(classpathOf("kapiJvm")))
}

kapi {
    generatorClasspath.from(classpathOf("kapiGenerator"))
    runtimes {
        enabled = false
    }
    openApi("petstore") {
        packageName = "com.example.petstore"
        artifacts {
            client = true
            server = true
        }
    }
}
