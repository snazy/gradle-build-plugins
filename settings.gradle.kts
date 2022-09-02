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
  // Cannot use a settings-script global variable/value, so pass the 'versions' Properties via
  // settings.extra around.
  val versions = java.util.Properties()
  val pluginIdPattern =
    java.util.regex.Pattern.compile("\\s*id\\(\"([^\"]+)\"\\) version \"([^\"]+)\"\\s*")
  settings.extra["nessieBuildTools.versions"] = versions

  plugins {

    // Note: this is NOT a real project but a hack for dependabot to manage the plugin versions.
    //
    // Background: dependabot only manages dependencies (incl Gradle plugins) in build.gradle[.kts]
    // files. It scans the root build.gradle[.kts] fila and those in submodules referenced in
    // settings.gradle[.kts].
    // But dependabot does not manage managed plugin dependencies in settings.gradle[.kts].
    // However, since dependabot is a "dumb search and replace engine", we can use a trick:
    // 1. Have this "dummy" build.gradle.kts file with all managed plugin dependencies.
    // 2. Add an `include()` to this build file in settings.gradle.kts, surrounded with an
    //    `if (false)`, so Gradle does _not_ pick it up.
    // 3. Parse this file in our settings.gradle.kts, provide a `ResolutionStrategy` to the
    //    plugin dependencies.

    val pulledVersions =
      file("gradle/dependabot/build.gradle.kts")
        .readLines()
        .map { line -> pluginIdPattern.matcher(line) }
        .filter { matcher -> matcher.matches() }
        .associate { matcher -> matcher.group(1) to matcher.group(2) }

    resolutionStrategy {
      eachPlugin {
        if (requested.version == null) {
          val pluginId = requested.id.id
          if (pulledVersions.containsKey(pluginId)) {
            useVersion(pulledVersions[pluginId])
          }
        }
      }
    }

    versions["versionIdeaExtPlugin"] = pulledVersions["org.jetbrains.gradle.plugin.idea-ext"]
    versions["versionSpotlessPlugin"] = pulledVersions["com.diffplug.spotless"]
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
    "protobuf",
    "reflection-config",
    "smallrye-openapi",
    "spotless",
    "dependency-declarations"
  )
  .forEach { include(it) }

if (false) {
  include("gradle:dependabot")
}
