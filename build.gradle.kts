
plugins {
    java
}

group = "vn.nexguard"
version = "0.1.0-alpha"

repositories {
    mavenCentral()

    maven {
        name = "PaperMC"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

    maven {
        name = "CodeMC"
        url = uri("https://repo.codemc.io/repository/maven-releases/")
    }

    maven {
        name = "CodeMCSnapshots"
        url = uri("https://repo.codemc.io/repository/maven-snapshots/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    compileOnly("com.github.retrooper:packetevents-spigot:2.14.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    filteringCharset = "UTF-8"
}
