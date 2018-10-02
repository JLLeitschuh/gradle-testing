import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3-M2"
}

repositories {
    mavenCentral()
    maven {
        setUrl("http://dl.bintray.com/kotlin/kotlin-eap")
    }
}

dependencies {
    compile(group = "org.jetbrains.kotlin", name = "kotlin-stdlib")
    compile(group = "com.google.code.findbugs", name = "jsr305", version = "3.0.0")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
    }
}

// Keep this at the bottom of this file.
tasks.withType<Wrapper>().configureEach {
    gradleVersion = "4.10.2"
    distributionType = Wrapper.DistributionType.ALL
}
