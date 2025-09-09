//buildscript {
//    repositories {
//        mavenCentral()
//    }
//    dependencies {
//        classpath("com.guardsquare:proguard-gradle:7.7.0")
//    }
//}
//
//tasks.register<proguard.gradle.ProGuardTask>("proguard") {
//    verbose()
//    ignorewarnings()
//    injars("build/libs/v1_18.jar")
//    outjars("build/libs/v1_18-test.jar")
//
//
//    assumenosideeffects("""
//        class kotlin.jvm.internal.Intrinsics
//    """.trimIndent())
//
//    keep("class *")
////    keeppackagenames("io.github.rothes")
//    keep("class io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.v1_18.ChunkDataThrottleHandlerImpl")
//    keep("class io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.v1_18.PalettedContainerReaderImpl")
////    dontobfuscate()
//    dontshrink()
//}