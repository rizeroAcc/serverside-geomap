package com.mapprjct.database.repositoryImpl

import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.database.tables.ProjectTable
import com.mapprjct.database.tables.ProjectUsersTable
import com.mapprjct.model.dto.Project
import com.mapprjct.model.dto.ProjectMembership
import com.mapprjct.model.datatype.Role
import com.mapprjct.model.datatype.RussiaPhoneNumber
import com.mapprjct.model.datatype.StringUUID
import com.mapprjct.utils.asRole
import com.mapprjct.utils.toStringUUID
import com.mapprjct.utils.toUUID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.select
import java.util.UUID

class ProjectRepositoryImpl(val database: Database) : ProjectRepository {

    override suspend fun getAllUserProjects(userPhone: RussiaPhoneNumber): List<ProjectMembership> {
        return (ProjectUsersTable innerJoin ProjectTable)
            .select(
                ProjectTable.id,
                ProjectTable.name,
                ProjectTable.membersCount,
                ProjectUsersTable.role
            )
            .where {
                ProjectUsersTable.userPhone eq userPhone.normalizeAsRussiaPhone()
            }
            .map { result->
                ProjectMembership(
                    project = Project(
                        projectID = StringUUID(result[ProjectTable.id].toString()) ,
                        name = result[ProjectTable.name] ,
                        membersCount = result[ProjectTable.membersCount].toInt()
                    ),
                    role = result[ProjectUsersTable.role].asRole()
                )
            }
    }
    override suspend fun insertProject(
        creatorPhone: RussiaPhoneNumber,
        projectName: String
    ): Project {
        val newProjectUUID = UUID.randomUUID()
        ProjectTable.insert {
            it[ProjectTable.name] = projectName
            it[ProjectTable.id] = newProjectUUID
            it[ProjectTable.membersCount] = 1
        }
        ProjectUsersTable.insert {
            it[ProjectUsersTable.projectId] = newProjectUUID
            it[ProjectUsersTable.userPhone] = creatorPhone.normalizeAsRussiaPhone()
            it[ProjectUsersTable.role] = 1
        }
        return Project(
            projectID = newProjectUUID.toStringUUID(),
            name = projectName,
            membersCount = 1,
        )
    }

    override suspend fun getProjectById(projectId: UUID): Project? {
        return ProjectTable
            .selectAll()
            .where{ ProjectTable.id eq projectId }
            .singleOrNull()?.let {
                Project(
                    projectID = it[ProjectTable.id].toStringUUID(),
                    name = it[ProjectTable.name],
                    membersCount = it[ProjectTable.membersCount].toInt()
                )
            }
    }

    override suspend fun addMemberToProject(
        userPhone : RussiaPhoneNumber,
        project : Project,
        role : Role
    ) {
        val projectUUID = project.projectID.toUUID()
        ProjectUsersTable.insert {
            it[ProjectUsersTable.userPhone] = userPhone.normalizeAsRussiaPhone()
            it[ProjectUsersTable.projectId] = projectUUID
            it[ProjectUsersTable.role] = role.toShort()
        }
        ProjectTable.update(
            where = { ProjectTable.id eq projectUUID },
            body = {
                it[ProjectTable.membersCount] = (project.membersCount + 1).toShort()
            }
        )
    }
}