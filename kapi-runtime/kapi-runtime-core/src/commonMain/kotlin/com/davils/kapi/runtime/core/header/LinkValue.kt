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

package com.davils.kapi.runtime.core.header

import com.davils.kapi.runtime.core.internal.ExtValue
import com.davils.kapi.runtime.core.internal.FieldGrammar
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.uri.UriReference

private const val REL: String = "rel"

private const val EXTENDED: Char = '*'

private const val HASH_FACTOR: Int = 31

/**
 * One link of a `Link` field of RFC 8288 §3, such as `<https://api.example.com/pets?page=2>; rel="next"`.
 *
 * The target is a URI reference of RFC 3986, relative ones resolved against the URL of the response by the caller;
 * it can carry neither a space nor a `>`. A parameter name is a token in lower case; a name ending in `*`, such as
 * `title*`, carries Unicode text written as an ext-value of RFC 8187, every other value consists of visible ASCII,
 * spaces and tabs. So [toString] always is a valid member of the field. A link is immutable.
 *
 * @property target the target URI reference, as written.
 * @param parameters the parameters in order, each name once.
 * @throws IllegalArgumentException if [target] is no URI reference, a name is no token or named twice, or a value
 * holds a character the field cannot carry, such as a line break.
 * @since 1.0.0
 */
public class LinkValue(public val target: String, parameters: List<Pair<String, String>> = emptyList()) {
    /**
     * The parameters in order, with their names in lower case.
     *
     * @since 1.0.0
     */
    public val parameters: List<Pair<String, String>> = parameters.map { (name, value) -> name.lowercase() to value }

    init {
        requireNotNull(UriReference.parseOrNull(target)) { "${SafeText.quoted(target)} is no URI reference" }
        val names = this.parameters.map { (name, _) -> name }
        require(names.all(FieldGrammar::isToken) && names.toSet().size == names.size) {
            "The parameters of a link have distinct names that are tokens"
        }
        require(this.parameters.all { (name, value) -> isExtended(name) || FieldGrammar.isSafeValue(value) }) {
            "A parameter of a link holds a character a field cannot carry"
        }
    }

    /**
     * The relation types of the link from its `rel` parameter, in lower case.
     *
     * @since 1.0.0
     */
    public val relations: List<String>
        get() {
            val relation = parameter(REL)
            val types = relation?.split(' ', '\t').orEmpty()
            return types.filter(String::isNotEmpty).map(String::lowercase)
        }

    /**
     * Returns the value of the parameter [name].
     *
     * @param name the name, compared without case.
     * @return the value, or `null` if the link has no such parameter.
     * @since 1.0.0
     */
    public fun parameter(name: String): String? {
        val key = name.lowercase()
        return parameters.firstOrNull { (candidate, _) -> candidate == key }?.second
    }

    override fun equals(other: Any?): Boolean =
        other is LinkValue && target == other.target && parameters == other.parameters

    override fun hashCode(): Int = target.hashCode() * HASH_FACTOR + parameters.hashCode()

    override fun toString(): String {
        val written = parameters.joinToString("") { (name, value) -> "; $name=" + valueText(name, value) }
        return "<$target>$written"
    }

    private fun valueText(name: String, value: String): String {
        if (isExtended(name)) return ExtValue.encode(value)
        return FieldGrammar.quote(value)
    }

    internal companion object {
        fun isExtended(name: String): Boolean = name.endsWith(EXTENDED)
    }
}
