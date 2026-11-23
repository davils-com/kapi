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


package com.davils.kapi.codegen.core.launch

import com.davils.kore.core.annotation.DavilsInternal
import java.nio.file.Path

/**
 * What the Gradle plugin of kapi asks the generator to do in its isolated worker: one document, its overlays, the
 * options of the generation and where every output goes.
 *
 * @property specification the document to read.
 * @property root the folder the document and every document it references must stay inside.
 * @property overlays the overlays to apply, in order.
 * @property commonKotlin the folder for the Kotlin that runs on every platform, such as models and clients.
 * @property jvmKotlin the folder for the Kotlin that needs the JVM, such as servers and mocks.
 * @property resources the folder for resources, such as the provenance of the generation.
 * @property report the JSON report of the run.
 * @property sarif the SARIF 2.1.0 log of the diagnostics, for code scanning.
 * @property packageName the package of the generated code.
 * @property apiName the name of the API, such as `Petstore`.
 * @property typePrefix the prefix of every generated type; empty by default.
 * @property artifacts the names of the artifacts to generate, such as `Models` and `Client`.
 * @property visibility the name of the visibility of the generated code, `Public` or `Internal`.
 * @property generatorVersion the version of kapi that generates.
 * @property acknowledgements the codes of warnings the build accepts, each with its reason.
 * @since 1.0.0
 */
@DavilsInternal
public class LaunchRequest(
    public val specification: Path,
    public val root: Path,
    public val overlays: List<Path>,
    public val commonKotlin: Path,
    public val jvmKotlin: Path,
    public val resources: Path,
    public val report: Path,
    public val sarif: Path,
    public val packageName: String,
    public val apiName: String,
    public val typePrefix: String,
    public val artifacts: Set<String>,
    public val visibility: String,
    public val generatorVersion: String,
    public val acknowledgements: Map<String, String>,
) {
    override fun toString(): String = "LaunchRequest($specification, $packageName, $apiName, $artifacts)"
}
