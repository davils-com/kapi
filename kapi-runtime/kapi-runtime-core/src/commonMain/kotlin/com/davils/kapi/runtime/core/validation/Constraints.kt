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

package com.davils.kapi.runtime.core.validation

import com.davils.kapi.runtime.core.internal.Utf8
import com.davils.kapi.runtime.core.pattern.Pattern

/**
 * The checks generated models run for the keywords of JSON Schema that Kotlin cannot express by itself.
 *
 * The functions keep no state and are safe to call from any thread.
 *
 * @since 1.0.0
 */
public object Constraints {
    /**
     * Returns the length of [text] in characters as JSON Schema counts them for `minLength` and `maxLength`: a pair of
     * surrogates is one character, a lone surrogate is one as well.
     *
     * @param text the text.
     * @return the number of code points.
     * @since 1.0.0
     */
    public fun length(text: String): Int = Utf8.codePointCount(text)

    /**
     * Returns whether [text] satisfies the keyword `pattern` of JSON Schema: whether [pattern] matches anywhere in it,
     * as [Pattern.containsMatchIn] decides, in time linear in the length of [text].
     *
     * Generated models compile each pattern once, with [Pattern.compileOrThrow], into a private value and report a
     * text that does not match as [ConstraintViolationCodes.PATTERN]. A text longer than
     * [com.davils.kapi.runtime.core.pattern.PatternLimits.maximumInputLength] of [pattern] is never read and does not
     * match, so an overlong value is refused instead of being matched in part.
     *
     * @param text the text to check, which may come from an attacker.
     * @param pattern the compiled pattern.
     * @return `true` if [text] is within the input limit of [pattern] and [pattern] matches some part of it.
     * @since 1.0.0
     */
    public fun matches(text: String, pattern: Pattern): Boolean {
        val isWithinLimit = text.length <= pattern.limits.maximumInputLength
        return isWithinLimit && pattern.containsMatchIn(text)
    }

    /**
     * Returns whether [value] is a whole multiple of [factor].
     *
     * @param value the number.
     * @param factor the factor.
     * @return `true` if [factor] is not zero and divides [value].
     * @since 1.0.0
     */
    public fun isMultipleOf(value: Long, factor: Long): Boolean = factor != 0L && value % factor == 0L

    /**
     * Returns whether [value] is a whole multiple of [factor], exactly, by the decimal text of both: `0.3` is a
     * multiple of `0.1`, although the binary doubles of both are not.
     *
     * @param value the number.
     * @param factor the factor.
     * @return `true` if both are finite, [factor] is not zero and divides [value] exactly.
     * @since 1.0.0
     */
    public fun isMultipleOf(value: Double, factor: Double): Boolean {
        val dividend = Decimal.ofOrNull(value)
        val divisor = Decimal.ofOrNull(factor)
        return dividend != null && divisor != null && dividend.isMultipleOf(divisor)
    }

    /**
     * Returns whether [value] is a whole multiple of [factor], exactly.
     *
     * @param value the number.
     * @param factor the factor.
     * @return `true` if [factor] is not zero and divides [value] exactly.
     * @since 1.0.0
     */
    public fun isMultipleOf(value: Decimal, factor: Decimal): Boolean = value.isMultipleOf(factor)

    /**
     * Returns whether every item of [items] appears once.
     *
     * Items that are all of one comparable class, such as strings, numbers and enums, are sorted and compared by
     * `compareTo`, so text chosen to collide in `hashCode` cannot slow the check, and numbers such as `1.0` and `1.00`
     * count as one value like in JSON Schema. Other items, such as models, are compared by `equals` and `hashCode`.
     *
     * @param items the items.
     * @return `true` if no two items are equal.
     * @since 1.0.0
     */
    public fun isUnique(items: Collection<*>): Boolean {
        val first = items.firstOrNull()
        val isOrdered = first != null && items.all { item -> item is Comparable<*> && item::class == first::class }
        if (!isOrdered) return items.toSet().size == items.size
        val sorted = items.sortedWith(::compared)
        return sorted.zipWithNext().none { (left, right) -> compared(left, right) == 0 }
    }

    private fun compared(left: Any?, right: Any?): Int =
        compareValues(a = left as? Comparable<*>, b = right as? Comparable<*>)
}
