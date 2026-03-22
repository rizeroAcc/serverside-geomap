package com.mapprjct.builders

import com.mapprjct.model.datatype.StringUUID
import com.mapprjct.model.dto.ProjectDTO
import java.util.UUID

fun createTestProject(block: ProjectBuilder.() -> Unit) : ProjectDTO {
    return ProjectBuilder().apply(block).build()
}

class ProjectBuilder {
    var projectID: String = UUID.randomUUID().toString()
    var name: String = "testProject"
    var memberCount: Int = 1
    fun build() = ProjectDTO(
        projectID = StringUUID(projectID),
        name = name,
        membersCount = memberCount
    )
}