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

private const val MAXIMUM_LITERAL_LENGTH: Int = 64

/**
 * The version a document declares in its field `openapi`, such as `3.1.1` or `3.2.0`.
 *
 * A version is read with [parseOrNull], which accepts `3.0`, `3.1` and `3.2` with an optional patch and an optional
 * pre-release of letters, digits, dots and hyphens, such as `3.2.0-rc1`, and refuses everything else, every text
 * longer than 64 characters included. Its [literal] is therefore safe to show as it is.
 *
 * @property family the line of the version.
 * @property literal the version as it was written.
 * @since 1.0.0
 */
public class Version private constructor(
    public val family: VersionFamily,
    public val literal: String,
) {
    /**
     * Whether the version names a patch, such as `3.1.1`; OpenAPI requires one.
     *
     * @since 1.0.0
     */
    public val hasPatch: Boolean get() = literal.substringBefore('-').count { character -> character == '.' } > 1

    override fun equals(other: Any?): Boolean = other is Version && literal == other.literal

    override fun hashCode(): Int = literal.hashCode()

    override fun toString(): String = literal

    /**
     * Reads versions.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The newest version kapi reads and writes, `3.2.0`.
         *
         * @since 1.0.0
         */
        public val Latest: Version = Version(VersionFamily.V32, "3.2.0")

        /**
         * The version Swagger 2.0 documents are upgraded to, `3.1.1`.
         *
         * @since 1.0.0
         */
        public val Upgraded: Version = Version(VersionFamily.V31, "3.1.1")

        /**
         * Reads [text] as an OpenAPI version.
         *
         * @param text the text of the field `openapi`, which may contain anything.
         * @return the version, or `null` if [text] is no version of 3.0, 3.1 or 3.2.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): Version? {
            if (text.length > MAXIMUM_LITERAL_LENGTH) return null
            val family = VersionFamily.entries.firstOrNull { family -> isOfFamily(text, family) }
            return family?.let { known -> Version(known, text) }
        }

        private fun isOfFamily(text: String, family: VersionFamily): Boolean {
            val rest = text.removePrefix(family.prefix).takeIf { text.startsWith(family.prefix) } ?: return false
            val core = rest.substringBefore('-')
            val preRelease = rest.substringAfter('-', missingDelimiterValue = "")
            val hasPreRelease = '-' in rest
            val isCore = core.isEmpty() || isPatch(core)
            val isPreRelease = !hasPreRelease || preRelease.isNotEmpty() && preRelease.all(::isPreReleaseCharacter)
            return isCore && isPreRelease
        }

        private fun isPatch(text: String): Boolean {
            val digits = text.removePrefix(".")
            return text.startsWith('.') && digits.isNotEmpty() && digits.all { character -> character in '0'..'9' }
        }

        private fun isPreReleaseCharacter(character: Char): Boolean = character in '0'..'9' ||
            character in 'a'..'z' ||
            character in 'A'..'Z' ||
            character == '.' ||
            character == '-'
    }
}
