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

package com.davils.kapi.runtime.client.conditional

import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.client.call.apiCall
import com.davils.kapi.runtime.core.conditional.EntityTag
import com.davils.kapi.runtime.core.conditional.EntityTagCondition
import com.davils.kapi.runtime.core.conditional.HttpDate
import com.davils.kapi.runtime.core.parameter.ParameterEntry
import com.davils.kapi.runtime.core.response.headersOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.io.bytestring.ByteString

private const val DATE: Long = 1_700_000_000

private const val TAG: String = "\"v1\""

private const val ETAG: String = "ETag"

private const val OK: Int = 200

private const val NOT_MODIFIED: Int = 304

private const val PRECONDITION_FAILED: Int = 412

private const val SERVER_ERROR: Int = 500

private const val OUTCOMES: Int = 4

private val tag: EntityTag = EntityTag("v1")

private val date: HttpDate = HttpDate(DATE)

private fun answered(status: Int, vararg headers: Pair<String, String>): Exchange.Answered = Exchange.Answered(
    status = status,
    headers = headersOf(headers.associate { (name, value) -> name to listOf(value) }),
    content = ByteString(),
)

private fun entries(vararg pairs: Pair<String, String>): List<ParameterEntry> =
    pairs.map { (name, value) -> ParameterEntry(name = name, value = value) }

class ConditionalsTest : FunSpec({
    context("the conditions of a call") {
        test("are the fields of RFC 9110 written from their values") {
            val call = apiCall(operation = "updatePet", method = "PUT") {
                ifMatch(EntityTagCondition.Tags(listOf(tag)))
                ifNoneMatch(EntityTagCondition.Any)
                ifModifiedSince(date)
                ifUnmodifiedSince(date)
            }

            call.headers shouldBe entries(
                "If-Match" to TAG,
                "If-None-Match" to "*",
                "If-Modified-Since" to date.toString(),
                "If-Unmodified-Since" to date.toString(),
            )
        }

        test("revalidate what the client holds by its entity tag and its time") {
            val call = apiCall(operation = "getPet", method = "GET") { revalidate(Validators(tag, date)) }
            val bare = apiCall(operation = "getPet", method = "GET") { revalidate(Validators()) }

            call.headers.map(ParameterEntry::name) shouldBe listOf("If-None-Match", "If-Modified-Since")
            bare.headers shouldBe emptyList()
        }
    }

    context("the validators of an answer") {
        test("are its entity tag and its time, without what cannot be read") {
            val headers = headersOf(mapOf(ETAG to listOf("W/\"v2\""), "Last-Modified" to listOf(date.toString())))
            val validators = Validators.of(headers)
            val weak = Validators(EntityTag("v2", isWeak = true), date)

            validators shouldBe weak
            validators.isEmpty shouldBe false
            Validators.of(headersOf(mapOf(ETAG to listOf("v2")))).isEmpty shouldBe true
            validators.toString() shouldBe """Validators(W/"v2", $date)"""
            Validators().toString() shouldBe "Validators(none, none)"
            validators.hashCode() shouldBe weak.hashCode()
            validators shouldNotBe Validators()
        }
    }

    context("how a conditional call ended") {
        test("is told by its status") {
            val modified = answered(OK, ETAG to TAG).conditional()
            val notModified = answered(NOT_MODIFIED, ETAG to TAG).conditional()
            val failed = answered(PRECONDITION_FAILED).conditional()
            val other = answered(SERVER_ERROR).conditional()
            val unsent = Exchange.Failed(CallFailure.TimedOut).conditional()
            val outcomes = listOf(modified, notModified, failed, other)

            modified.shouldBeInstanceOf<ConditionalOutcome.Modified>().validators.entityTag shouldBe tag
            notModified shouldBe ConditionalOutcome.NotModified(Validators(tag))
            failed shouldBe ConditionalOutcome.PreconditionFailed(answered(PRECONDITION_FAILED))
            other shouldBe ConditionalOutcome.Other(answered(SERVER_ERROR))
            unsent shouldBe ConditionalOutcome.Other(Exchange.Failed(CallFailure.TimedOut))
            outcomes.map { outcome -> outcome.hashCode() }.size shouldBe OUTCOMES
            notModified.toString() shouldBe """NotModified(Validators("v1", none))"""
            failed.toString() shouldBe "PreconditionFailed(${answered(PRECONDITION_FAILED)})"
            other.toString() shouldBe "Other(${answered(SERVER_ERROR)})"
            modified.toString() shouldBe """Modified(${answered(OK, ETAG to TAG)}, Validators("v1", none))"""
            modified shouldBe answered(OK, ETAG to TAG).conditional()
        }
    }
})
