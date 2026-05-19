plugins {
    `multi-release-jar`
    `esu-publishing`
}

multiReleaseJar {
    javaVersions.add(JavaVersion.VERSION_22)
}
