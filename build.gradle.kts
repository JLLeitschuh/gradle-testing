plugins {
    java
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "0.12.0"
}

version = "0.4.45"

dependencies {
    implementation(gradleApi())
}

gradlePlugin {
    (plugins) {
        create("securityPlugin") {
            id = "org.jlleitschuh.testing.security-plugin"
            implementationClass = "org.jlleitschuh.testing.security.SecurityPlugin"
        }
    }
}

pluginBundle {
    description = "Some much longer description"

    vcsUrl = "https://github.com/jlleitschuh/gradle-testing"
    website = "https://github.com/jlleitschuh/gradle-testing"
    tags = listOf("dont-use", "really-dont-use", "gradle", "plugin")

    (plugins) {
        "securityPlugin" {
            id = "org.jlleitschuh.testing.security-plugin"
            displayName = "Security testing plugin"
        }
    }
}

// Keep this at the bottom of this file.
tasks.withType<Wrapper>().configureEach {
    gradleVersion = "6.7.1"
}
