/*
 * Copyright (C) 2022 Dremio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
  `kotlin-dsl`
  alias(libs.plugins.gradle.publish.plugin)
}

dependencies { implementation(libs.asm) }

gradlePlugin {
  plugins {
    create("reflectionconfig") {
      id = "org.projectnessie.buildsupport.reflectionconfig"
      displayName = "Reflection-config JSON generator"
      description = "Generates reflection-config.json configs for GraalVM native images"
      implementationClass = "org.projectnessie.buildtools.reflectionconfig.ReflectionConfigPlugin"
      tags.addAll("projectnessie", "graal", "native")
    }
  }
  vcsUrl.set("https://github.com/projectnessie/nessie/")
  website.set("https://github.com/projectnessie/nessie/")
}

kotlinDslPluginOptions { jvmTarget.set(JavaVersion.VERSION_11.toString()) }
