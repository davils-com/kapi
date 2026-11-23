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

package com.davils.kapi.codegen.core.asyncapi.emit

import com.davils.kapi.codegen.core.asyncapi.plan.ChannelPlan
import com.davils.kapi.codegen.core.asyncapi.plan.DirectionPlan
import com.davils.kapi.codegen.core.asyncapi.plan.MessagePlan
import com.davils.kapi.codegen.core.asyncapi.plan.Side

internal object Kdocs {
    fun direction(plan: ChannelPlan, side: Side): String {
        val direction = side.of(plan)
        val action = "receive".takeIf { side == Side.Client } ?: "send"
        val operations = direction.operations.joinToString { operation -> code(operation) }
        val sender = side.label.lowercase()
        val receiver = side.other.label.lowercase()
        val head = "The messages the $sender sends and the $receiver receives on the channel ${code(plan.key)}."
        val why = "AsyncAPI describes the server: these are the messages of its operations with the action " +
            "`$action` ($operations) and the replies to the requests of the other side."
        val tag = direction.discriminator?.let { property ->
            "A message is told apart from the others by the constant value of its property ${code(property)}; a " +
                "message whose property names none of them is refused."
        }
        return listOfNotNull(head, why, tag).joinToString("\n\n")
    }

    fun message(direction: DirectionPlan, message: MessagePlan): String {
        val summary = message.summary?.let(Texts::kdoc)
        val head = "The message ${code(message.key)}."
        val tag = message.tag?.let { value ->
            "Its property ${code(direction.discriminator.orEmpty())} is ${code(value)}."
        }
        val wrapped = "@property payload the payload of the message."
        return listOfNotNull(head, summary, tag, wrapped).joinToString("\n\n")
    }

    fun channel(plan: ChannelPlan, subject: String): String {
        val summary = plan.summary?.let(Texts::kdoc)
        val head = "$subject the WebSocket channel ${code(plan.key)} at ${code(plan.template)}."
        return listOfNotNull(head, summary).joinToString("\n\n")
    }

    fun code(text: String): String = "`" + Texts.kdoc(text).replace(oldValue = "`", newValue = "'") + "`"
}
