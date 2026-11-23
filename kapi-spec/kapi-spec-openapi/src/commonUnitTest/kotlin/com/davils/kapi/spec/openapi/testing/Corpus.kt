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

package com.davils.kapi.spec.openapi.testing

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.source.Source

internal object Corpus {
    val names: List<String> = listOf(
        "additional-properties",
        "callbacks-and-webhooks",
        "content-negotiation-request",
        "feature-matrix",
        "hostile",
        "kitchen-sink",
        "mail-routes",
        "mail",
        "neutral",
        "openobject",
        "outbound",
        "parameters-constraints",
        "parameters-cookie",
        "parameters-path-styles",
        "parameters-style-query",
        "petstore",
        "readwrite-split",
        "readwrite",
        "response-links",
        "responses-default",
        "security-and-or",
        "security",
        "styles",
    )

    fun text(name: String): String = requireNotNull(corpusTexts[name]) { "The corpus holds no document named $name" }

    fun source(name: String): Source.Text = Source.Text(text(name), DocumentUri("corpus:/$name.yaml"))
}
