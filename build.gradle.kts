plugins {
    kotlin("jvm") version "1.2.71"
}

repositories {
    mavenCentral()
    maven {
        setUrl("https://dl.bintray.com/kotlin/kotlinx/")
    }
    maven {
        setUrl("http://dl.bintray.com/kotlin/ktor")
    }
    jcenter()
}

dependencies {
    compile(group = "org.jetbrains.kotlin", name = "kotlin-stdlib")
    compile(ktor("ktor-client-core"))
    compile(ktor("ktor-client-apache"))
    compile(ktor("ktor-client-json"))
    compile(group = "com.beust", name = "klaxon", version = "3.0.8")
}

fun DependencyHandler.ktor(name: String) =
        create(group = "io.ktor", name = name, version = "0.9.3")

// Keep this at the bottom of this file.
tasks.withType<Wrapper>().configureEach {
    gradleVersion = "4.10.2"
    distributionType = Wrapper.DistributionType.ALL
}
