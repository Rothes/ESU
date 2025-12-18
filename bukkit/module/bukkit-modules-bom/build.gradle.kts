dependencies {
    val thisProject = project
    parent!!.childProjects.forEach { (_, project) ->
        if (project !== thisProject) {
            api(project)
        }
    }
}