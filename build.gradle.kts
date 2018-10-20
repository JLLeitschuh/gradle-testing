buildscript {
    repositories {
        maven {
            setUrl("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("gradle.plugin.org.jlleitschuh.testing.security:gradle-testing:0.4.+")
    }
}
apply(plugin = "org.jlleitschuh.testing.security-plugin")

// Keep this at the bottom of this file.
tasks.withType<Wrapper>().configureEach {
    gradleVersion = "4.9"
    distributionType = Wrapper.DistributionType.ALL
}
