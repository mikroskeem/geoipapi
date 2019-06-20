import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("net.minecrell.licenser") version "0.4.1"
    id("com.github.johnrengelman.shadow") version "5.0.0"
}

extra["checkerQualVersion"] = "2.8.1"
extra["geoipVersion"] = "2.12.0"
extra["commonsCompressVersion"] = "1.18"
extra["expiringMapVersion"] = "0.5.9"
extra["slf4jApiVersion"] = "1.7.25"
extra["throwableFunctionVersion"] = "1.5.0"

// Platform versions
extra["paperApiVersion"] = "1.14.2-R0.1-SNAPSHOT"
extra["waterfallApiVersion"] = "1.14-SNAPSHOT"

allprojects {
    group = "eu.mikroskeem.cu"
    version = "0.0.1-SNAPSHOT"
    
    repositories {
        mavenLocal()
        mavenCentral()

        maven("https://papermc.io/repo/repository/maven-public/")
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "net.minecrell.licenser")

    license {
        header = rootProject.file("etc/HEADER")
        filter.include("**/*.java")
        filter.include("**/*.kt")
    }
}

dependencies {
    implementation(project(":bukkit"))
    implementation(project(":bungee"))
}

val shadowJar by tasks.getting(ShadowJar::class) {
    val target = "eu.mikroskeem.geoip.lib"
    val relocations = listOf(
            "com.fasterxml.jackson",
            "com.maxmind",
            "com.pivovarit.function",
            "net.jodah.expiringmap",
            "org.apache.commons.compress"
    )
    relocations.forEach {
        relocate(it, "$target.$it")
    }

    exclude("META-INF/maven/**")
    exclude("com/google/errorprone/**")
    exclude("org/checkerframework/**")

    exclude("org/apache/commons/compress/compressors/**") // No compressing needed
    exclude("org/apache/commons/compress/archivers/jar/**")
    exclude("org/apache/commons/compress/archivers/sevenz/**")
    exclude("org/apache/commons/compress/archivers/dump/**")
    exclude("org/apache/commons/compress/archivers/cpio/**")
    exclude("org/apache/commons/compress/archivers/ar/**")
    exclude("org/apache/commons/compress/archivers/arj/**")
}

tasks["build"].dependsOn(shadowJar)