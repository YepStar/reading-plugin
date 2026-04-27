plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.2.1"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

val publishingToken = providers.gradleProperty("intellijPlatformPublishingToken")
    .orElse(providers.environmentVariable("JETBRAINS_MARKETPLACE_TOKEN"))
    .orElse("")
val publishingChannels = providers.gradleProperty("pluginChannels")
    .map { channels -> channels.split(',').map(String::trim).filter(String::isNotBlank) }
    .orElse(listOf("default"))

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "com.reader.jetbrains"
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")
        vendor {
            name = "Reader"
        }
        ideaVersion {
            sinceBuild = "242"
        }
    }
    publishing {
        token = publishingToken
        channels = publishingChannels
        hidden = false
    }
}

dependencies {
    intellijPlatform {
        create(
            providers.gradleProperty("platformType"),
            providers.gradleProperty("platformVersion")
        )
        bundledPlugins(
            providers.gradleProperty("platformBundledPlugins")
                .map { it.split(',').map(String::trim).filter(String::isNotBlank) }
                .get()
        )
        pluginVerifier()
        zipSigner()
        instrumentationTools()
    }
}

tasks.processResources {
    from("../book-source-config") {
        into("book-source-config")
    }
}
