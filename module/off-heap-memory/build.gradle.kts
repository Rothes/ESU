plugins {
    `multi-release-jar`
}

multiReleaseJar {
    javaVersions.add(JavaVersion.VERSION_22)
}

publishing {
    repositories {
        mavenLocal()
    }
    publications {
        create<MavenPublication>("mavenJar") {
            from(components["java"])

            artifactId = project.name
            groupId = project.group as String?
            version = project.version as String?
        }
    }
}