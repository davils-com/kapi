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

package com.davils.kapi.spec.asyncapi

import com.davils.kapi.spec.asyncapi.model.message.Message
import com.davils.kapi.spec.asyncapi.model.operation.Operation
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.model.Reference

/**
 * Returns the object [reference] leads to, read from this document or another one.
 *
 * @param reference a reference of this document.
 * @return the object, or `null` if the reference could not be resolved, such as one that points at nothing.
 * @since 1.0.0
 */
public fun Document.resolve(reference: Reference): Element? = targets[reference.target]

/**
 * Returns the object of [referable]: the one written inline, or the one its reference leads to.
 *
 * @param T the type of the object.
 * @param referable a place of this document that holds an object or a reference to one.
 * @return the object, or `null` if the reference could not be resolved or leads to an object of another type.
 * @since 1.0.0
 */
public inline fun <reified T : Element> Document.resolve(referable: Referable<T>): T? = when (referable) {
    is Referable.Inline -> referable.value
    is Referable.Referenced -> resolve(referable.reference) as? T
}

/**
 * Returns [operation] with its traits applied, as AsyncAPI defines it: each trait is merged into the operation in
 * the order the traits are declared, a later trait over an earlier one, and a field the operation declares itself
 * always wins.
 *
 * The merge is shallow: a field is taken as a whole, from the operation or from the last trait that declares it, and
 * so are the extensions, by name. A trait whose reference cannot be resolved is left out. The result declares no
 * traits and keeps the location of [operation].
 *
 * @param operation an operation of this document.
 * @return the effective operation.
 * @since 1.0.0
 */
public fun Document.effective(operation: Operation): Operation {
    val traits = operation.traits.mapNotNull { trait -> resolve(trait) }
    return TraitMerge.operation(operation, traits)
}

/**
 * Returns [message] with its traits applied, as AsyncAPI defines it: each trait is merged into the message in the
 * order the traits are declared, a later trait over an earlier one, and a field the message declares itself always
 * wins.
 *
 * The merge is shallow: a field is taken as a whole, from the message or from the last trait that declares it, and
 * so are the extensions, by name. A trait whose reference cannot be resolved is left out. The result declares no
 * traits and keeps the location of [message].
 *
 * @param message a message of this document.
 * @return the effective message.
 * @since 1.0.0
 */
public fun Document.effective(message: Message): Message {
    val traits = message.traits.mapNotNull { trait -> resolve(trait) }
    return TraitMerge.message(message, traits)
}
