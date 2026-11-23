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

import java.time.Duration

plugins {
    id("kapi.base")
}

kreate {
    assurance {
        standards {
            enabled = true
            reviewEvery = Duration.ofDays(365)

            inventory {
                includes.set(setOf("*.md", "kapi-*/src/**"))
            }

            github("OAI/OpenAPI-Specification") {
                note = "OpenAPI 3.0, 3.1 and 3.2: the documents kapi-openapi reads, checks and writes and kapi-generator " +
                    "turns into Kotlin."
            }
            unicode {
                note = "Names, descriptions and values of untrusted documents are shown through SafeText of Kore."
            }
        }
    }
}
