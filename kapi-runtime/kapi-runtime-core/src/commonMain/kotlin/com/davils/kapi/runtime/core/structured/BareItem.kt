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

package com.davils.kapi.runtime.core.structured

import com.davils.kapi.runtime.core.structured.internal.StructuredGrammar
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlinx.io.bytestring.ByteString

/**
 * A bare item of RFC 9651 §3.3: the value of an [Item] or of a parameter, without parameters of its own.
 *
 * Every kind checks its value when it is made, so a bare item can always be serialized: a value outside the range
 * RFC 9651 allows is refused with an [IllegalArgumentException]. Bare items are immutable; [toString] writes the
 * serialized form, which never carries a line break.
 *
 * @since 1.0.0
 */
public sealed interface BareItem {
    /**
     * An Integer of §3.3.1, from -999,999,999,999,999 to 999,999,999,999,999.
     *
     * @property value the number.
     * @throws IllegalArgumentException if [value] has more than 15 digits.
     * @since 1.0.0
     */
    public class Integer(public val value: Long) : BareItem {
        init {
            require(value in StructuredGrammar.INTEGER_RANGE) { "An integer has at most 15 digits" }
        }

        override fun equals(other: Any?): Boolean = other is Integer && value == other.value

        override fun hashCode(): Int = value.hashCode()

        override fun toString(): String = value.toString()
    }

    /**
     * A Decimal of §3.3.2, kept exactly in thousandths: at most 12 digits before the point and 3 after it.
     *
     * @property thousandths the number times 1000, such as `1500` for `1.5`.
     * @throws IllegalArgumentException if the number has more than 12 digits before the point.
     * @since 1.0.0
     */
    public class Decimal(public val thousandths: Long) : BareItem {
        init {
            require(thousandths in StructuredGrammar.INTEGER_RANGE) { "A decimal has at most 12 integer digits" }
        }

        override fun equals(other: Any?): Boolean = other is Decimal && thousandths == other.thousandths

        override fun hashCode(): Int = thousandths.hashCode()

        override fun toString(): String = StructuredGrammar.decimalText(thousandths)
    }

    /**
     * A String of §3.3.3: printable ASCII from space to tilde.
     *
     * @property value the text.
     * @throws IllegalArgumentException if [value] holds a character outside printable ASCII, such as a line break.
     * @since 1.0.0
     */
    public class Text(public val value: String) : BareItem {
        init {
            require(value.all(StructuredGrammar::isPrintable)) {
                "${SafeText.quoted(value)} is no string of printable ASCII characters"
            }
        }

        override fun equals(other: Any?): Boolean = other is Text && value == other.value

        override fun hashCode(): Int = value.hashCode()

        override fun toString(): String = StructuredGrammar.quoted(value)
    }

    /**
     * A Token of §3.3.4, such as `gzip` or `text/html`.
     *
     * @property value the token.
     * @throws IllegalArgumentException if [value] is no token.
     * @since 1.0.0
     */
    public class Token(public val value: String) : BareItem {
        init {
            require(StructuredGrammar.isToken(value)) { "${SafeText.quoted(value)} is no token" }
        }

        override fun equals(other: Any?): Boolean = other is Token && value == other.value

        override fun hashCode(): Int = value.hashCode()

        override fun toString(): String = value
    }

    /**
     * A Byte Sequence of §3.3.5, written as Base64 between colons.
     *
     * @property value the bytes.
     * @since 1.0.0
     */
    public class Bytes(public val value: ByteString) : BareItem {
        override fun equals(other: Any?): Boolean = other is Bytes && value == other.value

        override fun hashCode(): Int = value.hashCode()

        override fun toString(): String = StructuredGrammar.bytesText(value)
    }

    /**
     * A Boolean of §3.3.6, written as `?1` or `?0`.
     *
     * @property isTrue the flag.
     * @since 1.0.0
     */
    public class Flag(public val isTrue: Boolean) : BareItem {
        override fun equals(other: Any?): Boolean = other is Flag && isTrue == other.isTrue

        override fun hashCode(): Int = isTrue.hashCode()

        override fun toString(): String = "?1".takeIf { isTrue } ?: "?0"
    }

    /**
     * A Date of §3.3.7: whole seconds since 1970-01-01T00:00:00Z, written as `@` and an integer.
     *
     * @property seconds the seconds since the epoch, with at most 15 digits.
     * @throws IllegalArgumentException if [seconds] has more than 15 digits.
     * @since 1.0.0
     */
    public class Date(public val seconds: Long) : BareItem {
        init {
            require(seconds in StructuredGrammar.INTEGER_RANGE) { "A date has at most 15 digits" }
        }

        override fun equals(other: Any?): Boolean = other is Date && seconds == other.seconds

        override fun hashCode(): Int = seconds.hashCode()

        override fun toString(): String = "@$seconds"
    }

    /**
     * A Display String of §3.3.8: Unicode text, written with every non-ASCII octet of its UTF-8 percent-encoded.
     *
     * @property value the text.
     * @throws IllegalArgumentException if [value] holds a lone surrogate, which has no UTF-8.
     * @since 1.0.0
     */
    public class DisplayText(public val value: String) : BareItem {
        init {
            require(StructuredGrammar.isWellFormed(value)) { "A display string holds no lone surrogate" }
        }

        override fun equals(other: Any?): Boolean = other is DisplayText && value == other.value

        override fun hashCode(): Int = value.hashCode()

        override fun toString(): String = StructuredGrammar.displayText(value)
    }
}
