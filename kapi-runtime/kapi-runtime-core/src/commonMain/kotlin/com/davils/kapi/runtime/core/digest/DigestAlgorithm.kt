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

package com.davils.kapi.runtime.core.digest

/**
 * The algorithms of the Hash Algorithms for HTTP Digest Fields registry of RFC 9530 §5.
 *
 * Only [Sha512] and [Sha256] are active; every other algorithm is deprecated, because it is insecure or no
 * cryptographic hash, and must not be relied on for integrity.
 *
 * @property key the key of the algorithm in a digest field, such as `sha-256`.
 * @property isActive whether the registry marks the algorithm as active.
 * @since 1.0.0
 */
public enum class DigestAlgorithm(public val key: String, public val isActive: Boolean) {
    /**
     * SHA-512, active.
     *
     * @since 1.0.0
     */
    Sha512("sha-512", isActive = true),

    /**
     * SHA-256, active.
     *
     * @since 1.0.0
     */
    Sha256("sha-256", isActive = true),

    /**
     * MD5, deprecated as insecure.
     *
     * @since 1.0.0
     */
    Md5("md5", isActive = false),

    /**
     * SHA-1, deprecated as insecure.
     *
     * @since 1.0.0
     */
    Sha("sha", isActive = false),

    /**
     * The UNIX sum, deprecated as no cryptographic hash.
     *
     * @since 1.0.0
     */
    UnixSum("unixsum", isActive = false),

    /**
     * The UNIX cksum, deprecated as no cryptographic hash.
     *
     * @since 1.0.0
     */
    UnixCksum("unixcksum", isActive = false),

    /**
     * Adler-32, deprecated as no cryptographic hash.
     *
     * @since 1.0.0
     */
    Adler("adler", isActive = false),

    /**
     * CRC32c, deprecated as no cryptographic hash.
     *
     * @since 1.0.0
     */
    Crc32c("crc32c", isActive = false),
    ;

    /**
     * Finds algorithms by key.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Returns the algorithm of [key].
         *
         * @param key the key, compared without case.
         * @return the algorithm, or `null` if the registry has none of this key.
         * @since 1.0.0
         */
        public fun of(key: String): DigestAlgorithm? {
            val lowered = key.lowercase()
            return entries.firstOrNull { algorithm -> algorithm.key == lowered }
        }
    }
}
