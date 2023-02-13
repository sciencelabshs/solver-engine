import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    antlr

    id("io.gitlab.arturbosch.detekt")
    id("java-test-fixtures")
    id("com.google.devtools.ksp")
}

dependencies {
    antlr("org.antlr:antlr4:4.10.1")

    testFixturesImplementation(kotlin("test"))
    testImplementation(kotlin("test"))
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-params:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.2")

    ksp(project(":export"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.withType<KotlinCompile> {
    // The antlr plugin defines a dependency for java compilation but not Kotlin, so we do it manually here.
    dependsOn(tasks.generateGrammarSource)
}

tasks.generateGrammarSource {
    arguments = arguments + listOf("-visitor")
}

detekt {
    buildUponDefaultConfig = true
    config = files("$rootDir/config/detekt.yaml")
}
