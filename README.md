# Gradle plugins for projectnessie builds

These Gradle plugins are used to build [Nessie](https://github.com/projectnessie/nessie) and
[CEL](https://github.com/projectnessie/cel-java).

## Developing / Testing

There are two ways to develop and test these plugins.

### Via local Maven repository

1. Publish the plugins to your local Maven repo using `./gradlew publishToMavenLocal`
2. Update the version of the Nessie build plugins to the current SNAPSHOT version in the
   "target" project.
3. Run the "target" project build with `-DwithMavenLocal=true`

### By including this project into your target build

1. Update the top-level `settings.gradle.kts` file of the "target" project, add a line like
   ```kotlin
   includeBuild("../../gradle-build-plugins")
   ```

(This approach hasn't been tested yet.)
