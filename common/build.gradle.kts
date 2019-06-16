dependencies {
    compile(project(":api"))
    implementation("org.slf4j:slf4j-api:${rootProject.extra["slf4jApiVersion"]}")
    implementation("com.maxmind.geoip2:geoip2:${rootProject.extra["geoipVersion"]}") {
        exclude(module = "httpcore")
        exclude(module = "httpclient")
    }
    implementation("org.apache.commons:commons-compress:${rootProject.extra["commonsCompressVersion"]}")
    implementation("net.jodah:expiringmap:${rootProject.extra["expiringMapVersion"]}")
}