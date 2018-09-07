/**
 * Some example task that has two outputs.
 */
@Suppress("LeakingThis")
open class OutputTest : DefaultTask() {

    @get:OutputFile
    val outputFile: RegularFileProperty = newOutputFile()

    @get:OutputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val outputDirectory: DirectoryProperty = newOutputDirectory()
}

/**
 * Some example task that has two inputs.
 */
@Suppress("LeakingThis")
open class InputTest : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val inputDir: DirectoryProperty = newInputDirectory()

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val inputFile: RegularFileProperty = newInputFile()
}

val out1 =
    tasks.register<OutputTest>("output1")

val dependencyWorks1 =
    tasks.register<InputTest>("dependencyWorks1") {
        inputFile.set(out1.map { it.outputFile }.get())
    }
/*
./gradlew --dry-run dependencyWorks1
:output1 SKIPPED
:dependencyWorks1 SKIPPED

BUILD SUCCESSFUL in 0s
 */

val dependencyWorks2 =
    tasks.register<InputTest>("dependencyWorks2") {
        inputDir.set(out1.map { it.outputDirectory }.get())
    }
/*
./gradlew --dry-run dependencyWorks2
:output1 SKIPPED
:dependencyWorks2 SKIPPED
 */

val dependencyFails1 =
    tasks.register<InputTest>("dependencyFails1") {
        // Notice the subtle shift in where the `get` is here.
        inputDir.set(out1.map { it.outputDirectory.get() })
    }
/*
./gradlew --dry-run dependencyFails1
:dependencyFails1 SKIPPED

BUILD SUCCESSFUL in 0s
 */

val dependencyFails2 =
    tasks.register<InputTest>("dependencyFails2") {
        // Calling `file` on a `DirectoryProperty` does not retain the dependency
        inputFile.set(
            out1
                .map { it.outputDirectory.file("someFile.txt") }
                .get()
        )
    }
/*
./gradlew --dry-run dependencyFails2
:dependencyFails2 SKIPPED

BUILD SUCCESSFUL in 0s
 */

val dependencyFails3 =
    tasks.register<InputTest>("dependencyFails3") {
        inputDir.set(
            out1
                .map { it.outputDirectory.dir("/someDir") }
                .get()
        )
    }
/*
./gradlew --dry-run dependencyFails3
:dependencyFails3 SKIPPED

BUILD SUCCESSFUL in 0s
 */

inline fun <reified T : Task> TaskContainer.register(name: String, noinline configure: T.() -> Unit = {}) =
    register(name, T::class.java, configure)

// Keep this at the bottom of this file.
tasks.withType<Wrapper>().configureEach {
    gradleVersion = "4.9"
    distributionType = Wrapper.DistributionType.ALL
}
