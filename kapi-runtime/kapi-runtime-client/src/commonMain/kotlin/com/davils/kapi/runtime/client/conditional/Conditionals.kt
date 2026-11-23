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

import com.davils.kapi.runtime.client.call.CallBuilder
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.core.conditional.EntityTagCondition
import com.davils.kapi.runtime.core.conditional.HttpDate
import com.davils.kapi.runtime.core.response.isSuccess

private const val NOT_MODIFIED: Int = 304

private const val PRECONDITION_FAILED: Int = 412

/**
 * Sends `If-Match` of RFC 9110 §13.1.1, so a change is made only to the representation the client read.
 *
 * @receiver the call.
 * @param condition `*` or the entity tags the representation must match strongly.
 * @since 1.0.0
 */
public fun CallBuilder.ifMatch(condition: EntityTagCondition) {
    header(name = "If-Match", value = condition.toString())
}

/**
 * Sends `If-None-Match` of RFC 9110 §13.1.2, so a read answers `304 Not Modified` while the representation still
 * matches, or a creation fails when one exists.
 *
 * @receiver the call.
 * @param condition `*` or the entity tags the representation must not match weakly.
 * @since 1.0.0
 */
public fun CallBuilder.ifNoneMatch(condition: EntityTagCondition) {
    header(name = "If-None-Match", value = condition.toString())
}

/**
 * Sends `If-Modified-Since` of RFC 9110 §13.1.3.
 *
 * @receiver the call.
 * @param date the time of the representation the client holds.
 * @since 1.0.0
 */
public fun CallBuilder.ifModifiedSince(date: HttpDate) {
    header(name = "If-Modified-Since", value = date.toString())
}

/**
 * Sends `If-Unmodified-Since` of RFC 9110 §13.1.4.
 *
 * @receiver the call.
 * @param date the time of the representation the client read.
 * @since 1.0.0
 */
public fun CallBuilder.ifUnmodifiedSince(date: HttpDate) {
    header(name = "If-Unmodified-Since", value = date.toString())
}

/**
 * Asks whether the representation of [validators] changed: `If-None-Match` with its entity tag and
 * `If-Modified-Since` with its time, each when it is known, as a cache revalidates by RFC 9111 §4.3.1.
 *
 * @receiver the call.
 * @param validators the validators of the representation the client holds.
 * @since 1.0.0
 */
public fun CallBuilder.revalidate(validators: Validators) {
    validators.entityTag?.let { tag -> ifNoneMatch(EntityTagCondition.Tags(listOf(tag))) }
    validators.lastModified?.let { date -> ifModifiedSince(date) }
}

/**
 * Reads how a conditional call ended.
 *
 * @receiver how the call ended.
 * @return [ConditionalOutcome.NotModified] for `304`, [ConditionalOutcome.PreconditionFailed] for `412`,
 * [ConditionalOutcome.Modified] for a successful answer and [ConditionalOutcome.Other] for anything else.
 * @since 1.0.0
 */
public fun Exchange.conditional(): ConditionalOutcome {
    val answered = this as? Exchange.Answered ?: return ConditionalOutcome.Other(this)
    val validators = Validators.of(answered.headers)
    val notModified = ConditionalOutcome.NotModified(validators).takeIf { answered.status == NOT_MODIFIED }
    val failed = ConditionalOutcome.PreconditionFailed(answered).takeIf { answered.status == PRECONDITION_FAILED }
    val modified = ConditionalOutcome.Modified(answered, validators).takeIf { answered.isSuccess }
    return notModified ?: failed ?: modified ?: ConditionalOutcome.Other(answered)
}
