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

package com.davils.kapi.spec.core.diagnostic

/**
 * Renders this diagnostic as its line, followed by the line of the source with the finding underlined when it has a
 * snippet.
 *
 * Text that came from a document is escaped, so the rendering is safe to print to a terminal or a log.
 *
 * @return the rendered diagnostic, one or three lines without a final line break.
 * @since 1.0.0
 */
public fun Diagnostic.render(): String = DiagnosticRendering.block(this)

/**
 * Renders these diagnostics one after the other, each as [Diagnostic.render] does.
 *
 * @return the rendered diagnostics separated by line breaks.
 * @since 1.0.0
 */
public fun Iterable<Diagnostic>.render(): String = joinToString("\n") { diagnostic -> diagnostic.render() }

/**
 * Whether one of these diagnostics is an error.
 *
 * @since 1.0.0
 */
public val Iterable<Diagnostic>.hasErrors: Boolean get() = any { diagnostic -> diagnostic.isError }
