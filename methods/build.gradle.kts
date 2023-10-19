import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask

val kotlinBenchmarkVersion: String by project

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")

    id("io.gitlab.arturbosch.detekt")
    id("com.google.devtools.ksp")

    // Plugins for benchmarking
    kotlin("plugin.allopen")
    id("org.jetbrains.kotlinx.benchmark")
}

dependencies {
    implementation(project(":engine"))
    implementation(project(":export"))
    implementation(project(":methodsProcessor"))

    testImplementation(kotlin("test"))
    testImplementation(testFixtures(project(":engine")))

    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.2")
    testImplementation("org.junit.platform:junit-platform-launcher:1.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    ksp(project(":export"))
    ksp(project(":methodsProcessor"))
}

tasks.test {
    useJUnitPlatform()
    outputs.file("../solver-poker/test-results-src/test-results.ts")
}

detekt {
    buildUponDefaultConfig = true
    config = files("$rootDir/config/detekt.yaml")
}

ktlint {
    filter {
        exclude {
            it.file.path.contains("$buildDir")
        }
    }
}

tasks.withType<KtLintCheckTask> {
    // It looks like gradle cannot figure out that ktlint doesn't depend on kspKotlin and complains if we don't
    // declare this dependency
    mustRunAfter("kspKotlin")
}

tasks.withType<KtLintFormatTask> {
    // It looks like gradle cannot figure out that ktlint doesn't depend on kspKotlin and complains if we don't
    // declare this dependency
    mustRunAfter("kspKotlin")
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
}

// This task depends on the results of the evaluation phase, as Gradle needs to complete
// checking which tasks are defined and configured before we can disable `kspTestKotlin`
// to not generate test directory from KSP
afterEvaluate {
    tasks.named("kspTestKotlin") {
        enabled = false
    }
}

//
// Configuration for benchmarks
//

// Benchmark requires benchmark classes to be open, this makes them open automatically
allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

// The benchmarks go in a new source set called "benchmarks"
sourceSets {
    create("benchmarks")
}

kotlin.sourceSets.named("benchmarks") {
    dependencies {
        // These are required to run benchmarks in the jvm
        implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:$kotlinBenchmarkVersion")
        implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime-jvm:$kotlinBenchmarkVersion")

        // These dependencies are for the code that is being benchmarked
        implementation(project(":engine"))
        implementation(sourceSets.main.get().output)
    }
}

benchmark {
    configurations {
        named("main") {
            mode = "avgt"
        }
    }
    targets {
        register("benchmarks")
    }
}
