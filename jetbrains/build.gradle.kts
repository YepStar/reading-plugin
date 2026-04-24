plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.2.1"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

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
