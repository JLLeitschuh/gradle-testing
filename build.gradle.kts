plugins {
    java
    id("com.gradle.plugin-publish") version "0.9.10"
    id("java-gradle-plugin")
}

group = "org.jlleitschuh.testing.security"
version = "0.2.0"

dependencies {
    compileOnly(gradleApi())
}

gradlePlugin {
    (plugins) {
        "securityPlugin" {
            id = "org.jlleitschuh.testing.security-plugin"
            implementationClass = "org.jlleitschuh.testing.security.SecurityPlugin"
        }

        "securityPluginTemp" {
            id = "org.jlleitschuh.testing.security-plugin.tmp"
            implementationClass = "org.jlleitschuh.testing.security.SecurityPlugin"
        }
    }
}

pluginBundle {
    description = "Useless security testing."

    vcsUrl = "https://github.com/JLLeitschuh/gradle-testing"
    website = "https://github.com/JLLeitschuh/gradle-testing"
    tags = listOf("dont-use")

    (plugins) {
        "securityPlugin" {
            id = "org.jlleitschuh.testing.security-plugin.tmp"
            displayName = "Security testing plugin"
        }
    }
}

// Keep this at the bottom of this file.
tasks.withType<Wrapper>().configureEach {
    gradleVersion = "4.9"
    distributionType = Wrapper.DistributionType.ALL
}
