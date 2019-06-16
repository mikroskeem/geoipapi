plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api("org.checkerframework:checker-qual:${rootProject.extra["checkerQualVersion"]}")
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allJava)
}

val javadocJar by tasks.creating(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "eu.mikroskeem.geoipapi"
            artifactId = "api"

            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)
        }
    }

    repositories {
        mavenLocal()
    }
}