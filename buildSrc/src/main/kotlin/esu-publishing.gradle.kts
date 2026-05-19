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
    val pomDescription: Property<String>
}

val extension = project.extensions.create<EsuPublishingExtension>("esuPublishing")

val projectUrl = "https://github.com/Rothes/ESU"

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

                    artifacts.removeIf { it.classifier == "sources" }
                    artifact(tasks.findByName("sourcesFatJar") ?: tasks.getByName("sourcesJar")) {
                        classifier = "sources"
                    }
                } else {
                    from(components["java"])
                }

                artifactId = extension.nameOverride.orNull ?: project.defaultArtifactId()
                groupId = project.group as String?
                version = project.version as String?

                pom {
                    name.set(artifactId)
                    description.set(
                        extension.pomDescription.orNull ?: "Utilities for Minecraft servers."
                    )
                    url.set(projectUrl)

                    licenses {
                        license {
                            name.set("GNU Lesser General Public License v3.0")
                            url.set("https://www.gnu.org/licenses/lgpl-3.0.html")
                            distribution.set("repo")
                        }
                    }

                    scm {
                        connection.set("scm:git:$projectUrl.git")
                        developerConnection.set("scm:git:$projectUrl.git")
                        url.set("$projectUrl/tree/master")
                    }

                    developers {
                        developer {
                            id.set("Rothes")
                            name.set("Rothes")
                            url.set("https://github.com/Rothes")
                        }
                    }
                }
            }
        }
    }

}
