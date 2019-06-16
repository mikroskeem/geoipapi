plugins {
    id("net.minecrell.plugin-yml.bungee") version "0.3.0"
}

dependencies {
    implementation(project(":common")) {
        exclude(module = "slf4j-api")
    }
    
    compileOnly("io.github.waterfallmc:waterfall-api:${rootProject.extra["waterfallApiVersion"]}")
}

bungee {
    name = "GeoIPAPI"
    description = "Provides GeoIP database access to plugins"
    main = "eu.mikroskeem.geoip.bungee.GeoIPAPIPlugin"
    author = "${listOf("mikroskeem")}"
}