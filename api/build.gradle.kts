plugins {
    `java-library`
}

dependencies {
    api("org.checkerframework:checker-qual:${rootProject.extra["checkerQualVersion"]}")
}