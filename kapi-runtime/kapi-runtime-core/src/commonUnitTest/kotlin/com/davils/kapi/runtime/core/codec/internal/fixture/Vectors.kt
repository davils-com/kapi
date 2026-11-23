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

package com.davils.kapi.runtime.core.codec.internal.fixture

internal const val ENDS_EARLY: String = "ends early"

internal const val NOT_FINITE: String = "not finite"

private const val OBJECT: String = """{"a":1,"b":[2,3]}"""

internal val readVectors: Map<String, String> = mapOf(
    "00" to "0",
    "17" to "23",
    "1818" to "24",
    "1903e8" to "1000",
    "1a000f4240" to "1000000",
    "1b000000e8d4a51000" to "1000000000000",
    "20" to "-1",
    "3903e7" to "-1000",
    "3b7fffffffffffffff" to "-9223372036854775808",
    "f4" to "false",
    "f5" to "true",
    "f6" to "null",
    "f7" to "null",
    "60" to "\"\"",
    "6449455446" to "\"IETF\"",
    "62c3bc" to "\"ü\"",
    "7f657374726561646d696e67ff" to "\"streaming\"",
    "4401020304" to "\"AQIDBA==\"",
    "5f42010243030405ff" to "\"AQIDBAU=\"",
    "80" to "[]",
    "83010203" to "[1,2,3]",
    "9f018202039f0405ffff" to "[1,[2,3],[4,5]]",
    "a0" to "{}",
    "a26161016162820203" to OBJECT,
    "bf61610161629f0203ffff" to OBJECT,
    "c074323031332d30332d32315432303a30343a30305a" to "\"2013-03-21T20:04:00Z\"",
    "d81801" to "1",
)

internal val floatVectors: Map<String, Double> = mapOf(
    "fb3ff199999999999a" to 1.1,
    "f93c00" to 1.0,
    "f90001" to 5.960464477539063E-8,
    "f9c400" to -4.0,
    "f90000" to 0.0,
    "fa47c35000" to 100_000.0,
)

internal val refusedVectors: Map<String, String> = mapOf(
    "" to ENDS_EARLY,
    "18" to ENDS_EARLY,
    "1903" to ENDS_EARLY,
    "6261" to ENDS_EARLY,
    "5a7fffffff" to ENDS_EARLY,
    "5bffffffffffffffff" to ENDS_EARLY,
    "9a7fffffff" to ENDS_EARLY,
    "ba7fffffff" to ENDS_EARLY,
    "9f01" to ENDS_EARLY,
    "0000" to "followed by more bytes",
    "1bffffffffffffffff" to "64 signed bits",
    "3bffffffffffffffff" to "64 signed bits",
    "1c" to "argument 28 is malformed",
    "1f" to "argument 31 is malformed",
    "f0" to "simple value 16",
    "f801" to "simple value 24",
    "ff" to "simple value 31",
    "f97e00" to NOT_FINITE,
    "f97c00" to NOT_FINITE,
    "fa7f800000" to NOT_FINITE,
    "a10102" to "key is not a text string",
    "a2616101616102" to "repeats a key",
    "62c328" to "no UTF-8",
    "7f4101ff" to "chunk that is none",
    "5f5fffff" to "chunk that is none",
)

internal val writeVectors: Map<String, String> = mapOf(
    "0" to "00",
    "23" to "17",
    "24" to "1818",
    "100" to "1864",
    "1000" to "1903e8",
    "1000000" to "1a000f4240",
    "1000000000000" to "1b000000e8d4a51000",
    "9223372036854775807" to "1b7fffffffffffffff",
    "-1" to "20",
    "-100" to "3863",
    "-1000" to "3903e7",
    "-9223372036854775808" to "3b7fffffffffffffff",
    "1.1" to "fb3ff199999999999a",
    "-4.1" to "fbc010666666666666",
    "false" to "f4",
    "true" to "f5",
    "null" to "f6",
    "\"\"" to "60",
    "\"a\"" to "6161",
    "\"IETF\"" to "6449455446",
    "\"ü\"" to "62c3bc",
    "\"true\"" to "6474727565",
    "[]" to "80",
    "[1,2,3]" to "83010203",
    "{}" to "a0",
    OBJECT to "a26161016162820203",
)
