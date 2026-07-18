/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    java
    signing
    `maven-publish`
    id("com.gradleup.shadow")
}

interface EsuPublishingExtension {
    val artifactIdOverride: Property<String>
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
                    from(components["kotlin"])
                }

                artifactId = extension.artifactIdOverride.orNull ?: project.defaultArtifactId()
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

    signing {
        val signingKey: String? by project
        val signingPassword: String? by project
        if (signingKey != null && signingPassword != null) {
            useInMemoryPgpKeys(signingKey, signingPassword)
        } else {
            useGpgCmd()
        }
        sign(publishing.publications["mavenJar"])
    }

}
