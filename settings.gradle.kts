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

if (!JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_11)) {
  throw GradleException("Build requires Java 11")
}

pluginManagement {
  val versionIdeaExtPlugin = "1.1.7"
  val versionSpotlessPlugin = "6.13.0"

  // Cannot use a settings-script global variable/value, so pass the 'versions' Properties via
  // settings.extra around.
  val versions = java.util.Properties()
  settings.extra["nessieBuildTools.versions"] = versions

  plugins {
    id("com.gradle.plugin-publish") version "1.1.0"
    id("com.diffplug.spotless") version versionSpotlessPlugin
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("org.jetbrains.gradle.plugin.idea-ext") version versionIdeaExtPlugin

    versions["versionIdeaExtPlugin"] = versionIdeaExtPlugin
    versions["versionSpotlessPlugin"] = versionSpotlessPlugin
  }

  repositories {
    mavenCentral() // prefer Maven Central, in case Gradle's repo has issues
    gradlePluginPortal()
    if (java.lang.Boolean.getBoolean("withMavenLocal")) {
      mavenLocal()
    }
  }
}

gradle.rootProject {
  val prj = this
  val versions = settings.extra["nessieBuildTools.versions"] as java.util.Properties
  versions.forEach { k, v -> prj.extra[k.toString()] = v }
}

listOf(
    "errorprone",
    "checkstyle",
    "ide-integration",
    "publishing",
    "jandex",
    "jacoco",
    "reflection-config",
    "smallrye-openapi",
    "spotless",
    "dependency-declarations"
  )
  .forEach { include(it) }
