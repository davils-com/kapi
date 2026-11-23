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

package com.davils.kapi.spec.schema.internal.compile

import com.davils.kapi.spec.schema.DiagnosticCodes
import com.davils.kapi.spec.schema.internal.Uris
import com.davils.kapi.spec.schema.internal.dialect.Draft
import com.davils.kapi.spec.schema.internal.dialect.Profile
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.violation.ViolationCode
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal class Scanner(private val session: Session, private val document: Document) {
    fun scan(element: JsonElement, pointer: List<String>, resource: Int, profile: Profile, isRoot: Boolean) {
        if (pointer.size > session.limits.maximumSchemaDepth) {
            val message = "The schema is nested deeper than ${session.limits.maximumSchemaDepth} levels"
            session.abort(DiagnosticCodes.LIMIT_EXCEEDED, message, Target(document.index, pointer))
        }
        val target = Target(document.index, pointer)
        val members = element as? JsonObject
        if (members == null) {
            session.record(target, resource, profile)
            return
        }
        val effective = profileOf(members, profile, pointer, isRoot)
        val own = identified(members, target, resource, effective)
        session.record(target, own, effective)
        anchors(members, target, own, effective)
        references(members, own, effective)
        children(members, pointer, own, effective)
    }

    private fun profileOf(members: JsonObject, profile: Profile, pointer: List<String>, isRoot: Boolean): Profile {
        val hasId = profile.draft.idKeyword?.let { it in members } ?: false
        val applies = isRoot || (profile.draft.hasVocabularies && hasId)
        val declared = textOf(members["\$schema"])?.takeIf { applies } ?: return profile
        return session.profileFor(declared) ?: throw MetaSchemaNeeded(declared, pointer + "\$schema")
    }

    private fun identified(members: JsonObject, target: Target, resource: Int, profile: Profile): Int {
        val id = idOf(members, profile) ?: return resource
        return identifiedBy(id, target, resource, profile)
    }

    private fun idOf(members: JsonObject, profile: Profile): String? {
        val keyword = profile.draft.idKeyword ?: return null
        val isIgnored = profile.draft.refOverridesSiblings && "\$ref" in members
        return textOf(members[keyword])?.takeIf { !isIgnored }
    }

    private fun identifiedBy(id: String, target: Target, resource: Int, profile: Profile): Int {
        val base = session.resources[resource].uri
        val absolute = Uris.resolve(id, base) ?: return invalidId(id, target, resource)
        val uri = Uris.withoutFragment(absolute)
        val fragment = Uris.fragmentOf(absolute)
        val anchorsInId = profile.draft.anchorsInId
        if (fragment.isNotEmpty() && !anchorsInId) {
            report("The identifier ${SafeText.quoted(id)} must not have a fragment", target, DiagnosticCodes.ID_INVALID)
        }
        val isAnchorOnly = anchorsInId && fragment.isNotEmpty() && uri == base
        val own = resource.takeIf { isAnchorOnly } ?: session.resourceFor(uri, target)
        if (anchorsInId && fragment.isNotEmpty()) session.addAnchor(own, fragment, target)
        return own
    }

    private fun invalidId(id: String, target: Target, resource: Int): Int {
        report("The identifier ${SafeText.quoted(id)} is no URI reference", target, DiagnosticCodes.ID_INVALID)
        return resource
    }

    private fun anchors(members: JsonObject, target: Target, resource: Int, profile: Profile) {
        val draft = profile.draft
        if (!draft.hasVocabularies) return
        textOf(members["\$anchor"])?.let { name -> anchor(name, target, resource, draft) }
        val dynamic = textOf(members["\$dynamicAnchor"])?.takeIf { draft == Draft.Draft202012 }
        dynamic?.let { name ->
            anchor(name, target, resource, draft)
            session.resources[resource].dynamicAnchors[name] = target.pointer
        }
        val isRecursive = (members["\$recursiveAnchor"] as? JsonPrimitive)?.content == "true"
        val isRoot = session.resources[resource].pointer == target.pointer
        if (draft == Draft.Draft201909 && isRecursive && isRoot) session.resources[resource].recursiveAnchor = true
    }

    private fun anchor(name: String, target: Target, resource: Int, draft: Draft) {
        val isValid = Anchors.isValid(name, draft)
        if (!isValid) {
            report(
                "The anchor ${SafeText.quoted(name)} is no valid name",
                target,
                DiagnosticCodes.ANCHOR_INVALID,
            )
        }
        if (isValid) session.addAnchor(resource, name, target)
    }

    private fun references(members: JsonObject, resource: Int, profile: Profile) {
        val base = session.resources[resource].uri
        val keywords = listOf("\$ref", "\$dynamicRef", "\$recursiveRef")
        keywords.forEach { keyword ->
            val value = textOf(members[keyword])
            val absolute = value?.let { Uris.resolve(it, base) }
            absolute?.let { session.references += PendingReference(Uris.withoutFragment(it), profile) }
        }
    }

    private fun children(members: JsonObject, pointer: List<String>, resource: Int, profile: Profile) {
        members.forEach { (name, value) ->
            val shape = KeywordRules.ruleOf(name, profile)?.shape ?: Shape.None
            child(shape, value, pointer + name, resource, profile)
        }
    }

    private fun child(shape: Shape, value: JsonElement, pointer: List<String>, resource: Int, profile: Profile) {
        val isList = value is JsonArray
        when (shape) {
            Shape.None -> Unit
            Shape.Single -> scan(value, pointer, resource, profile, isRoot = false)
            Shape.List -> listed(value, pointer, resource, profile)
            Shape.Map -> mapped(value, pointer, resource, profile, onlySchemas = false)
            Shape.SingleOrList -> child(Shape.List.takeIf { isList } ?: Shape.Single, value, pointer, resource, profile)
            Shape.Dependencies -> mapped(value, pointer, resource, profile, onlySchemas = true)
        }
    }

    private fun listed(value: JsonElement, pointer: List<String>, resource: Int, profile: Profile) {
        val items = value as? JsonArray ?: return
        items.forEachIndexed { index, item ->
            scan(
                item,
                pointer + index.toString(),
                resource,
                profile,
                isRoot = false,
            )
        }
    }

    private fun mapped(
        value: JsonElement,
        pointer: List<String>,
        resource: Int,
        profile: Profile,
        onlySchemas: Boolean,
    ) {
        val members = value as? JsonObject ?: return
        members.forEach { (key, item) ->
            val isSkipped = onlySchemas && item is JsonArray
            if (!isSkipped) scan(item, pointer + key, resource, profile, isRoot = false)
        }
    }

    private fun report(message: String, target: Target, code: ViolationCode) {
        if (session.isStrict) session.report(code, message, target)
    }

    private fun textOf(element: JsonElement?): String? = (element as? JsonPrimitive)?.takeIf { it.isString }?.content
}
