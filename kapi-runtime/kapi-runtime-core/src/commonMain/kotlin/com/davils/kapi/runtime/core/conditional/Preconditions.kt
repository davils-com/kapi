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

package com.davils.kapi.runtime.core.conditional

import com.davils.kapi.runtime.core.FieldLimits
import com.davils.kapi.runtime.core.internal.FieldGrammar

private const val GET: String = "GET"

private const val HEAD: String = "HEAD"

private const val NONE: String = "none"

private val nothing: EntityTagCondition = EntityTagCondition.Tags(emptyList())

/**
 * The preconditions of a request of RFC 9110 §13.1 and their evaluation in the order of §13.2.2.
 *
 * [evaluate] decides, for a server that would otherwise answer with a 2xx status: `If-Match` first, else
 * `If-Unmodified-Since`, then `If-None-Match`, else `If-Modified-Since` for `GET` and `HEAD`. `If-Match` compares
 * strongly, `If-None-Match` weakly. A date is ignored when the representation has no last modification. `If-Range`
 * belongs to the range of the request and is decided there. Preconditions are immutable and safe to share between
 * threads.
 *
 * @property ifMatch the condition of `If-Match`, or `null` if the request has none.
 * @property ifNoneMatch the condition of `If-None-Match`, or `null` if the request has none.
 * @property ifModifiedSince the date of `If-Modified-Since`, or `null` if the request has none or it is invalid.
 * @property ifUnmodifiedSince the date of `If-Unmodified-Since`, or `null` if the request has none or it is invalid.
 * @since 1.0.0
 */
public class Preconditions(
    public val ifMatch: EntityTagCondition? = null,
    public val ifNoneMatch: EntityTagCondition? = null,
    public val ifModifiedSince: HttpDate? = null,
    public val ifUnmodifiedSince: HttpDate? = null,
) {
    /**
     * Decides the preconditions for [method] against [state].
     *
     * @param method the method of the request, such as `GET` or `PUT`, in upper case as RFC 9110 spells it.
     * @param state the current representation.
     * @return [PreconditionDecision.PreconditionFailed] if `If-Match` or `If-Unmodified-Since` fails, or
     * `If-None-Match` matches a method other than `GET` and `HEAD`; [PreconditionDecision.NotModified] if
     * `If-None-Match` matches or `If-Modified-Since` finds no change for `GET` and `HEAD`; else
     * [PreconditionDecision.Proceed].
     * @since 1.0.0
     */
    public fun evaluate(method: String, state: RepresentationState): PreconditionDecision {
        val isRead = method == GET || method == HEAD
        val isRefused = isMatchFailed(state) || isUnmodifiedFailed(state)
        if (isRefused) return PreconditionDecision.PreconditionFailed
        val readDecision = PreconditionDecision.NotModified.takeIf { isRead } ?: PreconditionDecision.PreconditionFailed
        val isNoneMatchFailed = ifNoneMatch?.let { condition -> isMatched(condition, state, isStrong = false) } == true
        val isNotModified = ifNoneMatch == null && isRead && isModifiedFailed(state)
        return readDecision.takeIf { isNoneMatchFailed } ?: PreconditionDecision.NotModified.takeIf { isNotModified }
            ?: PreconditionDecision.Proceed
    }

    override fun equals(other: Any?): Boolean = other is Preconditions &&
        ifMatch == other.ifMatch &&
        ifNoneMatch == other.ifNoneMatch &&
        ifModifiedSince == other.ifModifiedSince &&
        ifUnmodifiedSince == other.ifUnmodifiedSince

    override fun hashCode(): Int = listOf(ifMatch, ifNoneMatch, ifModifiedSince, ifUnmodifiedSince).hashCode()

    override fun toString(): String {
        val match = ifMatch ?: NONE
        val noneMatch = ifNoneMatch ?: NONE
        val modified = ifModifiedSince ?: NONE
        val unmodified = ifUnmodifiedSince ?: NONE
        return "Preconditions(ifMatch=$match, ifNoneMatch=$noneMatch, ifModifiedSince=$modified, " +
            "ifUnmodifiedSince=$unmodified)"
    }

    private fun isMatchFailed(state: RepresentationState): Boolean {
        val condition = ifMatch ?: return false
        return !isMatched(condition, state, isStrong = true)
    }

    private fun isUnmodifiedFailed(state: RepresentationState): Boolean {
        val date = ifUnmodifiedSince?.takeIf { ifMatch == null }
        val lastModified = state.lastModified
        return date != null && lastModified != null && lastModified > date
    }

    private fun isModifiedFailed(state: RepresentationState): Boolean {
        val date = ifModifiedSince
        val lastModified = state.lastModified
        return date != null && lastModified != null && lastModified <= date
    }

    private fun isMatched(condition: EntityTagCondition, state: RepresentationState, isStrong: Boolean): Boolean =
        when (condition) {
            EntityTagCondition.Any -> state.isPresent

            is EntityTagCondition.Tags -> condition.entityTags.any { tag ->
                isTagMatched(
                    tag,
                    state.entityTag,
                    isStrong,
                )
            }
        }

    private fun isTagMatched(tag: EntityTag, current: EntityTag?, isStrong: Boolean): Boolean {
        if (current == null) return false
        return tag.matchesStrongly(current).takeIf { isStrong } ?: tag.matchesWeakly(current)
    }

    /**
     * Reads preconditions.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Reads the preconditions from the values of their fields.
         *
         * A malformed `If-Match` or `If-None-Match` is read as a list that matches nothing, so `If-Match` fails and
         * `If-None-Match` passes; a date that is no HTTP-date is ignored, as RFC 9110 §13.1.3 and §13.1.4 demand.
         *
         * @param ifMatch the value of `If-Match`, or `null`.
         * @param ifNoneMatch the value of `If-None-Match`, or `null`.
         * @param ifModifiedSince the value of `If-Modified-Since`, or `null`.
         * @param ifUnmodifiedSince the value of `If-Unmodified-Since`, or `null`.
         * @param now the present, to read an HTTP-date with a two-digit year.
         * @param limits the limits of the lists of entity tags.
         * @return the preconditions.
         * @since 1.0.0
         */
        public fun read(
            ifMatch: String? = null,
            ifNoneMatch: String? = null,
            ifModifiedSince: String? = null,
            ifUnmodifiedSince: String? = null,
            now: HttpDate? = null,
            limits: FieldLimits = FieldLimits.Default,
        ): Preconditions = Preconditions(
            ifMatch = ifMatch?.let { text -> EntityTagCondition.parseOrNull(text, limits) ?: nothing },
            ifNoneMatch = ifNoneMatch?.let { text -> EntityTagCondition.parseOrNull(text, limits) ?: nothing },
            ifModifiedSince = ifModifiedSince?.let { text -> HttpDate.parseOrNull(FieldGrammar.trimmed(text), now) },
            ifUnmodifiedSince = ifUnmodifiedSince?.let { text ->
                HttpDate.parseOrNull(
                    FieldGrammar.trimmed(text),
                    now,
                )
            },
        )
    }
}
