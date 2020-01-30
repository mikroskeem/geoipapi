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
        if (rootProject.hasProperty("wutee.repository.deploy.username") && rootProject.hasProperty("wutee.repository.deploy.password")) {
            maven("https://repo.wut.ee/repository/mikroskeem-repo") {
                credentials {
                    username = rootProject.property("wutee.repository.deploy.username") as String
                    password = rootProject.property("wutee.repository.deploy.password") as String
                }
            }
        }
        mavenLocal()
    }
}
