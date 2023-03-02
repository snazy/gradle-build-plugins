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

import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin

plugins {
  alias(libs.plugins.spotless)
  alias(libs.plugins.gradle.nexus.publish)
}

val projectVersion = file("version.txt").readText().trim()

allprojects {
  group = "org.projectnessie.buildsupport"
  version = projectVersion

  repositories {
    gradlePluginPortal()
    mavenCentral()
    if (java.lang.Boolean.getBoolean("withMavenLocal")) {
      mavenLocal()
    }
  }

  apply<MavenPublishPlugin>()

  tasks.withType<JavaCompile>().configureEach {
    targetCompatibility = JavaVersion.VERSION_11.toString()
  }

  apply<SpotlessPlugin>()
  plugins.withType<SpotlessPlugin>().configureEach {
    configure<SpotlessExtension> {
      kotlinGradle {
        ktfmt().googleStyle()
        licenseHeaderFile(rootProject.file("codestyle/copyright-header-java.txt"), "$")
      }
      if (project != rootProject) {
        kotlin {
          ktfmt().googleStyle()
          licenseHeaderFile(rootProject.file("codestyle/copyright-header-java.txt"), "$")
        }
      }
    }
  }

  if (project.hasProperty("release")) {
    apply<SigningPlugin>()
    plugins.withType<SigningPlugin>().configureEach {
      configure<SigningExtension> {
        val signingKey: String? by project
        val signingPassword: String? by project
        useInMemoryPgpKeys(signingKey, signingPassword)
      }
    }
  }

  configure<PublishingExtension> {
    publications {
      withType(MavenPublication::class.java).configureEach {
        val mavenPublication = this

        if (project.hasProperty("release")) {
          if (
            mavenPublication.name != "pluginMaven" &&
              !mavenPublication.name.endsWith("PluginMarkerMaven")
          ) {
            System.err.println("$project   ${mavenPublication.name}")
            configure<SigningExtension> { sign(mavenPublication) }
          }
        }

        pom {
          val nessieRepoName = "gradle-build-plugins"

          if (mavenPublication.name == "pluginMaven") {
            val pluginBundle = project.extensions.getByType<GradlePluginDevelopmentExtension>()
            name.set(project.name)
            pluginBundle.plugins.stream().findFirst().ifPresent { plugin ->
              description.set(plugin.description)
            }
          }

          inceptionYear.set("2022")
          url.set("https://github.com/projectnessie/$nessieRepoName")
          organization {
            name.set("Project Nessie")
            url.set("https://projectnessie.org")
          }
          licenses {
            license {
              name.set("The Apache License, Version 2.0")
              url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
          }
          mailingLists {
            mailingList {
              name.set("Project Nessie List")
              subscribe.set("projectnessie-subscribe@googlegroups.com")
              unsubscribe.set("projectnessie-unsubscribe@googlegroups.com")
              post.set("projectnessie@googlegroups.com")
              archive.set("https://groups.google.com/g/projectnessie")
            }
          }
          scm {
            connection.set("scm:git:https://github.com/projectnessie/$nessieRepoName")
            developerConnection.set("scm:git:https://github.com/projectnessie/$nessieRepoName")
            url.set("https://github.com/projectnessie/$nessieRepoName/tree/main")
            tag.set("main")
          }
          issueManagement {
            system.set("Github")
            url.set("https://github.com/projectnessie/$nessieRepoName/issues")
          }
          developers {
            file(rootProject.file("gradle/developers.csv"))
              .readLines()
              .map { line -> line.trim() }
              .filter { line -> line.isNotEmpty() && !line.startsWith("#") }
              .forEach { line ->
                val args = line.split(",")
                if (args.size < 3) {
                  throw GradleException("gradle/developers.csv contains invalid line '${line}'")
                }
                developer {
                  id.set(args[0])
                  name.set(args[1])
                  url.set(args[2])
                }
              }
          }
          contributors {
            file(rootProject.file("gradle/contributors.csv"))
              .readLines()
              .map { line -> line.trim() }
              .filter { line -> line.isNotEmpty() && !line.startsWith("#") }
              .forEach { line ->
                val args = line.split(",")
                if (args.size > 2) {
                  throw GradleException("gradle/contributors.csv contains invalid line '${line}'")
                }
                contributor {
                  name.set(args[0])
                  url.set(args[1])
                }
              }
          }
        }
      }
    }
  }
}

tasks.named<Wrapper>("wrapper") { distributionType = Wrapper.DistributionType.ALL }

// Pass environment variables:
//    ORG_GRADLE_PROJECT_sonatypeUsername
//    ORG_GRADLE_PROJECT_sonatypePassword
// OR in ~/.gradle/gradle.properties set
//    sonatypeUsername
//    sonatypePassword
// Call targets:
//    publishToSonatype
//    closeAndReleaseSonatypeStagingRepository
nexusPublishing {
  transitionCheckOptions {
    // default==60 (10 minutes), wait up to 60 minutes
    maxRetries.set(360)
    // default 10s
    delayBetween.set(java.time.Duration.ofSeconds(10))
  }
  repositories { sonatype() }
}
