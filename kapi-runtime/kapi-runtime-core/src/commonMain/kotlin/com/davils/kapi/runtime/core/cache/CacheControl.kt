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

package com.davils.kapi.runtime.core.cache

import com.davils.kapi.runtime.core.FieldLimits
import com.davils.kapi.runtime.core.internal.FieldGrammar
import com.davils.kapi.runtime.core.internal.FieldLists
import com.davils.kapi.runtime.core.internal.FieldScanner

private const val MOST_DELTA: Long = 2_147_483_648L

private const val MOST_DIGITS: Int = 10

/**
 * The value of `Cache-Control` of RFC 9111 §5.2: the directives of a request or a response, in order.
 *
 * The accessors read the directives of RFC 9111 §5.2.1 and §5.2.2, `stale-while-revalidate` and `stale-if-error` of
 * RFC 5861 and `immutable` of RFC 8246. When a directive appears more than once, the first occurrence counts, as
 * RFC 9111 §4.2.1 allows; a delta of seconds above 2^31 counts as 2^31 as §1.2.2 demands, and a malformed one counts
 * as absent. Unknown directives are kept, so a cache-control written back loses nothing. A value is immutable;
 * [toString] is the field value.
 *
 * @param directives the directives in order.
 * @since 1.0.0
 */
public class CacheControl(directives: List<CacheDirective>) {
    /**
     * The directives in order.
     *
     * @since 1.0.0
     */
    public val directives: List<CacheDirective> = directives.toList()

    /**
     * `max-age` in seconds, or `null`.
     *
     * @since 1.0.0
     */
    public val maxAge: Long? get() = seconds(MAX_AGE)

    /**
     * `s-maxage` in seconds, or `null`.
     *
     * @since 1.0.0
     */
    public val sharedMaxAge: Long? get() = seconds(SHARED_MAX_AGE)

    /**
     * `max-stale` in seconds, or `null` if it is absent or has no argument; see [isMaxStale].
     *
     * @since 1.0.0
     */
    public val maxStale: Long? get() = seconds(MAX_STALE)

    /**
     * Whether `max-stale` is present, with or without an argument.
     *
     * @since 1.0.0
     */
    public val isMaxStale: Boolean get() = MAX_STALE in this

    /**
     * `min-fresh` in seconds, or `null`.
     *
     * @since 1.0.0
     */
    public val minFresh: Long? get() = seconds(MIN_FRESH)

    /**
     * `stale-while-revalidate` of RFC 5861 in seconds, or `null`.
     *
     * @since 1.0.0
     */
    public val staleWhileRevalidate: Long? get() = seconds(STALE_WHILE_REVALIDATE)

    /**
     * `stale-if-error` of RFC 5861 in seconds, or `null`.
     *
     * @since 1.0.0
     */
    public val staleIfError: Long? get() = seconds(STALE_IF_ERROR)

    /**
     * Whether `no-cache` is present.
     *
     * @since 1.0.0
     */
    public val isNoCache: Boolean get() = NO_CACHE in this

    /**
     * The field names `no-cache` limits itself to, in lower case; empty when it covers the whole response.
     *
     * @since 1.0.0
     */
    public val noCacheFields: List<String> get() = fields(NO_CACHE)

    /**
     * Whether `no-store` is present.
     *
     * @since 1.0.0
     */
    public val isNoStore: Boolean get() = NO_STORE in this

    /**
     * Whether `no-transform` is present.
     *
     * @since 1.0.0
     */
    public val isNoTransform: Boolean get() = NO_TRANSFORM in this

    /**
     * Whether `only-if-cached` is present.
     *
     * @since 1.0.0
     */
    public val isOnlyIfCached: Boolean get() = ONLY_IF_CACHED in this

    /**
     * Whether `must-revalidate` is present.
     *
     * @since 1.0.0
     */
    public val isMustRevalidate: Boolean get() = MUST_REVALIDATE in this

    /**
     * Whether `must-understand` is present.
     *
     * @since 1.0.0
     */
    public val isMustUnderstand: Boolean get() = MUST_UNDERSTAND in this

    /**
     * Whether `private` is present.
     *
     * @since 1.0.0
     */
    public val isPrivate: Boolean get() = PRIVATE in this

    /**
     * The field names `private` limits itself to, in lower case; empty when it covers the whole response.
     *
     * @since 1.0.0
     */
    public val privateFields: List<String> get() = fields(PRIVATE)

    /**
     * Whether `proxy-revalidate` is present.
     *
     * @since 1.0.0
     */
    public val isProxyRevalidate: Boolean get() = PROXY_REVALIDATE in this

    /**
     * Whether `public` is present.
     *
     * @since 1.0.0
     */
    public val isPublic: Boolean get() = PUBLIC in this

    /**
     * Whether `immutable` of RFC 8246 is present.
     *
     * @since 1.0.0
     */
    public val isImmutable: Boolean get() = IMMUTABLE in this

    /**
     * Returns the first directive named [name].
     *
     * @param name the name, compared without case.
     * @return the directive, or `null` if there is none.
     * @since 1.0.0
     */
    public operator fun get(name: String): CacheDirective? {
        val key = name.lowercase()
        return directives.firstOrNull { directive -> directive.name == key }
    }

    /**
     * Returns whether a directive named [name] is present.
     *
     * @param name the name, compared without case.
     * @return `true` if it is present.
     * @since 1.0.0
     */
    public operator fun contains(name: String): Boolean = get(name) != null

    override fun equals(other: Any?): Boolean = other is CacheControl && directives == other.directives

    override fun hashCode(): Int = directives.hashCode()

    override fun toString(): String = directives.joinToString(", ")

    private fun seconds(name: String): Long? {
        val argument = get(name)?.argument ?: return null
        val isDelta = argument.isNotEmpty() && argument.all(FieldGrammar::isDigit)
        val significant = argument.trimStart('0').takeIf { isDelta }
        return significant?.let { digits -> minOf(digits.take(MOST_DIGITS + 1).ifEmpty { "0" }.toLong(), MOST_DELTA) }
    }

    private fun fields(name: String): List<String> {
        val argument = get(name)?.argument ?: return emptyList()
        val names = argument.split(',').map(FieldGrammar::trimmed)
        return names.filter(String::isNotEmpty).map(String::lowercase)
    }

    /**
     * The names of the directives and the reading of the field.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The name `max-age`.
         *
         * @since 1.0.0
         */
        public const val MAX_AGE: String = "max-age"

        /**
         * The name `s-maxage`.
         *
         * @since 1.0.0
         */
        public const val SHARED_MAX_AGE: String = "s-maxage"

        /**
         * The name `max-stale`.
         *
         * @since 1.0.0
         */
        public const val MAX_STALE: String = "max-stale"

        /**
         * The name `min-fresh`.
         *
         * @since 1.0.0
         */
        public const val MIN_FRESH: String = "min-fresh"

        /**
         * The name `stale-while-revalidate` of RFC 5861.
         *
         * @since 1.0.0
         */
        public const val STALE_WHILE_REVALIDATE: String = "stale-while-revalidate"

        /**
         * The name `stale-if-error` of RFC 5861.
         *
         * @since 1.0.0
         */
        public const val STALE_IF_ERROR: String = "stale-if-error"

        /**
         * The name `no-cache`.
         *
         * @since 1.0.0
         */
        public const val NO_CACHE: String = "no-cache"

        /**
         * The name `no-store`.
         *
         * @since 1.0.0
         */
        public const val NO_STORE: String = "no-store"

        /**
         * The name `no-transform`.
         *
         * @since 1.0.0
         */
        public const val NO_TRANSFORM: String = "no-transform"

        /**
         * The name `only-if-cached`.
         *
         * @since 1.0.0
         */
        public const val ONLY_IF_CACHED: String = "only-if-cached"

        /**
         * The name `must-revalidate`.
         *
         * @since 1.0.0
         */
        public const val MUST_REVALIDATE: String = "must-revalidate"

        /**
         * The name `must-understand`.
         *
         * @since 1.0.0
         */
        public const val MUST_UNDERSTAND: String = "must-understand"

        /**
         * The name `private`.
         *
         * @since 1.0.0
         */
        public const val PRIVATE: String = "private"

        /**
         * The name `proxy-revalidate`.
         *
         * @since 1.0.0
         */
        public const val PROXY_REVALIDATE: String = "proxy-revalidate"

        /**
         * The name `public`.
         *
         * @since 1.0.0
         */
        public const val PUBLIC: String = "public"

        /**
         * The name `immutable` of RFC 8246.
         *
         * @since 1.0.0
         */
        public const val IMMUTABLE: String = "immutable"

        /**
         * Makes a cache-control of [directives].
         *
         * @param directives the directives in order.
         * @return the cache-control.
         * @since 1.0.0
         */
        public fun of(vararg directives: CacheDirective): CacheControl = CacheControl(directives.toList())

        /**
         * Reads the value of `Cache-Control`.
         *
         * @param text the field value, the lines of a repeated field joined with commas.
         * @param limits the limits: the length of the value and the number of directives.
         * @return the cache-control, or `null` if a directive is malformed, an argument is no token or quoted string
         * of visible characters, or the value exceeds a limit.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String, limits: FieldLimits = FieldLimits.Default): CacheControl? {
            val directives = FieldLists.read(text, limits.maxFieldLength, limits.maxMembers, ::directive)
            return directives?.let(::CacheControl)
        }

        private fun directive(scanner: FieldScanner): CacheDirective? {
            val name = scanner.token()
            val hasArgument = name != null && scanner.consume('=')
            val argument = argumentOf(scanner, hasArgument)
            val isArgumentValid = !hasArgument || (argument != null && FieldGrammar.isSafeValue(argument))
            if (name == null || !isArgumentValid) return null
            return CacheDirective(name, argument)
        }

        private fun argumentOf(scanner: FieldScanner, hasArgument: Boolean): String? {
            if (!hasArgument) return null
            return scanner.tokenOrQuoted()
        }
    }
}
