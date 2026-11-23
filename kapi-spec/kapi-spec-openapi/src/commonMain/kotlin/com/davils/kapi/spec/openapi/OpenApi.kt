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

package com.davils.kapi.spec.openapi

import com.davils.kaml.core.Yaml
import com.davils.kapi.spec.core.compatibility.Comparison
import com.davils.kapi.spec.core.compatibility.ComparisonBuilder
import com.davils.kapi.spec.openapi.compatibility.DocumentComparison
import com.davils.kapi.spec.openapi.write.DocumentWriter

/**
 * The entry point of OpenAPI in kapi: reads documents of OpenAPI 3.0, 3.1 and 3.2, and Swagger 2.0 by upgrading
 * them, writes them back and compares two versions for compatibility.
 *
 * ```kotlin
 * val reading = OpenApi.reader { limits { maximumDocumentLength = 4 * 1024 * 1024 } }.read(source)
 * when (reading) {
 *     is Reading.Read -> reading.document.paths
 *     is Reading.Invalid -> reading.diagnostics.render()
 *     is Reading.Malformed -> reading.diagnostics.render()
 * }
 * ```
 *
 * The object holds no state; every reader carries its own configuration and is safe to share between threads.
 *
 * @since 1.0.0
 */
public object OpenApi {
    /**
     * Returns a reader configured by [configure].
     *
     * @param configure sets the limits, the reference policy, the severities and the structure check of the reader;
     * the defaults suit documents from an untrusted source.
     * @return the reader, which may be used for any number of readings.
     * @throws IllegalArgumentException if the configuration is invalid, such as a limit above its hard ceiling.
     * @since 1.0.0
     */
    public fun reader(configure: ReaderBuilder.() -> Unit = {}): Reader {
        val builder = ReaderBuilder()
        builder.configure()
        return Reader(builder.build())
    }

    /**
     * Compares [baseline] with [candidate], two versions of one API, and returns what changed and what it means for
     * clients written against the baseline and for providers implementing it.
     *
     * ```kotlin
     * val comparison = OpenApi.compare(baseline, candidate) { limits { maximumDepth = 64 } }
     * comparison.breakingForClients.forEach { change -> println(change) }
     * ```
     *
     * The comparison matches servers by their URL, paths by their shape, so a path that only renames its parameters
     * is the same path, operations by their method, parameters by their place and name, a path parameter by its
     * position, media types and headers by their name without case, responses by their status and security
     * requirements by the names of their schemes. It follows references through the targets of each document, one
     * hop at a time and at most [com.davils.kapi.spec.core.policy.ReadLimits.maximumReferenceHops] in a row, compares
     * every pair of schemas once, so cyclic schemas end, and judges a narrower schema of a request as breaking for
     * clients and a narrower schema of a response as breaking for providers. A schema with a reference is compared
     * through its target; its sibling keywords are left out. Callbacks and webhooks are not compared.
     *
     * Every change carries a code of [CompatibilityCodes] and the places of the changed parts. The comparison stays
     * within the limits of [configure]: what lies beyond the depth, the reference hops or the steps, and the changes
     * beyond the most it reports, become one change of [CompatibilityCodes.INCOMPLETE] each instead of being dropped
     * silently. Both documents are only read; the comparison runs in the thread of the caller and shares no state.
     *
     * @param baseline the version clients and providers were written against.
     * @param candidate the version that replaces it.
     * @param configure sets the limits of the comparison; the defaults are those of a reading.
     * @return the changes in the order they were found.
     * @throws com.davils.kore.core.validation.report.ValidationException if the configuration is invalid, such as a
     * limit above its hard ceiling.
     * @since 1.0.0
     */
    public fun compare(
        baseline: Document,
        candidate: Document,
        configure: ComparisonBuilder.() -> Unit = {},
    ): Comparison {
        val builder = ComparisonBuilder()
        builder.configure()
        val comparison = DocumentComparison(baseline = baseline, candidate = candidate, settings = builder.build())
        return comparison.compare()
    }

    /**
     * Writes [document] as YAML in the form of its version family.
     *
     * Every value is written so that it reads back to the same document; text from the document cannot start a node
     * of its own, because the emitter of kaml quotes what cannot be written plain. What the version family of
     * [document] cannot express is left out, such as the fields that OpenAPI 3.2 adds when an older family is written:
     * a parameter `in: querystring` is dropped from the components and from every list of parameters, also where the
     * list refers to it, and the style `cookie` is not written, so the default style of the location applies.
     *
     * @param document the document to write.
     * @return the YAML text of the document.
     * @since 1.0.0
     */
    public fun write(document: Document): String {
        val writer = DocumentWriter(document)
        return Yaml.Default.emit(writer.document(document))
    }
}
