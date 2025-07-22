plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.17"
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

dependencies {
    val serverVer = rootProject.property("targetMinecraftVersion").toString()
    paperweight.paperDevBundle("$serverVer-R0.1-SNAPSHOT")
    subprojects.filter {
        it.parent == project
    }.forEach {
        api(project(it.path, configuration = "shadow"))
    }
}

//tasks.shadowJar {
//    subprojects.filter {
//        it.parent == project
//    }.forEach {
//        from(it.components["shadow"])
////        from(it.sourceSets.main.get().output)
////        api(project(it.path, configuration = "shadow"))
//    }
//}


subprojects {
    apply(plugin = "io.papermc.paperweight.userdev")

    val serverVer = project.name.substring(1).replace('_', '.')

    dependencies {
        paperweight.paperDevBundle("$serverVer-R0.1-SNAPSHOT")
        compileOnly(project(":bukkit"))
    }
}