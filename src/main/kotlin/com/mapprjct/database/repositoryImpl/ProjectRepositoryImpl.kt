package com.mapprjct.database.repositoryImpl

import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.database.tables.ProjectTable
import com.mapprjct.database.tables.ProjectUsersTable
import com.mapprjct.model.datatype.Role
import com.mapprjct.model.datatype.RussiaPhoneNumber
import com.mapprjct.model.dto.ProjectDTO
import com.mapprjct.model.dto.ProjectMembershipDTO
import com.mapprjct.model.dto.ProjectRegistrationResultDTO
import com.mapprjct.model.dto.UnregisteredProjectDTO
import com.mapprjct.utils.asRole
import com.mapprjct.utils.toStringUUID
import com.mapprjct.utils.toUUID
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import java.util.*

class ProjectRepositoryImpl() : ProjectRepository {

    override suspend fun findUserMembershipInProject(
        userPhone: RussiaPhoneNumber,
        projectID: UUID
    ): ProjectMembershipDTO? {
        return (ProjectUsersTable innerJoin ProjectTable)
            .select(
            ProjectTable.id,
            ProjectTable.name,
            ProjectTable.membersCount,
            ProjectUsersTable.role
        ).where{
            (ProjectUsersTable.userPhone eq userPhone.value) and
                    (ProjectUsersTable.projectId eq projectID)
        }.singleOrNull()?.let { resultRow ->
            return ProjectMembershipDTO(
                projectDTO = ProjectDTO(
                    projectID = resultRow[ProjectTable.id].toStringUUID(),
                    name = resultRow[ProjectTable.name],
                    membersCount = resultRow[ProjectTable.membersCount].toInt(),
                ),
                role = resultRow[ProjectUsersTable.role].asRole()
            )
        }
    }

    override suspend fun findAllUserProjects(userPhone: RussiaPhoneNumber): List<ProjectMembershipDTO> {
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
                ProjectMembershipDTO(
                    projectDTO = result.toProjectDTO(),
                    role = result[ProjectUsersTable.role].asRole()
                )
            }
    }

    override suspend fun insert(
        creatorPhone: RussiaPhoneNumber,
        project: UnregisteredProjectDTO,
    ): ProjectRegistrationResultDTO {
        val newProjectUUID = UUID.randomUUID()
        val insertedProject = ProjectTable.insertReturning(
            returning = ProjectTable.columns
        ) {
            it[ProjectTable.name] = project.name
            it[ProjectTable.id] = newProjectUUID
            it[ProjectTable.membersCount] = 1
        }.single().toProjectDTO()
        ProjectUsersTable.insert {
            it[ProjectUsersTable.projectId] = newProjectUUID
            it[ProjectUsersTable.userPhone] = creatorPhone.normalizeAsRussiaPhone()
            it[ProjectUsersTable.role] = 1
        }
        return ProjectRegistrationResultDTO(
            projectDTO = insertedProject,
            oldID = project.oldID
        )
    }

    override suspend fun insertAll(userPhone : RussiaPhoneNumber, projects: List<UnregisteredProjectDTO>) : List<ProjectRegistrationResultDTO>{
        val insertedList = ProjectTable.batchInsert(projects) {
            this[ProjectTable.id] = UUID.randomUUID()
            this[ProjectTable.name] = it.name
            this[ProjectTable.membersCount] = 1
        }.mapIndexed { index,row->
            ProjectRegistrationResultDTO(
                projectDTO = row.toProjectDTO(),
                oldID = projects[index].oldID
            )
        }
        ProjectUsersTable.batchInsert(insertedList) {
            this[ProjectUsersTable.userPhone] = userPhone.normalizeAsRussiaPhone()
            this[ProjectUsersTable.role] = 1
            this[ProjectUsersTable.projectId] = it.projectDTO.projectID.toUUID()
        }
        return insertedList
    }

    override suspend fun getProjectById(projectId: UUID): ProjectDTO? {
        return ProjectTable
            .selectAll()
            .where{ ProjectTable.id eq projectId }
            .singleOrNull()?.let {
                ProjectDTO(
                    projectID = it[ProjectTable.id].toStringUUID(),
                    name = it[ProjectTable.name],
                    membersCount = it[ProjectTable.membersCount].toInt()
                )
            }
    }

    override suspend fun addMemberToProject(
        userPhone : RussiaPhoneNumber,
        projectDTO : ProjectDTO,
        role : Role
    ) : ProjectMembershipDTO {
        val projectUUID = projectDTO.projectID.toUUID()
        ProjectUsersTable.insert {
            it[ProjectUsersTable.userPhone] = userPhone.normalizeAsRussiaPhone()
            it[ProjectUsersTable.projectId] = projectUUID
            it[ProjectUsersTable.role] = role.toShort()
        }
        ProjectTable.update(
            where = { ProjectTable.id eq projectUUID },
            body = {
                it[ProjectTable.membersCount] = (projectDTO.membersCount + 1).toShort()
            }
        )
        return ProjectMembershipDTO(
            projectDTO = projectDTO,
            role = role
        )
    }

    private fun ResultRow.toProjectDTO(): ProjectDTO {
        return ProjectDTO(
            projectID = this[ProjectTable.id].toStringUUID(),
            name = this[ProjectTable.name],
            membersCount = this[ProjectTable.membersCount].toInt(),
        )
    }
}