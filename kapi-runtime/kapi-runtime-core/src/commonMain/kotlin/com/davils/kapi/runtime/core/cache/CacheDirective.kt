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

package com.davils.kapi.runtime.core.cache

import com.davils.kapi.runtime.core.internal.FieldGrammar
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

private const val HASH_FACTOR: Int = 31

/**
 * One directive of `Cache-Control` of RFC 9111 §5.2, such as `max-age=60` or `no-cache="Set-Cookie"`.
 *
 * The name is a token kept in lower case; the argument is written as a token when it is one and as a quoted string
 * otherwise. A directive refuses an argument a field cannot carry, so [toString] is always a valid member of the field.
 * The companion makes the directives of RFC 9111, RFC 5861 and RFC 8246. A directive is immutable.
 *
 * @param name the name of the directive, a token compared without case.
 * @property argument the argument, or `null` if the directive has none.
 * @throws IllegalArgumentException if [name] is no token or [argument] holds a character other than visible ASCII,
 * spaces and tabs, such as a line break.
 * @since 1.0.0
 */
public class CacheDirective(name: String, public val argument: String? = null) {
    /**
     * The name of the directive in lower case.
     *
     * @since 1.0.0
     */
    public val name: String = name.lowercase()

    init {
        require(FieldGrammar.isToken(name)) { "${SafeText.quoted(name)} is no name of a directive" }
        require(argument == null || FieldGrammar.isSafeValue(argument)) { "The argument holds a control character" }
    }

    override fun equals(other: Any?): Boolean =
        other is CacheDirective && name == other.name && argument == other.argument

    override fun hashCode(): Int = name.hashCode() * HASH_FACTOR + argument.hashCode()

    override fun toString(): String {
        val written = argument?.let { value -> "=" + FieldGrammar.tokenOrQuoted(value) }.orEmpty()
        return name + written
    }

    /**
     * The directives of RFC 9111, RFC 5861 and RFC 8246.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * `max-age`: the response stays fresh for [seconds]; in a request, the client accepts no older response.
         *
         * @param seconds the age in seconds, zero or more.
         * @return the directive.
         * @throws IllegalArgumentException if [seconds] is negative.
         * @since 1.0.0
         */
        public fun maxAge(seconds: Long): CacheDirective = seconds(CacheControl.MAX_AGE, seconds)

        /**
         * `s-maxage`: the response stays fresh for [seconds] in a shared cache.
         *
         * @param seconds the age in seconds, zero or more.
         * @return the directive.
         * @throws IllegalArgumentException if [seconds] is negative.
         * @since 1.0.0
         */
        public fun sharedMaxAge(seconds: Long): CacheDirective = seconds(CacheControl.SHARED_MAX_AGE, seconds)

        /**
         * `max-stale`: the client accepts a response that is stale by at most [seconds], or by any time for `null`.
         *
         * @param seconds the staleness in seconds, or `null` for any.
         * @return the directive.
         * @throws IllegalArgumentException if [seconds] is negative.
         * @since 1.0.0
         */
        public fun maxStale(seconds: Long? = null): CacheDirective {
            if (seconds == null) return CacheDirective(CacheControl.MAX_STALE)
            return seconds(CacheControl.MAX_STALE, seconds)
        }

        /**
         * `min-fresh`: the client wants a response that stays fresh for at least [seconds].
         *
         * @param seconds the freshness in seconds, zero or more.
         * @return the directive.
         * @throws IllegalArgumentException if [seconds] is negative.
         * @since 1.0.0
         */
        public fun minFresh(seconds: Long): CacheDirective = seconds(CacheControl.MIN_FRESH, seconds)

        /**
         * `stale-while-revalidate` of RFC 5861.
         *
         * @param seconds the time in seconds, zero or more.
         * @return the directive.
         * @throws IllegalArgumentException if [seconds] is negative.
         * @since 1.0.0
         */
        public fun staleWhileRevalidate(seconds: Long): CacheDirective =
            seconds(CacheControl.STALE_WHILE_REVALIDATE, seconds)

        /**
         * `stale-if-error` of RFC 5861.
         *
         * @param seconds the time in seconds, zero or more.
         * @return the directive.
         * @throws IllegalArgumentException if [seconds] is negative.
         * @since 1.0.0
         */
        public fun staleIfError(seconds: Long): CacheDirective = seconds(CacheControl.STALE_IF_ERROR, seconds)

        /**
         * `no-cache`, optionally limited to [fields] of a response.
         *
         * @param fields the names of the fields a cache must not reuse; empty for the whole response.
         * @return the directive.
         * @throws IllegalArgumentException if a field name is no token.
         * @since 1.0.0
         */
        public fun noCache(fields: List<String> = emptyList()): CacheDirective = listed(CacheControl.NO_CACHE, fields)

        /**
         * `private`, optionally limited to [fields] of a response.
         *
         * @param fields the names of the fields a shared cache must not store; empty for the whole response.
         * @return the directive.
         * @throws IllegalArgumentException if a field name is no token.
         * @since 1.0.0
         */
        public fun private(fields: List<String> = emptyList()): CacheDirective = listed(CacheControl.PRIVATE, fields)

        /**
         * A directive without an argument, such as `no-store`, `public`, `immutable` or `must-revalidate`.
         *
         * @param name the name of the directive.
         * @return the directive.
         * @throws IllegalArgumentException if [name] is no token.
         * @since 1.0.0
         */
        public fun flag(name: String): CacheDirective = CacheDirective(name)

        private fun seconds(name: String, seconds: Long): CacheDirective {
            require(seconds >= 0) { "A delta of seconds is not negative" }
            return CacheDirective(name, seconds.toString())
        }

        private fun listed(name: String, fields: List<String>): CacheDirective {
            require(fields.all(FieldGrammar::isToken)) { "A field name is a token" }
            val argument = fields.joinToString(", ").takeIf { fields.isNotEmpty() }
            return CacheDirective(name, argument)
        }
    }
}
