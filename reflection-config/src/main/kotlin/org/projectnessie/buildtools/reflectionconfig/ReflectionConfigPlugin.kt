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

package org.projectnessie.buildtools.reflectionconfig

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.provideDelegate

/** Generates `reflection-config.json` files from compiled classes. */
@Suppress("unused")
class ReflectionConfigPlugin : Plugin<Project> {
  override fun apply(project: Project): Unit =
    project.run {
      plugins.apply(JavaLibraryPlugin::class.java)

      extensions.create("reflectionConfig", ReflectionConfigExtension::class.java)

      configureFor(SourceSet.MAIN_SOURCE_SET_NAME, this)
      configureFor(SourceSet.TEST_SOURCE_SET_NAME, this)
    }

  private fun configureFor(sourceSetName: String, project: Project) =
    project.run {
      extensions.getByType(SourceSetContainer::class.java).named(sourceSetName) {
        val dirName = project.buildDir.resolve("generated/resource/reflect-conf/$sourceSetName")
        resources.srcDir(dirName)

        val e = project.extensions.getByType(ReflectionConfigExtension::class.java)
        val compileJava = tasks.named(compileJavaTaskName, JavaCompile::class.java)

        val allFiles =
          project.objects
            .fileCollection()
            .from(
              e.includeConfigurations.flatMap { configNames ->
                provider {
                  configNames
                    .map { c ->
                      val config = project.configurations.getByName(c)

                      val dependencies: DependencySet = config.dependencies
                      config.fileCollection(*dependencies.toTypedArray())
                    }
                    .reduce { x, y -> x.plus(y) }
                }
              }
            )

        val genRefCfg =
          tasks.register(
            getTaskName("generate", "reflectionConfig"),
            ReflectionConfigTask::class.java,
            e.classExtendsPatterns,
            e.classImplementsPatterns,
            allFiles,
            e.relocations
          )
        genRefCfg.configure {
          setName.set(sourceSetName)
          classesFolder.set(compileJava.get().destinationDirectory)
          outputDirectory.set(file(dirName))

          projectGroup.convention(provider { project.group.toString() })
          projectName.convention(provider { project.name })
          projectVersion.convention(provider { project.version.toString() })

          dependsOn(compileJava)
        }
        tasks.named(processResourcesTaskName) { dependsOn(genRefCfg) }
        try {
          tasks.named(sourcesJarTaskName) { dependsOn(genRefCfg) }
        } catch (ignore: UnknownTaskException) {
          // ignore
        }
      }
    }
}
