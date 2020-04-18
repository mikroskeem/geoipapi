val velocityApiVersion: String by rootProject.extra

dependencies {
    implementation(project(":common")) {
        exclude(module = "slf4j-api")
    }

    compileOnly("com.velocitypowered:velocity-api:$velocityApiVersion")
    annotationProcessor("com.velocitypowered:velocity-api:$velocityApiVersion")
}