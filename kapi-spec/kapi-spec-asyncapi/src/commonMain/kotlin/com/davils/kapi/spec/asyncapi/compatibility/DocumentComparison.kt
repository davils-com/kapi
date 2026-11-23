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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.spec.asyncapi.compatibility

import com.davils.kapi.spec.asyncapi.Document
import com.davils.kapi.spec.asyncapi.model.channel.Channel
import com.davils.kapi.spec.asyncapi.model.server.Server
import com.davils.kapi.spec.asyncapi.resolve
import com.davils.kapi.spec.core.compatibility.ChangeLog
import com.davils.kapi.spec.core.compatibility.Comparison
import com.davils.kapi.spec.core.compatibility.ComparisonSettings
import com.davils.kapi.spec.core.compatibility.Impact
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kore.core.annotation.DavilsInternal

private const val PREFIX: String = "asyncapi"

internal class DocumentComparison(baseline: Document, candidate: Document, settings: ComparisonSettings) {
    private val context: Context =
        Context(baseline = baseline, candidate = candidate, log = ChangeLog(prefix = PREFIX, settings = settings))

    private val entries: Entries = Entries(context)

    fun compare(): Comparison {
        val servers = Both(before = context.baseline.servers, after = context.candidate.servers)
        entries.compare(entries = servers, kind = Kind.Servers, common = ::server)
        val channels = Both(before = context.baseline.channels, after = context.candidate.channels)
        entries.compare(entries = channels, kind = Kind.Channels, common = ::channel)
        OperationComparison(context).compare()
        return context.log.comparison()
    }

    private fun server(key: String, servers: Both<Server>) {
        val before = servers.before
        val after = servers.after
        val owners = Both<Element>(before = before, after = after)
        val label = "The server ${shown(key)}"
        val isMoved = before.protocol != after.protocol ||
            before.protocolVersion != after.protocolVersion ||
            before.host != after.host ||
            before.pathname != after.pathname
        if (isMoved) {
            val address = after.protocol + "://" + after.host + after.pathname.orEmpty()
            report(rule = "server-address-changed", message = "$label moves to ${shown(address)}", owners = owners)
        }
        val bindings = context.resolved(before = before.bindings, after = after.bindings)
        if (bindings.before != bindings.after) {
            report(
                rule = "server-bindings-changed",
                message = "The bindings of $label change",
                owners = owners,
            )
        }
        val requirements = Both(before = before.security, after = after.security)
        context.security(label = label, requirements = requirements, owners = owners)
    }

    private fun channel(key: String, channels: Both<Channel>) {
        val before = channels.before
        val after = channels.after
        val owners = Both<Element>(before = before, after = after)
        val label = "The channel ${shown(key)}"
        if (before.address != after.address) {
            val message = "$label moves to ${shown(after.address.orEmpty())}"
            report(rule = "channel-address-changed", message = message, owners = owners)
        }
        val bindings = context.resolved(before = before.bindings, after = after.bindings)
        if (bindings.before != bindings.after) {
            report(
                rule = "channel-bindings-changed",
                message = "The bindings of $label change",
                owners = owners,
            )
        }
    }

    private fun report(rule: String, message: String, owners: Both<Element>) {
        context.report(
            rule = rule,
            impacts = bothBreaking,
            message = message,
            before = owners.before,
            after = owners.after,
        )
    }

    enum class Kind(val word: String, val removal: Pair<Impact, Impact>, val addition: Pair<Impact, Impact>) {
        Servers(word = "server", removal = clientBreaking, addition = neitherBreaking),
        Channels(word = "channel", removal = clientBreaking, addition = neitherBreaking),
        Operations(word = "operation", removal = clientBreaking, addition = providerBreaking),
    }

    class Entries(val context: Context) {
        inline fun <reified T : Element> compare(
            entries: Both<Map<String, Referable<T>>>,
            kind: Kind,
            common: (String, Both<T>) -> Unit,
        ) {
            val old = entries.before
            val new = entries.after
            old.forEach { (key, referable) ->
                val before = context.baseline.resolve(referable)
                val found = new[key]
                val after = found?.let { candidate -> context.candidate.resolve(candidate) }
                if (found == null) removed(kind = kind, key = key, element = before)
                if (before != null && after != null) common(key, Both(before = before, after = after))
            }
            new.forEach { (key, referable) ->
                if (key !in old) added(kind = kind, key = key, element = context.candidate.resolve(referable))
            }
        }

        fun removed(kind: Kind, key: String, element: Element?) {
            context.report(
                rule = "${kind.word}-removed",
                impacts = kind.removal,
                message = "The ${kind.word} ${shown(key)} is removed",
                before = element,
                after = null,
            )
        }

        fun added(kind: Kind, key: String, element: Element?) {
            context.report(
                rule = "${kind.word}-added",
                impacts = kind.addition,
                message = "The ${kind.word} ${shown(key)} is added",
                before = null,
                after = element,
            )
        }
    }
}
