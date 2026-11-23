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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.runtime.core.problem

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.report.ValidationReport
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

private const val FIRST_STATUS: Int = 100

private const val LAST_STATUS: Int = 599

/**
 * A problem detail of RFC 9457, the body of `application/problem+json` that explains why a request failed.
 *
 * Besides the members of RFC 9457 a problem carries the members of kapi, [code] and [violations], and keeps every
 * other member as an extension, so a problem read from one service and written again loses nothing. When a problem is
 * read, a member whose value has the wrong type is ignored as RFC 9457 §3.1 demands, and so is a status outside
 * 100 to 599. Problems are written and read by [ProblemSerializer], which works with the JSON format and with the
 * formats of the runtime built on it, such as CBOR.
 *
 * Every text of a problem may reach a client: never put a secret, a stack trace or an internal detail into one.
 * [toString] shows the texts escaped. A problem is immutable when its extensions are, which every [JsonElement] is.
 *
 * @property type a URI reference that names the kind of problem; [BLANK_TYPE] when the status says it all.
 * @property title a short summary of the kind of problem, or `null`.
 * @property status the HTTP status code of the response, or `null`.
 * @property detail an explanation of this occurrence, or `null`.
 * @property instance a URI reference that names this occurrence, or `null`.
 * @property code the stable code of kapi that names the problem, or `null`.
 * @property violations the violations of an invalid request.
 * @param extensions every other member.
 * @throws IllegalArgumentException if [status] lies outside 100 to 599 or an extension takes the name of a member of
 * RFC 9457 or kapi.
 * @since 1.0.0
 */
@Serializable(with = ProblemSerializer::class)
public class Problem(
    public val type: String = BLANK_TYPE,
    public val title: String? = null,
    public val status: Int? = null,
    public val detail: String? = null,
    public val instance: String? = null,
    public val code: String? = null,
    public val violations: List<ProblemViolation> = emptyList(),
    extensions: Map<String, JsonElement> = emptyMap(),
) {
    /**
     * The members besides those of RFC 9457 and kapi, in the order they were given.
     *
     * @since 1.0.0
     */
    public val extensions: Map<String, JsonElement> = extensions.toMap()

    init {
        require(status == null || status in FIRST_STATUS..LAST_STATUS) { "A status lies between 100 and 599" }
        require(extensions.keys.none { name -> name in MEMBERS }) { "An extension cannot take the name of a member" }
    }

    private val identity: List<Any?>
        get() = listOf(type, title, status, detail, instance, code, violations, extensions)

    override fun equals(other: Any?): Boolean = other is Problem && identity == other.identity

    override fun hashCode(): Int = identity.hashCode()

    override fun toString(): String {
        val shown = listOf(
            "type" to type,
            "title" to title,
            "status" to status?.toString(),
            "detail" to detail,
            "instance" to instance,
            "code" to code,
        )
        val members = shown.mapNotNull { (name, value) -> value?.let { text -> "$name=${SafeText.text(text)}" } }
        val counts = "violations=${violations.size}, extensions=${extensions.keys.map(SafeText::text)}"
        return "Problem(${members.joinToString(", ")}, $counts)"
    }

    /**
     * The media type, the defaults and the problems kapi answers with.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The media type of a problem detail in JSON.
         *
         * @since 1.0.0
         */
        public const val MEDIA_TYPE: String = "application/problem+json"

        /**
         * The type of a problem that has no type of its own.
         *
         * @since 1.0.0
         */
        public const val BLANK_TYPE: String = "about:blank"

        /**
         * The status of an invalid request.
         *
         * @since 1.0.0
         */
        public const val INVALID_STATUS: Int = 400

        /**
         * The code of an invalid request.
         *
         * @since 1.0.0
         */
        public const val INVALID_CODE: String = "kapi.runtime.problem.invalid"

        internal val MEMBERS: Set<String> =
            setOf("type", "title", "status", "detail", "instance", "code", "violations")

        /**
         * Returns the problem that answers a request that failed validation, with every violation of [report].
         *
         * The paths of the violations are written as Kore renders them, with every untrusted part escaped.
         *
         * @param report the report of the validation.
         * @return the problem with status 400 and code [INVALID_CODE].
         * @since 1.0.0
         */
        public fun invalid(report: ValidationReport): Problem {
            val violations = report.violations.map { violation ->
                ProblemViolation(violation.path.toString(), violation.code.value, violation.message)
            }
            return Problem(
                title = "The request is invalid",
                status = INVALID_STATUS,
                code = INVALID_CODE,
                violations = violations,
            )
        }
    }
}
