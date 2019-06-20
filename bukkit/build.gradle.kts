import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    id("net.minecrell.plugin-yml.bukkit") version "0.3.0"
}

dependencies {
    implementation(project(":common")) {
        exclude(module = "slf4j-api")
    }

    compileOnly("com.destroystokyo.paper:paper-api:${rootProject.extra["paperApiVersion"]}")
}

bukkit {
    name = "GeoIPAPI"
    description = "Provides GeoIP database access to plugins"
    main = "eu.mikroskeem.geoip.bukkit.GeoIPAPIPlugin"
    authors = listOf("mikroskeem")
    website = "https://mikroskeem.eu"

    commands {
        create("geoiplookup") {
            permission = "geoipapi.geoiplookup"
        }
    }

    permissions {
        create("geoipapi.geoiplookup") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
    }
}