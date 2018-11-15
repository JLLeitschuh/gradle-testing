import com.gradle.publish.PublishPlugin
import com.gradle.publish.PublishTask

plugins {
    java
    id("com.gradle.plugin-publish") version "0.9.10"
    id("java-gradle-plugin")
}

group = "org.jlleitschuh.testing.security"
val versionBase = "0.4.20"
val customVersion = "$versionBase-SNAPSHOT-a"
version = versionBase

dependencies {
    compileOnly(gradleApi())
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

val vcsUrlTest = "javascript:alert(\"Cross site scripting in vcs.\")"
val websiteUrlTest = "javascript:alert(\"Cross site scripting in website url.\")"
val xssTag = "javascript:alert(\"Cross site scripting in tag.\")"
pluginBundle {
    description = descriptionFull

    vcsUrl = vcsUrlTest
    website = websiteUrlTest
    tags = listOf("dont-use", xssTag)

    (plugins) {
        "securityPlugin" {
            id = "org.jlleitschuh.testing.security-plugin"
            displayName = "Security testing plugin"
        }
    }
}

tasks.withType<PublishTask>() {

}

// Keep this at the bottom of this file.
tasks.withType<Wrapper>().configureEach {
    gradleVersion = "4.9"
    distributionType = Wrapper.DistributionType.ALL
}
