plugins {
    id("java-gradle-plugin")
}

repositories {
    gradlePluginPortal()
}

dependencies {
    compile("com.gradle.plugin-publish:com.gradle.plugin-publish.gradle.plugin:0.9.10")
}

gradlePlugin {
    (plugins) {
        "org.jlleitschuh.plugin-publish" {
            id = "org.jlleitschuh.plugin-publish"
            implementationClass = "com.gradle.publish.MyPublishPlugin"
        }
    }
}
