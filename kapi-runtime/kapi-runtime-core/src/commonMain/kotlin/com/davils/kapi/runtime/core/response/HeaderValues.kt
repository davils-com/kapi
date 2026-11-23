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

package com.davils.kapi.runtime.core.response

/**
 * Builds the header fields of a response or a request.
 *
 * @param entries the fields with their values; names that differ only in case are merged.
 * @param sensitive the names of further fields to mask besides [Headers.SENSITIVE], compared without case.
 * @return the headers.
 * @since 1.0.0
 */
public fun headersOf(entries: Map<String, List<String>>, sensitive: Set<String> = emptySet()): Headers =
    Headers(entries, sensitive)

/**
 * Returns the first value of the field [name] as a whole number, whatever blanks surround it.
 *
 * @param name the name of the field.
 * @return the number, or `null` if the field is absent or no number of this kind.
 * @since 1.0.0
 */
public fun Headers.int(name: String): Int? = trimmed(name)?.toIntOrNull()

/**
 * Returns the first value of the field [name] as a long whole number, whatever blanks surround it.
 *
 * @param name the name of the field.
 * @return the number, or `null` if the field is absent or no number of this kind.
 * @since 1.0.0
 */
public fun Headers.long(name: String): Long? = trimmed(name)?.toLongOrNull()

/**
 * Returns the first value of the field [name] as a double, whatever blanks surround it.
 *
 * @param name the name of the field.
 * @return the number, or `null` if the field is absent or no number.
 * @since 1.0.0
 */
public fun Headers.double(name: String): Double? = trimmed(name)?.toDoubleOrNull()

/**
 * Returns the first value of the field [name] as a float, whatever blanks surround it.
 *
 * @param name the name of the field.
 * @return the number, or `null` if the field is absent or no number.
 * @since 1.0.0
 */
public fun Headers.float(name: String): Float? = trimmed(name)?.toFloatOrNull()

/**
 * Returns the first value of the field [name] as a flag: exactly `true` or `false`.
 *
 * @param name the name of the field.
 * @return the flag, or `null` if the field is absent or neither `true` nor `false`.
 * @since 1.0.0
 */
public fun Headers.flag(name: String): Boolean? = trimmed(name)?.toBooleanStrictOrNull()

private fun Headers.trimmed(name: String): String? = get(name)?.trim()
