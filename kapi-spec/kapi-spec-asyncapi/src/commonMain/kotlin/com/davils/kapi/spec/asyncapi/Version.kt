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

package com.davils.kapi.spec.asyncapi

private const val MAXIMUM_LITERAL_LENGTH: Int = 64

private const val PATCH_DOTS: Int = 2

/**
 * The version a document declares in its field `asyncapi`, such as `3.0.0`.
 *
 * A version is read with [parseOrNull], which accepts `3.0` with an optional patch and an optional pre-release of
 * letters, digits, dots and hyphens, such as `3.0.0-rc1`, and refuses everything else, every text longer than 64
 * characters included. Its [literal] is therefore safe to show as it is.
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
     * Whether the version names a patch, such as `3.0.0`; AsyncAPI requires one.
     *
     * @since 1.0.0
     */
    public val hasPatch: Boolean
        get() {
            val core = literal.substringBefore('-')
            return core.count { character -> character == '.' } >= PATCH_DOTS
        }

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
         * The newest version kapi reads and writes, `3.0.0`.
         *
         * @since 1.0.0
         */
        public val Latest: Version = Version(VersionFamily.V30, "3.0.0")

        /**
         * Reads [text] as an AsyncAPI version.
         *
         * @param text the text of the field `asyncapi`, which may contain anything.
         * @return the version, or `null` if [text] is no version of a line kapi reads.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): Version? {
            if (text.length > MAXIMUM_LITERAL_LENGTH) return null
            val family = VersionFamily.entries.firstOrNull { family -> isOfFamily(text, family) }
            return family?.let { known -> Version(known, text) }
        }

        private fun isOfFamily(text: String, family: VersionFamily): Boolean {
            if (!text.startsWith(family.prefix)) return false
            val rest = text.removePrefix(family.prefix)
            val core = rest.substringBefore('-')
            val preRelease = rest.substringAfter('-', missingDelimiterValue = "")
            val hasPreRelease = '-' in rest
            val isCore = core.isEmpty() || isPatch(core)
            val isPreRelease = !hasPreRelease || (preRelease.isNotEmpty() && preRelease.all(::isPreReleaseCharacter))
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
