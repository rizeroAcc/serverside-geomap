package com.mapprjct.builders

import com.mapprjct.model.dto.Project
import java.util.UUID

fun createTestProject(block: ProjectBuilder.() -> Unit) : Project {
    return ProjectBuilder().apply(block).build()
}

class ProjectBuilder {
    var projectID: String = UUID.randomUUID().toString()
    var name: String = "testProject"
    var memberCount: Int = 1
    fun build() = Project(
        projectID = projectID,
        name = name,
        membersCount = memberCount
    )
}