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

package com.davils.kapi.spec.schema.suite

object SkipList {
    private const val FILE: String = "optional/ecmascript-regex.json"

    private const val UNICODE_SPACE: String =
        "ECMA-262 \\s and \\S cover every Unicode space and line terminator; the temporary pattern bridge runs the " +
            "patterns on the regex of the platform, whose \\s is ASCII only on the JVM"

    private const val CONTROL: String =
        "ECMA-262 \\c with a lower-case letter is a control character; the regex of the JVM accepts only upper case"

    private const val DIGIT_PROPERTY: String =
        "ECMA-262 has no property \\p{digit}, so the pattern is an error there; the regex of the JVM reads it as the " +
            "POSIX class of ASCII digits"

    private const val SPACE_MATCHES: String = "ECMA 262 \\s matches whitespace"

    private const val NON_SPACE_MATCHES: String = "ECMA 262 \\S matches everything but whitespace"

    private const val PATTERN_DIGITS: String = "pattern with non-ASCII digits"

    private const val PROPERTY_DIGITS: String = "patternProperties with non-ASCII digits"

    private const val BENGALI: String = "non-ascii digits (BENGALI DIGIT FOUR, BENGALI DIGIT TWO)"

    val reasons: Map<String, String> = buildMap {
        listOf("EM SPACE", "latin-1 non-breaking-space", "paragraph separator", "zero-width whitespace").forEach {
            put(key(SPACE_MATCHES, spaceMatches(it)), UNICODE_SPACE)
            put(key(NON_SPACE_MATCHES, spaceMismatches(it)), UNICODE_SPACE)
        }
        put(key("ECMA 262 regex escapes control codes with \\c and lower letter", "matches"), CONTROL)
        listOf(PATTERN_DIGITS, PROPERTY_DIGITS).forEach { group ->
            listOf("ascii digits", "ascii non-digits", BENGALI).forEach { test -> put(key(group, test), DIGIT_PROPERTY) }
        }
    }

    fun reasonOf(file: String, group: String, test: String): String? = reasons[listOf(file, group, test).joinToString(" / ")]

    private fun key(group: String, test: String): String = listOf(FILE, group, test).joinToString(" / ")

    private fun spaceMatches(space: String): String = space + mapOf(
        "EM SPACE" to " matches (Space_Separator)",
        "paragraph separator" to " matches (line terminator)",
    ).getOrDefault(space, " matches")

    private fun spaceMismatches(space: String): String = space + mapOf(
        "EM SPACE" to " does not match (Space_Separator)",
        "paragraph separator" to " does not match (line terminator)",
    ).getOrDefault(space, " does not match")
}
