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

package com.davils.kapi.runtime.core.parameter

import com.davils.kapi.runtime.core.internal.PercentCoding
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.parameter.internal.UriEncoding

/**
 * Writes and reads a parameter `in: querystring` of OpenAPI 3.2 §4.12.2, which carries the whole query as one value
 * serialized by a media type.
 *
 * For `application/x-www-form-urlencoded` the serialized value already is a query and is used as it is; for every
 * other media type, such as `application/json`, it is percent-encoded as a whole. The functions keep no state and
 * are safe to call from any thread.
 *
 * @since 1.0.0
 */
public object QueryString {
    /**
     * Writes [serialized], the parameter written by its media type, as the query of a URL.
     *
     * @param serialized the value as its media type writes it.
     * @param mediaType the media type of the parameter.
     * @return the query without a leading `?`.
     * @since 1.0.0
     */
    public fun encode(serialized: String, mediaType: MediaType): String {
        if (isForm(mediaType)) return serialized
        return UriEncoding.encode(serialized)
    }

    /**
     * Reads the raw query of a URL as the serialized value of the parameter.
     *
     * @param query the query as it was sent, without its `?`, or `null` if the URL has none.
     * @param mediaType the media type of the parameter.
     * @return the serialized value as [ParameterValue.Text], [ParameterReading.Absent] for `null`, or
     * [ParameterReading.Malformed] if the query of another media type than a form is no percent-encoded UTF-8.
     * @since 1.0.0
     */
    public fun read(query: String?, mediaType: MediaType): ParameterReading {
        if (query == null) return ParameterReading.Absent
        val text = query.takeIf { isForm(mediaType) } ?: PercentCoding.decode(query)
        return text?.let { value -> ParameterReading.Present(ParameterValue.Text(value)) } ?: ParameterReading.Malformed
    }

    private fun isForm(mediaType: MediaType): Boolean = mediaType.essence == MediaType.FormUrlEncoded.essence
}
