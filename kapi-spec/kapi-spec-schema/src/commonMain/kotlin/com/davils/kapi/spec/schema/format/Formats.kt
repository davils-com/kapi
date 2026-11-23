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

package com.davils.kapi.spec.schema.format

/**
 * The assertions of the `format` keyword of JSON Schema 2020-12 and of the formats OpenAPI adds.
 *
 * [check] knows these formats, each read by a hand-written scanner without regular expressions:
 *
 * - `date-time`, `date` and `time` of RFC 3339, with `T`, `Z` and their lower case, an offset required on every time,
 *   the days of every month and leap year, and a leap second only at 23:59:60 in UTC.
 * - `duration` of appendix A of RFC 3339, such as `P1Y2M3DT4H5M6S` or `P2W`, with upper case designators, the date and
 *   time elements in order and weeks never combined with other elements.
 * - `email` of RFC 5321, a dot-atom or a quoted string before the `@` and a hostname or an IPv4 or `IPv6:` address
 *   literal after it, and `idn-email` of RFC 6531, which allows non-ASCII text in the local part and an
 *   `idn-hostname` as the domain. General address literals and comments are refused.
 * - `hostname` of RFC 1123, whose labels starting with `xn--` must be the canonical Punycode of a valid U-label, and
 *   `idn-hostname` of RFC 5890 to 5892, which also separates labels by U+3002, U+FF0E and U+FF61. The code points of
 *   a U-label are derived from the general categories of Unicode 16.0 with the exceptions and contextual rules of
 *   RFC 5892; compatibility characters outside the blocks of compatibility forms, the joining types of scripts other
 *   than Arabic and the Bidi rule of RFC 5893 are not checked. ASCII letters of either case are accepted. A final dot
 *   is refused.
 * - `ipv4` in dotted decimal without leading zeros and `ipv6` of RFC 4291 with an embedded IPv4 address but without a
 *   zone.
 * - `uri` and `uri-reference` of RFC 3986, and `iri` and `iri-reference` of RFC 3987, which allow the characters of
 *   `ucschar` and, in the query, of `iprivate`.
 * - `uuid` in the textual form of RFC 9562, of any version and case.
 * - `uri-template` of RFC 6570, `json-pointer` of RFC 6901 and `relative-json-pointer`.
 * - `regex`: valid syntax of ECMA-262 in Unicode mode, as [com.davils.kapi.spec.schema.pattern.Pattern] reads it. A
 *   regular expression that is valid but cannot run in the linear engine, such as one with a back reference or a
 *   lookaround, is [FormatCheck.Valid] here; compile it with `Pattern.compile` to tell these apart.
 * - The formats of OpenAPI: `int32` and `int64`, a JSON number with an integral value in the range of the type, `float`
 *   and `double`, a JSON number in the range of the type, `byte`, padded base64 of RFC 4648, and `binary` and
 *   `password`, which every value meets.
 *
 * Every value is checked in time linear in its length. A value longer than [MAXIMUM_VALUE_LENGTH] UTF-16 code units
 * is [FormatCheck.Invalid] without being read, except for `byte`, `binary` and `password`, which are checked at any
 * length; a hostname is refused beyond 506 code units and an e-mail address beyond 254 octets before their labels are
 * read. The object keeps no state and is safe to use from any thread.
 *
 * @since 1.0.0
 */
public object Formats {
    /**
     * The most UTF-16 code units of a value whose format is checked: 65 536.
     *
     * @since 1.0.0
     */
    public const val MAXIMUM_VALUE_LENGTH: Int = 65_536

    private val checkers: Map<String, (String) -> String?> = mapOf(
        "date-time" to DateTimeFormats::dateTime,
        "date" to DateTimeFormats::date,
        "time" to DateTimeFormats::time,
        "duration" to DateTimeFormats::duration,
        "email" to EmailFormats::email,
        "idn-email" to EmailFormats::idnEmail,
        "hostname" to HostnameFormats::hostname,
        "idn-hostname" to HostnameFormats::idnHostname,
        "ipv4" to AddressFormats::ipv4,
        "ipv6" to AddressFormats::ipv6,
        "uri" to UriFormats::uri,
        "uri-reference" to UriFormats::uriReference,
        "iri" to UriFormats::iri,
        "iri-reference" to UriFormats::iriReference,
        "uuid" to TextFormats::uuid,
        "uri-template" to UriFormats::uriTemplate,
        "json-pointer" to PointerFormats::jsonPointer,
        "relative-json-pointer" to PointerFormats::relativeJsonPointer,
        "regex" to TextFormats::regex,
        "int32" to NumberFormats::int32,
        "int64" to NumberFormats::int64,
        "float" to NumberFormats::float,
        "double" to NumberFormats::double,
        "byte" to TextFormats::base64,
        "binary" to { _ -> null },
        "password" to { _ -> null },
    )

    private val unbounded: Set<String> = setOf("byte", "binary", "password")

    /**
     * The names of every format [check] knows, such as `date-time` and `int32`.
     *
     * @since 1.0.0
     */
    public val names: Set<String> = checkers.keys

    /**
     * Checks [value] against [format].
     *
     * Format names are compared exactly, so `Date` is not `date`. For `int32`, `int64`, `float` and `double` the value
     * is the text of a JSON number, such as `42` or `1.5e3`.
     *
     * @param format the name of the format, such as `email`; it may come from an attacker.
     * @param value the value to check, which may come from an attacker.
     * @return [FormatCheck.Valid] or [FormatCheck.Invalid] with a reason that never repeats [value], or
     * [FormatCheck.Unknown] if [format] is none of [names].
     * @since 1.0.0
     */
    public fun check(format: String, value: String): FormatCheck {
        val checker = checkers[format] ?: return FormatCheck.Unknown
        val isBounded = value.length <= MAXIMUM_VALUE_LENGTH || format in unbounded
        val reason = "the value is longer than $MAXIMUM_VALUE_LENGTH code units".takeUnless { isBounded }
        return (reason ?: checker(value))?.let(FormatCheck::Invalid) ?: FormatCheck.Valid
    }
}
