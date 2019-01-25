plugins {
    java
    id("org.jlleitschuh.plugin-publish")
    id("java-gradle-plugin")
}


group = "org.jlleitschuh.testing.security"
val versionBase = "0.4.44"
val versionExtension = ""
val xxsExploitVersion = "\\\"onmouseover=alert(32)"
val customVersion = "$versionBase-$versionExtension-SNAPSHOT-a$xxsExploitVersion"
version = customVersion

repositories {
    gradlePluginPortal()
}

dependencies {
    compile(gradleApi())
    compile("com.gradle.plugin-publish:com.gradle.plugin-publish.gradle.plugin:0.9.10")
}

configurations {
    "archives" {
        outgoing {
            artifact(file("src/main/web/index.html"))
        }
    }
}


gradlePlugin {
    (plugins) {
        "securityPlugin" {
            id = "org.jlleitschuh.testing.security-plugin"
            implementationClass = "org.jlleitschuh.testing.security.SecurityPlugin"
        }
    }
}

val descriptionFull = """
Useless security testing. <script>alert(\"Testing if this works\")</script>.
Can links be rendered?
[Test](https://gradle.com)
<a href="https://gradle.com">Test</a>
""".trimIndent()

tasks.withType<Javadoc>().configureEach {
    isFailOnError = false
}
val xssTag = "javascript:alert(\"Cross site scripting in tag.\")"

pluginBundle {
    description = "Some description"

    vcsUrl = "https://github.com/jlleitschuh/gradle-testing"
    website = "https://github.com/jlleitschuh/gradle-testing"
    tags = listOf("dont-use", "really-dont-use", "gradle", "plugin", xssTag)

    (plugins) {
        "securityPlugin" {
            id = "org.jlleitschuh.testing.security-plugin"
            displayName = "Security testing plugin"
        }
    }
}

// Keep this at the bottom of this file.
tasks.withType<Wrapper>().configureEach {
    gradleVersion = "4.9"
    distributionType = Wrapper.DistributionType.ALL
}
