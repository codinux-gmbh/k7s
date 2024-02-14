pluginManagement {
    val kotlinVersion: String by settings
    val quarkusVersion: String by settings

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.allopen") version kotlinVersion

        id("io.quarkus") version quarkusVersion
    }
}


rootProject.name = "k7s"