import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    java
    `maven-publish`
    id("com.gradleup.shadow")
}

interface EsuPublishingExtension {
    val nameOverride: Property<String>
    val useShadow: Property<Boolean>
}

val extension = project.extensions.create<EsuPublishingExtension>("esuPublishing")

fun Project.defaultArtifactId(): String = buildString {
    append(name)
    var proj = parent
    while (proj != null) {
        insert(0, '-')
        insert(0, proj.name)
        proj = proj.parent
    }
}.lowercase()

project.afterEvaluate {
    extensions.configure<PublishingExtension>("publishing") {
        repositories {
            mavenLocal()
        }
        publications {
            create<MavenPublication>("mavenJar") {
                if (extension.useShadow.getOrElse(false)) {
                    from(components["shadow"])
//                    from(components["java"])
//                    artifacts.removeIf { it.classifier == null && it.extension == "jar" }
//                    artifact(tasks.named("shadowJar")) {
//                        classifier = null
//                    }
//                    tasks.withType<GenerateModuleMetadata>().configureEach {
//                        enabled = false
//                    }
                } else {
                    from(components["java"])
                }

                artifacts.removeIf { it.classifier == "sources" }
                artifact(tasks.findByName("sourcesFatJar") ?: tasks.getByName("sourcesJar")) {
                    classifier = "sources"
                }

                artifactId = extension.nameOverride.orNull ?: project.defaultArtifactId()
                groupId = project.group as String?
                version = project.version as String?
            }
        }
    }
}
