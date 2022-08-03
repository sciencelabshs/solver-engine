import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")

    antlr

    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
    id("org.jetbrains.dokka")
}

repositories {
    mavenCentral()
}

dependencies {
    antlr("org.antlr:antlr4:4.10.1")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.2")
    dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.7.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.dokkaHtml.configure {
    outputDirectory.set(file("../documentation/html"))
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"

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