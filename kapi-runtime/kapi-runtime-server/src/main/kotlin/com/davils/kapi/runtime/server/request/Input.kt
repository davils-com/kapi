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

package com.davils.kapi.runtime.server.request

import com.davils.kapi.runtime.core.codec.Decoding
import com.davils.kapi.runtime.core.codec.multipart.Boundary
import com.davils.kapi.runtime.core.codec.multipart.MultipartFormData
import com.davils.kapi.runtime.core.codec.multipart.MultipartPart
import com.davils.kapi.runtime.core.negotiation.AcceptFields
import com.davils.kapi.runtime.core.negotiation.MediaRange
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.negotiation.Negotiation
import com.davils.kapi.runtime.core.parameter.ParameterFormat
import com.davils.kapi.runtime.core.parameter.ParameterReading
import com.davils.kapi.runtime.core.parameter.ParameterShape
import com.davils.kapi.runtime.core.parameter.ParameterValue
import com.davils.kapi.runtime.core.parameter.QueryString
import com.davils.kapi.runtime.core.response.HttpStatus
import com.davils.kapi.runtime.server.internal.BodyRead
import com.davils.kapi.runtime.server.internal.BodyRefusal
import com.davils.kapi.runtime.server.internal.InputFindings
import com.davils.kapi.runtime.server.internal.MultipartRefusals
import com.davils.kapi.runtime.server.internal.ParameterReader
import com.davils.kapi.runtime.server.problem.ServerCodes
import com.davils.kapi.runtime.server.reply.Reply
import com.davils.kore.core.failure.contained
import com.davils.kore.core.outcome.Outcome
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val BODY: String = "body"

private const val QUERY: String = "query"

private const val ACCEPT: String = "accept"

private const val ACCEPT_ENCODING: String = "Accept-Encoding"

private const val SUPPORTED_CODINGS: String = "gzip, deflate"

private const val STRUCTURED_STARTS: String = "[{"

private fun pathOf(place: Place, format: ParameterFormat): String = "${place.label}.${format.name}"

private fun pathOf(name: String): String = "$BODY.$name"

/**
 * Reads a request as a whole: its parameters in every style of OpenAPI 3.2 §4.12, its body by its `Content-Type`
 * through the codecs of the server and the media type of the reply by `Accept`, and collects every problem into one
 * answer.
 *
 * Every reader returns `null` for what is missing or malformed and records why; a handler reads everything it needs,
 * then answers [refused] when [isValid] is false, so a client learns every problem of its request at once. A missing
 * or malformed parameter or body is a violation of a problem with status 400 (Bad Request); a body of a media type the
 * operation does not read is refused with 415 (Unsupported Media Type), a body larger than the limits of the server
 * with 413 (Content Too Large), and a reply that no media type the client accepts fits with 406 (Not Acceptable). A
 * refusal takes precedence over violations, which it carries as well. The paths of violations name the place and the
 * parameter, such as `query.limit`, or the body and the property, such as `body.name`.
 *
 * A body is read within the limits of the server and decoded by the codecs of the server, whose JSON and CBOR codecs
 * bound the depth of nesting; a decoded value that is [Validatable] is validated, and its violations are reported
 * under `body`. A parameter of a type other than a string is read as a JSON literal and never as a structure, so no
 * parameter makes the decoder nest. An input is not safe to share between threads.
 *
 * @since 1.0.0
 */
public class Input internal constructor(private val request: Request) {
    private val context = request.context

    private val reader = ParameterReader(request)

    private val findings: InputFindings = InputFindings()

    /**
     * Whether every reader so far found what it was asked for.
     *
     * @since 1.0.0
     */
    public val isValid: Boolean get() = findings.isValid

    /**
     * Reads a parameter whose schema is a single value.
     *
     * @param T the type of the value.
     * @param place where the parameter lies.
     * @param format the name, the style, `explode` and `allowReserved` of the parameter.
     * @param type how the text becomes a value.
     * @param isRequired whether the parameter is required.
     * @param rule the rules of the schema a decoded value must keep, such as `minimum` or `maxLength`, which report
     * through [ValidationScope.fail] under the path of the parameter; none by default.
     * @return the value, or `null` if it is missing or malformed.
     * @since 1.0.0
     */
    public fun <T : Any> value(
        place: Place,
        format: ParameterFormat,
        type: ParameterType<T>,
        isRequired: Boolean,
        rule: (ValidationScope.(T) -> Unit)? = null,
    ): T? {
        val path = pathOf(place, format)
        val reading = reader.read(place = place, format = format, shape = ParameterShape.Text)
        val text = present(reading, path, isRequired) as? ParameterValue.Text ?: return null
        val value = decoded(type, text.value)
        return value?.let { found -> findings.ruled(found, path, rule) } ?: findings.violated(path)
    }

    /**
     * Reads a parameter whose schema is an array.
     *
     * @param T the type of an item.
     * @param place where the parameter lies.
     * @param format the name, the style, `explode` and `allowReserved` of the parameter.
     * @param type how the text of an item becomes a value.
     * @param isRequired whether the parameter is required.
     * @param rule the rules of the schema the items must keep, such as `minItems` or the rules of each item, which
     * report through [ValidationScope.fail] under the path of the parameter; none by default.
     * @return the items, or `null` if the parameter is missing or an item is malformed.
     * @since 1.0.0
     */
    public fun <T : Any> items(
        place: Place,
        format: ParameterFormat,
        type: ParameterType<T>,
        isRequired: Boolean,
        rule: (ValidationScope.(List<T>) -> Unit)? = null,
    ): List<T>? {
        val path = pathOf(place, format)
        val reading = reader.read(place = place, format = format, shape = ParameterShape.Items)
        val items = present(reading, path, isRequired) as? ParameterValue.Items ?: return null
        val values = items.values.mapNotNull { text -> decoded(type, text) }
        val complete = values.takeIf { values.size == items.values.size }
        return complete?.let { found -> findings.ruled(found, path, rule) } ?: findings.violated(path)
    }

    /**
     * Reads a parameter whose schema is an object, such as the `deepObject` style or an exploded form.
     *
     * Every property is handed to [deserializer] as a JSON string, which a number or a flag of the schema reads as
     * well.
     *
     * @param T the type of the object.
     * @param place where the parameter lies.
     * @param format the name, the style, `explode` and `allowReserved` of the parameter.
     * @param deserializer the deserializer of the object.
     * @param isRequired whether the parameter is required.
     * @param fields the names of the properties, which an exploded form or cookie needs to find them.
     * @param rule the rules of the schema a decoded value must keep, such as `minimum` or `maxLength`, which report
     * through [ValidationScope.fail] under the path of the parameter; none by default.
     * @return the object, or `null` if it is missing or malformed.
     * @since 1.0.0
     */
    public fun <T : Any> fields(
        place: Place,
        format: ParameterFormat,
        deserializer: DeserializationStrategy<T>,
        isRequired: Boolean,
        fields: Set<String> = emptySet(),
        rule: (ValidationScope.(T) -> Unit)? = null,
    ): T? {
        val path = pathOf(place, format)
        val reading = reader.read(place = place, format = format, shape = ParameterShape.Fields, fields = fields)
        val found = present(reading, path, isRequired) as? ParameterValue.Fields ?: return null
        val element = JsonObject(found.values.mapValues { (_, text) -> JsonPrimitive(text) })
        val value = contained { context.codecs.json.decodeFromJsonElement(deserializer, element) }.getOrNull()
        val checked = value?.let { found -> findings.ruled(findings.checked(found, path), path, rule) }
        return checked ?: findings.violated(path)
    }

    /**
     * Reads a parameter `in: querystring` of OpenAPI 3.2 §4.12.2: the whole query as one value of [mediaType].
     *
     * @param T the type of the value.
     * @param mediaType the media type of the parameter, such as `application/x-www-form-urlencoded`.
     * @param deserializer the deserializer of the value.
     * @param isRequired whether the parameter is required.
     * @param rule the rules of the schema a decoded value must keep, such as `minimum` or `maxLength`, which report
     * through [ValidationScope.fail] under the path of the parameter; none by default.
     * @return the value, or `null` if the query is missing or malformed.
     * @since 1.0.0
     */
    public fun <T : Any> queryString(
        mediaType: MediaType,
        deserializer: DeserializationStrategy<T>,
        isRequired: Boolean,
        rule: (ValidationScope.(T) -> Unit)? = null,
    ): T? {
        val reading = QueryString.read(request.rawQuery?.takeIf(String::isNotEmpty), mediaType)
        val text = present(reading, QUERY, isRequired) as? ParameterValue.Text ?: return null
        val content = text.value.encodeToByteString()
        return findings.decoded(QUERY, context.codecs.decode(deserializer, content, mediaType), rule)
    }

    /**
     * Reads the body as a value of one of [mediaTypes], decoded by the codec of the server that covers its
     * `Content-Type`, and validates it when it is [Validatable].
     *
     * @param T the type of the value.
     * @param deserializer the deserializer of the value.
     * @param isRequired whether the body is required.
     * @param mediaTypes the media types the operation reads; every type by default.
     * @return the value, or `null` if the body is missing, refused or malformed.
     * @throws IllegalStateException if the body was read before.
     * @since 1.0.0
     */
    public suspend fun <T : Any> body(
        deserializer: DeserializationStrategy<T>,
        isRequired: Boolean,
        mediaTypes: List<MediaRange> = listOf(MediaRange.Any),
    ): T? {
        val type = typed(isRequired, mediaTypes)?.takeIf(::isDecodable)
        val content = type?.let { content(isRequired, context.limits.maxRequestBytes) }
        val decoding = content?.let { bytes -> context.codecs.decode(deserializer, bytes, type) }
        return when (decoding) {
            null -> null
            is Decoding.Failed -> malformedBody()
            is Decoding.Decoded -> findings.checked(decoding.value, BODY)
        }
    }

    /**
     * Reads the body as bytes of one of [mediaTypes].
     *
     * @param isRequired whether the body is required.
     * @param mediaTypes the media types the operation reads; every type by default.
     * @return the bytes, or `null` if the body is missing or refused.
     * @throws IllegalStateException if the body was read before.
     * @since 1.0.0
     */
    public suspend fun bytes(isRequired: Boolean, mediaTypes: List<MediaRange> = listOf(MediaRange.Any)): ByteString? {
        typed(isRequired, mediaTypes) ?: return null
        return content(isRequired, context.limits.maxRequestBytes)
    }

    /**
     * Reads a body of `multipart/form-data` of RFC 7578 as a whole, within the multipart limits of the server.
     *
     * @param isRequired whether the body is required.
     * @return the parts in order, or `null` if the body is missing, refused or malformed.
     * @throws IllegalStateException if the body was read before.
     * @since 1.0.0
     */
    public suspend fun parts(isRequired: Boolean): List<MultipartPart>? {
        val boundary = boundaryOf(isRequired) ?: return null
        val limits = context.limits.multipart
        val content = content(isRequired, minOf(a = context.limits.maxRequestBytes, b = limits.maxBodyBytes))
        return when (val decoded = content?.let { body -> MultipartFormData.decode(body, boundary, limits) }) {
            null -> null
            is Outcome.Success -> decoded.value
            is Outcome.Failure -> multipartRefused(MultipartRefusals.statusOf(decoded.failure))
        }
    }

    /**
     * Reads the first part named [name] of [parts] as a value, decoded by the codec of the server that covers the
     * `Content-Type` of the part, or [mediaType] when the part names none, and validates it when it is [Validatable].
     *
     * A part without a type is `text/plain` by RFC 7578 §4.4; the encoding object of OpenAPI 3.2 §4.15 declares
     * `application/json` for an object instead, which the caller passes as [mediaType]. A missing required part or
     * one the codec cannot read is a violation under `body.<name>`; a part of a type no codec of the server reads is
     * malformed as well, since the body as a whole was accepted.
     *
     * @param T the type of the value.
     * @param parts the parts [Input.parts] read, or `null` if the body is missing or was refused.
     * @param name the name of the part in its `Content-Disposition`.
     * @param deserializer the deserializer of the value.
     * @param isRequired whether the part is required.
     * @param mediaType the media type of a part that names none; `text/plain` by default.
     * @param rule the rules of the schema a decoded value must keep, such as `minimum` or `maxLength`, which report
     * through [ValidationScope.fail] under the path of the parameter; none by default.
     * @return the value, or `null` if the part is missing or malformed.
     * @since 1.0.0
     */
    public fun <T : Any> part(
        parts: List<MultipartPart>?,
        name: String,
        deserializer: DeserializationStrategy<T>,
        isRequired: Boolean,
        mediaType: MediaType = MediaType.TextPlain,
        rule: (ValidationScope.(T) -> Unit)? = null,
    ): T? {
        val found = parts?.firstOrNull { part -> part.name == name }
        val path = pathOf(name)
        if (found == null) return findings.absent(path, isRequired && parts != null)
        val type = found.contentType ?: mediaType
        return findings.decoded(path, context.codecs.decode(deserializer, found.content, type), rule)
    }

    /**
     * Reads every part named [name] of [parts] as a value, in order, as [part] reads one.
     *
     * @param T the type of an item.
     * @param parts the parts [Input.parts] read, or `null` if the body is missing or was refused.
     * @param name the name of the parts in their `Content-Disposition`.
     * @param deserializer the deserializer of an item.
     * @param mediaType the media type of a part that names none; `text/plain` by default.
     * @param rule the rules of the schema the items must keep, such as `minItems` or the rules of each item, which
     * report through [ValidationScope.fail] under the path of the parameter; none by default.
     * @return the items, empty if there is no such part, or `null` if [parts] is `null` or a part is malformed.
     * @since 1.0.0
     */
    public fun <T : Any> partItems(
        parts: List<MultipartPart>?,
        name: String,
        deserializer: DeserializationStrategy<T>,
        mediaType: MediaType = MediaType.TextPlain,
        rule: (ValidationScope.(List<T>) -> Unit)? = null,
    ): List<T>? {
        val named = parts?.filter { part -> part.name == name } ?: return null
        val path = pathOf(name)
        val values = named.mapNotNull { part ->
            findings.decoded(path, context.codecs.decode(deserializer, part.content, part.contentType ?: mediaType))
        }
        return values.takeIf { values.size == named.size }?.let { complete -> findings.ruled(complete, path, rule) }
    }

    /**
     * Reads [text], such as a field of a form or a text part, as a value of [mediaType] through the codec of the
     * server that covers it, and validates it when it is [Validatable].
     *
     * A form of `application/x-www-form-urlencoded` carries every field as text; a field whose schema is no string,
     * such as a number or an object, is read as JSON by default, as the encoding object of OpenAPI 3.2 §4.15 declares.
     * A missing required field or a text the codec cannot read is a violation under `body.<name>`.
     *
     * @param T the type of the value.
     * @param name the name of the field, which names the violation.
     * @param text the text of the field, or `null` if it is missing.
     * @param deserializer the deserializer of the value.
     * @param isRequired whether the field is required.
     * @param mediaType the media type of the text; `application/json` by default.
     * @param rule the rules of the schema a decoded value must keep, such as `minimum` or `maxLength`, which report
     * through [ValidationScope.fail] under the path of the parameter; none by default.
     * @return the value, or `null` if the field is missing or malformed.
     * @since 1.0.0
     */
    public fun <T : Any> field(
        name: String,
        text: String?,
        deserializer: DeserializationStrategy<T>,
        isRequired: Boolean,
        mediaType: MediaType = MediaType.Json,
        rule: (ValidationScope.(T) -> Unit)? = null,
    ): T? {
        val path = pathOf(name)
        val content = text?.encodeToByteString() ?: return findings.absent(path, isRequired)
        return findings.decoded(path, context.codecs.decode(deserializer, content, mediaType), rule)
    }

    /**
     * Opens the body as a raw upload of one of [mediaTypes], which the handler reads in pieces.
     *
     * @param isRequired whether the body is required.
     * @param mediaTypes the media types the operation reads; every type by default.
     * @return the upload, or `null` if the body is missing or of another media type.
     * @since 1.0.0
     */
    public fun upload(isRequired: Boolean, mediaTypes: List<MediaRange> = listOf(MediaRange.Any)): Upload? {
        val type = typed(isRequired, mediaTypes) ?: return null
        return Upload(
            contentType = type,
            length = request.body.declaredLength,
            source = request.body,
            limit = context.limits.maxUploadBytes,
        )
    }

    /**
     * Opens a body of `multipart/form-data` of RFC 7578 as a streamed upload.
     *
     * @param isRequired whether the body is required.
     * @return the upload, or `null` if the body is missing, of another media type or without a valid boundary.
     * @since 1.0.0
     */
    public fun multipart(isRequired: Boolean): MultipartUpload? {
        val boundary = boundaryOf(isRequired) ?: return null
        return MultipartUpload(
            boundary = boundary,
            limits = context.limits.multipart,
            source = request.body,
            limit = context.limits.maxUploadBytes,
        )
    }

    /**
     * Chooses the media type of the reply among [offered] by the `Accept` of the request, with the negotiation of
     * RFC 9110 §12.5.1 of the runtime core.
     *
     * A request without `Accept`, or with one that is malformed or exceeds the field limits of the server, accepts
     * the first offer. When more than one media type is offered, the reply carries `Vary: accept`.
     *
     * @param offered the media types the operation answers with, in the order the server prefers them.
     * @return the chosen media type, or `null` if no offer is acceptable, which refuses the request with 406.
     * @since 1.0.0
     */
    public fun accept(offered: List<MediaType>): MediaType? {
        if (offered.size > 1) request.state.negotiated()
        val values = request.headers.all(ACCEPT).takeIf { found -> found.isNotEmpty() }
        val accepted = values?.let { found ->
            AcceptFields.parseAccept(
                found.joinToString(", "),
                context.limits.fields,
            )
        }
        val chosen = Negotiation.chooseMediaType(accepted, offered)
        return chosen ?: findings.refuse(HttpStatus.NOT_ACCEPTABLE, ServerCodes.NOT_ACCEPTABLE)
    }

    /**
     * Returns the problem detail of RFC 9457 that answers this request: the first refusal with every violation, or a
     * problem with status 400 and every violation.
     *
     * @return the reply with an `application/problem+json` body.
     * @since 1.0.0
     */
    public fun refused(): Reply {
        val found = findings.refusal ?: return context.problems.invalid(findings.violations)
        return context.problems.problem(
            status = found.status,
            code = found.code,
            violations = findings.violations,
            headers = found.headers,
        )
    }

    override fun toString(): String = "Input(${request.operation}, violations=${findings.violations.size})"

    private fun typed(isRequired: Boolean, mediaTypes: List<MediaRange>): MediaType? {
        if (!request.hasBody) return findings.absent(BODY, isRequired)
        val type = request.contentType?.takeIf { found -> mediaTypes.any { range -> range.matches(found) } }
        return type ?: findings.refuse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, UNSUPPORTED)
    }

    private fun isDecodable(type: MediaType): Boolean {
        val isCovered = context.codecs.codecFor(type) != null
        if (!isCovered) findings.refuse<Unit>(HttpStatus.UNSUPPORTED_MEDIA_TYPE, UNSUPPORTED)
        return isCovered
    }

    private fun boundaryOf(isRequired: Boolean): Boundary? {
        val type = typed(isRequired, listOf(MediaRange.of(MediaType.MultipartFormData))) ?: return null
        return MultipartFormData.boundaryOf(type) ?: malformedBody()
    }

    private fun <T> malformedBody(): T? = findings.violated(BODY)

    private suspend fun content(isRequired: Boolean, limit: Long): ByteString? =
        when (val read = request.body.bytes(minOf(a = limit, b = context.codecs.maxContentBytes.toLong()))) {
            is BodyRead.Refused -> refused(read.refusal)
            is BodyRead.Read -> read.value.takeIf { bytes -> bytes.size > 0 } ?: findings.absent(BODY, isRequired)
        }

    private fun present(reading: ParameterReading, path: String, isRequired: Boolean): ParameterValue? =
        when (reading) {
            ParameterReading.Absent -> findings.absent(path, isRequired)
            ParameterReading.Malformed -> findings.violated(path)
            is ParameterReading.Present -> reading.value
        }

    private fun <T : Any> decoded(type: ParameterType<T>, text: String): T? {
        val first = text.firstOrNull()
        val isStructured = !type.isQuoted && first != null && first in STRUCTURED_STARTS
        val quoted = JsonPrimitive(text).toString()
        val source = quoted.takeIf { type.isQuoted } ?: text
        val json = context.codecs.json
        val value = contained { json.decodeFromString(type.deserializer, source) }.getOrNull()
        return value?.takeUnless { isStructured }
    }

    private fun <T> refused(found: BodyRefusal): T? {
        val headers = listOf(ACCEPT_ENCODING to SUPPORTED_CODINGS).takeIf { found == BodyRefusal.UnsupportedEncoding }
        return findings.refuse(found.status, found.code, headers.orEmpty())
    }

    private fun <T> multipartRefused(status: Int): T? {
        if (status == HttpStatus.CONTENT_TOO_LARGE) return findings.refuse(status, ServerCodes.TOO_LARGE)
        return findings.violated(BODY)
    }

    private companion object {
        const val UNSUPPORTED: String = ServerCodes.UNSUPPORTED_MEDIA_TYPE
    }
}
