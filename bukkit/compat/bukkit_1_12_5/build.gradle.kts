repositories {
    mavenLocal()
    mavenCentral()

    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    implementation("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT") {
        isTransitive = false
    }
}

tasks.shadowJar {
    include("org/bukkit/inventory/**")
}