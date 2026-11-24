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

package com.davils.kapi.codegen.core.asyncapi.read

import com.davils.kapi.codegen.core.asyncapi.GenerationCodes
import com.davils.kapi.codegen.core.asyncapi.plan.ChannelPlan
import com.davils.kapi.codegen.core.asyncapi.plan.DirectionPlan
import com.davils.kapi.codegen.core.pipeline.Findings
import com.davils.kapi.codegen.core.pipeline.shown
import com.davils.kapi.spec.core.diagnostic.Location

internal class ChannelNames(private val report: Findings) {
    fun audit(plans: List<ChannelPlan>) {
        unique(plans.map { plan -> plan.base to plan.location }, "channels")
        plans.forEach(::auditChannel)
    }

    private fun auditChannel(plan: ChannelPlan) {
        val subject = "the channel ${shown(plan.key)}"
        unique(members(plan.upstream), "messages the client sends on $subject")
        unique(members(plan.downstream), "messages the server sends on $subject")
        unique(plan.requests.map { request -> request.function to plan.location }, "operations of $subject")
        val parameters = plan.parameters.map { parameter -> parameter.member to plan.location }
        val fields = plan.handshake.map { field -> field.member to plan.location }
        unique(parameters + fields, "parameters of $subject")
    }

    private fun members(direction: DirectionPlan): List<Pair<String, Location?>> =
        direction.messages.map { message -> message.member to message.location }

    private fun unique(names: List<Pair<String, Location?>>, subject: String) {
        val seen = mutableSetOf<String>()
        names.forEach { (name, location) ->
            val isNew = seen.add(name)
            if (!isNew) {
                val text = "Two $subject become the Kotlin name ${shown(name)}"
                report.error(GenerationCodes.NAME_COLLISION, text, location)
            }
        }
    }
}
