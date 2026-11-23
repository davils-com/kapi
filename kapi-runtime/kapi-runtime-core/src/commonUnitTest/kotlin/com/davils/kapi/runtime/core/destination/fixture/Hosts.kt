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

package com.davils.kapi.runtime.core.destination.fixture

internal const val LOCALHOST: String = "localhost"

internal const val UNREADABLE: String = "exa mple.com"

internal const val PUBLIC_V6: String = "2606:4700::1111"

internal val privateHosts: List<String> = listOf(
    LOCALHOST,
    "db",
    "api.internal",
    "printer.local",
    "home.arpa",
    "127.0.0.1",
    PRIVATE_ADDRESS,
    "0.0.0.0",
    "100.64.0.1",
    "169.254.169.254",
    "172.31.255.255",
    "192.0.0.9",
    "192.0.2.1",
    "192.168.1.1",
    "198.18.0.1",
    "224.0.0.1",
    "255.255.255.255",
    "[::1]",
    "[::]",
    "fc00::1",
    "fe80::1",
    "ff02::1",
    "[::ffff:10.0.0.1]",
    "[::ffff:127.0.0.1]",
    "[::10.0.0.1]",
    "[2002:a00:1::1]",
    "[64:ff9b::a00:1]",
    "[64:ff9b:1::1]",
    "[2001::1]",
    "[2001:db8::1]",
    "[3fff::1]",
    "[100::1]",
)

internal val invalidHosts: List<String> = listOf(
    "",
    UNREADABLE,
    "ex%61mple.com",
    "bücher.de",
    "0x7f.1",
    "127.0.0.0x1",
    "127.1",
    "010.0.0.1",
    "1.2.3.256",
    "[::1",
    "[]",
    "[1.2.3.4]",
    "[fe80::1%25eth0]",
    "1::2::3",
    "1:2:3:4:5:6:7:8:9",
    "a..example.com",
    "-a.example.com",
    "a-.example.com",
    "example.-1",
    "example.com..",
    "a".repeat(64) + ".com",
)
