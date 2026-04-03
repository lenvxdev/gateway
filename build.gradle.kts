import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    kotlin("jvm") version "2.1.10"
    id("com.gradleup.shadow") version "9.4.0"
}

group = "dev.lenvx"
version = "1.0.1"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

val buildNumber = System.getenv("BUILD_NUMBER")?.let { "-b$it" } ?: ""
val fullVersion = "${project.version}$buildNumber"

dependencies {
    implementation("org.apache.commons:commons-lang3:3.18.0")
    implementation("com.github.Querz:NBT:6.1")
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("org.yaml:snakeyaml:2.4")
    implementation("com.googlecode.json-simple:json-simple:1.1.1") {
        exclude(group = "junit", module = "junit")
    }
    implementation("net.md-5:bungeecord-chat:1.21-R0.3")
    implementation("net.md-5:bungeecord-serializer:1.21-R0.3")
    implementation("net.kyori:adventure-text-serializer-gson:4.25.0")
    implementation("net.kyori:adventure-text-serializer-legacy:4.25.0")
    implementation("net.kyori:adventure-text-serializer-plain:4.25.0")
    implementation("net.kyori:adventure-text-minimessage:4.25.0")
    implementation("net.kyori:adventure-api:4.25.0")
    implementation("net.kyori:adventure-nbt:4.25.0")
    implementation("org.fusesource.jansi:jansi:2.4.2")
    implementation("org.jline:jline:3.30.5")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
    withJavadocJar()
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    withType<Javadoc> {
        options.encoding = "UTF-8"
        (options as StandardJavadocDocletOptions).apply {
            docTitle = "Gateway JavaDocs"
            windowTitle = "Gateway JavaDocs"
            addStringOption("Xdoclint:none", "-quiet")
        }
    }

    processResources {
        val tokens = mapOf(
            "project.version" to project.version,
            "project.fullVersion" to fullVersion
        )
        inputs.properties(tokens)
        
        filesMatching(listOf("**/*.txt", "**/*.yml", "**/*.json", "**/*.properties")) {
            filter<ReplaceTokens>("tokens" to tokens)
        }
    }

    shadowJar {
        archiveBaseName.set("Gateway")
        archiveClassifier.set("")
        archiveVersion.set("${project.version}-1.21.11")
        
        manifest {
            attributes(
                "Main-Class" to "dev.lenvx.gateway.Gateway",
                "Gateway-Version" to project.version,
                "Implementation-Title" to "Gateway",
                "Implementation-Vendor" to "lenvxdev",
                "Implementation-URL" to "https://lenvx.dev",
                "Implementation-Description" to "Gateway is a one-to-one fallback server rewrite focused on stability, compatibility, and lightweight performance."
            )
        }
    }

    build {
        dependsOn(shadowJar)
    }

    register("qualityGate") {
        group = "verification"
        description = "Runs build and test verification."
        dependsOn("build")
    }

    register("releaseGate") {
        group = "verification"
        description = "Strict release verification gate."
        dependsOn("clean", "qualityGate")
        doLast {
            if (project.version.toString().contains("ALPHA", ignoreCase = true)) {
                throw GradleException("Release gate failed: project.version still contains ALPHA")
            }
            if (project.version.toString().contains("SNAPSHOT", ignoreCase = true)) {
                throw GradleException("Release gate failed: project.version still contains SNAPSHOT")
            }
        }
    }
}

