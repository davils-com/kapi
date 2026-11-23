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

package com.davils.kapi.spec.testing

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.source.Source

/**
 * A corpus of OpenAPI documents for tests: petstores, kitchen sinks, every parameter style, security combinations,
 * callbacks and webhooks, links, content negotiation, read-only and write-only models, OpenAPI 3.2 and a hostile
 * document. Every document but `hostile` reads without an error.
 *
 * The documents are part of the code, so the corpus works on every platform without files or resources.
 *
 * @since 1.0.0
 */
public object Corpus {
    /**
     * The names of the OpenAPI documents of the corpus, sorted.
     *
     * @since 1.0.0
     */
    public val openApiNames: List<String> = listOf(
        "additional-properties",
        "callbacks-and-webhooks",
        "content-negotiation-request",
        "feature-matrix",
        "hostile",
        "kitchen-sink",
        "mail-routes",
        "mail",
        "neutral",
        "openapi-32",
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

    /**
     * Returns the text of the OpenAPI document [name].
     *
     * @param name one of [openApiNames].
     * @return the YAML text of the document.
     * @throws IllegalArgumentException if the corpus holds no such document.
     * @since 1.0.0
     */
    public fun openApiText(name: String): String =
        requireNotNull(openApiTexts[name]) { "The corpus holds no OpenAPI document named $name" }

    /**
     * Returns the OpenAPI document [name] as a source at `corpus:/<name>.yaml`.
     *
     * @param name one of [openApiNames].
     * @return the source.
     * @throws IllegalArgumentException if the corpus holds no such document.
     * @since 1.0.0
     */
    public fun openApi(name: String): Source.Text = Source.Text(openApiText(name), DocumentUri("corpus:/$name.yaml"))
}
