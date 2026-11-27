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

@file:OptIn(ExperimentalAtomicApi::class)

package com.davils.kapi.runtime.client

import com.davils.kapi.runtime.client.call.Body
import com.davils.kapi.runtime.client.call.Call
import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.client.internal.Authorization
import com.davils.kapi.runtime.client.internal.ClientTelemetry
import com.davils.kapi.runtime.client.internal.Pipeline
import com.davils.kapi.runtime.client.internal.Transport
import com.davils.kapi.runtime.client.pipeline.RoundTrip
import com.davils.kapi.runtime.client.retry.Attempt
import com.davils.kapi.runtime.core.codec.Decoding
import com.davils.kapi.runtime.core.negotiation.MediaRange
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.parameter.ParameterValue
import com.davils.kontinuity.core.rejectionOrNull
import com.davils.kontinuity.policy.Policy
import com.davils.kore.core.failure.attempted
import com.davils.kore.core.failure.contained
import io.ktor.client.HttpClient
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

private const val UNAUTHORIZED: Int = 401

private const val PIPELINE: String = "pipeline"

private const val CONTENT_TYPE: String = "content-type"

private const val CHALLENGE: String = "www-authenticate"

private const val NONCE: String = "dpop-nonce"

private const val NONCE_ERROR: String = "use_dpop_nonce"

/**
 * Calls the operations of an API over the Ktor `HttpClient` of the application; the client a generated client runs
 * on, built by [apiClient].
 *
 * A call passes its phases: the interceptors of [ClientSettings.interceptions], the credentials of the client, the
 * destination policy for its URL and every redirect hop, and the transport, which reads an answer up to
 * [ClientSettings.maxResponseBytes] and never throws for a status. A call the server rejects with `401` is sent once
 * more after a source of its credentials refreshed or a DPoP nonce arrived. Every call, and every attempt of a
 * retried one, runs under [ClientSettings.protection]; only a retryable call runs under [ClientSettings.resilience]
 * as well, so a call that is not idempotent is never sent twice. Every call is counted, timed and traced in kollect.
 * The client never creates an `HttpClient`, never closes the one it was handed and is safe to share between
 * coroutines and threads.
 *
 * @since 1.0.0
 */
public class ApiClient internal constructor(
    internal val http: HttpClient,
    /**
     * The settings of the client.
     *
     * @since 1.0.0
     */
    public val settings: ClientSettings,
) {
    internal val telemetry: ClientTelemetry = ClientTelemetry(settings)

    internal val authorization: Authorization = Authorization(settings.credentials, settings.dpop)

    private val transport: Transport = Transport(http, settings, telemetry)

    private val pipeline: Pipeline = Pipeline(settings.interceptions, authorization, transport)

    /**
     * Sends [call] and returns how it ended.
     *
     * @param call the call.
     * @return the answer of the server, whatever its status, or why there is none; a call that carries a
     * [Call.refusal] is not sent and fails with it.
     * @since 1.0.0
     */
    public suspend fun execute(call: Call): Exchange {
        val refusal = call.refusal ?: return telemetry.calling(call) { resilient(call) }
        return Exchange.Failed(refusal)
    }

    /**
     * Reads the content of [answered] as [deserializer] with the codec its `Content-Type` names.
     *
     * @param T the type of the content.
     * @param deserializer the deserializer.
     * @param answered the answer.
     * @return the value, or why it could not be read: no content type, no codec for it, content larger than the
     * codecs read or content that is not what [deserializer] expects.
     * @since 1.0.0
     */
    public fun <T> decode(deserializer: DeserializationStrategy<T>, answered: Exchange.Answered): Decoding<T> =
        settings.codecs.decode(deserializer, answered.content, answered.headers[CONTENT_TYPE])

    /**
     * Writes [value] as the content of a call of the media type [mediaType].
     *
     * @param T the type of the value.
     * @param serializer the serializer.
     * @param value the value.
     * @param mediaType the media type; JSON by default.
     * @return the body, or `null` if no codec writes [mediaType] or the codec cannot write [value].
     * @since 1.0.0
     */
    public fun <T> encode(
        serializer: SerializationStrategy<T>,
        value: T,
        mediaType: MediaType = MediaType.Json,
    ): Body? {
        val codec = settings.codecs.codecFor(mediaType) ?: return null
        val content = contained { codec.encode(serializer, value) }.getOrNull()
        return content?.let { found -> Body(mediaType, found) }
    }

    /**
     * Writes [value] as JSON with the JSON of the codecs of the client.
     *
     * @param T the type of the value.
     * @param serializer the serializer.
     * @param value the value.
     * @return the UTF-8 bytes of the JSON text.
     * @since 1.0.0
     */
    public fun <T> json(serializer: SerializationStrategy<T>, value: T): ByteString {
        val text = settings.codecs.json.encodeToString(serializer, value)
        return text.encodeToByteString()
    }

    /**
     * Writes the members of the object [value] as the fields of a parameter, such as a `deepObject` query parameter.
     *
     * @param T the type of the value.
     * @param serializer the serializer.
     * @param value the value.
     * @return every member that is not null, a text as its content and anything else as its JSON; no fields for a
     * value that is no object.
     * @since 1.0.0
     */
    public fun <T> fields(serializer: SerializationStrategy<T>, value: T): ParameterValue.Fields {
        val element = settings.codecs.json.encodeToJsonElement(serializer, value)
        val members = (element as? JsonObject).orEmpty()
        val present = members.filterValues { member -> member !is JsonNull }
        return ParameterValue.Fields(present.mapValues { (_, member) -> textOf(member) })
    }

    /**
     * Returns the media types of [offered] the codecs of the client can read, as the ranges of an `Accept` field, so
     * a call never asks for content it could not read.
     *
     * @param offered the media types the responses of an operation offer, in the order of the document.
     * @return the readable ones in the same order.
     * @since 1.0.0
     */
    public fun acceptable(offered: List<MediaType>): List<MediaRange> {
        val readable = offered.filter { mediaType -> settings.codecs.codecFor(mediaType) != null }
        return readable.map(MediaRange::of)
    }

    override fun toString(): String = "ApiClient($settings)"

    internal suspend fun streamed(call: Call): Exchange {
        val refusal = call.refusal ?: return telemetry.calling(call) { attempt(call) }
        return Exchange.Failed(refusal)
    }

    private fun textOf(member: JsonElement): String {
        val primitive = member as? JsonPrimitive ?: return member.toString()
        return primitive.content
    }

    private suspend fun resilient(call: Call): Exchange {
        val policy = settings.resilience?.takeIf { call.isRetryable } ?: return shielded(call)
        return guarded(policy, call)
    }

    private suspend fun shielded(call: Call): Exchange {
        val protection = settings.protection ?: return attempt(call)
        val outcome = attempted { protection.execute { Attempt(call, attempt(call)) } }
        return outcome.fold(
            onSuccess = { attempt -> attempt.exchange },
            onFailure = { failure -> refusedOf(failure, Exchange.Failed(CallFailure.TimedOut)) },
        )
    }

    private suspend fun guarded(policy: Policy, call: Call): Exchange {
        val attempts = AtomicInt(0)
        val last = AtomicReference<Exchange>(Exchange.Failed(CallFailure.TimedOut))
        val outcome = attempted {
            policy.execute {
                val number = attempts.incrementAndFetch()
                if (number > 1) telemetry.emit { retrying(call, number) }
                val attempt = Attempt(call, shielded(call))
                last.store(attempt.exchange)
                attempt
            }
        }
        return outcome.fold(
            onSuccess = { attempt -> attempt.exchange },
            onFailure = { failure -> refusedOf(failure, last.load()) },
        )
    }

    private fun retrying(call: Call, number: Int): ClientEvent =
        ClientEvent.Retrying(api = settings.name, operation = call.operation, attempt = number)

    private fun refusedOf(failure: Throwable, last: Exchange): Exchange {
        val rejected = failure.rejectionOrNull()?.let { found -> Exchange.Failed(CallFailure.Rejected(found.kind.id)) }
        return rejected ?: last
    }

    private suspend fun attempt(call: Call): Exchange {
        val first = pipeline.run(call)
        val exchange = first.exchange ?: Exchange.Failed(CallFailure.Unsent(PIPELINE))
        val answered = (exchange as? Exchange.Answered)?.takeIf { found -> found.status == UNAUTHORIZED }
        if (answered == null || !call.isReplayable) return exchange
        return retried(call = call, first = first, answered = answered)
    }

    private suspend fun retried(call: Call, first: RoundTrip, answered: Exchange.Answered): Exchange {
        val challenge = answered.headers[CHALLENGE].orEmpty()
        val isNonce = first.granted.binding != null && NONCE_ERROR in challenge && NONCE in answered.headers
        val isAgain = isNonce || refreshed(call, first)
        if (!isAgain) return answered
        return pipeline.run(call).exchange ?: answered
    }

    private suspend fun refreshed(call: Call, first: RoundTrip): Boolean {
        val results = authorization.refresh(first.secrets)
        results.forEach { (scheme, isRefreshed) ->
            telemetry.emit {
                ClientEvent.Refreshed(
                    api = settings.name,
                    operation = call.operation,
                    scheme = scheme,
                    isRefreshed = isRefreshed,
                )
            }
        }
        return results.any { (_, isRefreshed) -> isRefreshed }
    }
}
